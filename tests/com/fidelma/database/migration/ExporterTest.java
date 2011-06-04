package com.fidelma.database.migration;

import com.fidelma.salesforce.database.migration.MigrationCriteria;
import com.fidelma.salesforce.database.migration.Exporter;
import com.fidelma.salesforce.database.migration.Migrator;
import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.misc.TestHelper;
import org.h2.util.ScriptReader;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 */
public class ExporterTest {
    private static SfConnection sfconn = null;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        sfconn = TestHelper.getTestConnection();
    }


    @Test
    public void testMigration() throws Exception {
        // Get a connection to the local database
        Properties info = new Properties();
        Connection h2Conn = DriverManager.getConnection(
//                "jdbc:h2:mem:"
                "jdbc:h2:/tmp/sfdc-h2"
                , info);

        // create salesforce tables with a relationship
        Statement sfdc = sfconn.createStatement();

        /*
        one__c.tworef__c    -- Lookup --> two__c
        three__c.one__c  -- Master/Detail child --> one__c
         */
        sfdc.execute("drop table three__c if exists");
        sfdc.execute("drop table one__c if exists");
        sfdc.execute("drop table two__c if exists");


        sfdc.execute("create table two__c(Name__c Text(20))"); // Just get the default columns, Id and Name.
        sfdc.execute("create table one__c(tworef__c Lookup references(two__c) with (relationshipName 'RefLookup'))");
        sfdc.execute("create table three__c(oneref__c MasterDetail references(one__c) with (relationshipName 'oneChild'))");

        sfdc.addBatch("insert into two__c(Name__c) values ('Norman')");
        sfdc.addBatch("insert into two__c(Name__c) values ('Wibbles')");
        sfdc.executeBatch();

        ResultSet keyRs = sfdc.getGeneratedKeys();
        keyRs.next();
        String twoNormanId = keyRs.getString(1);
        keyRs.next();
        String twoWibblesId = keyRs.getString(1);

        sfdc.addBatch("insert into one__c(tworef__c) values ('" + twoNormanId + "')");
        sfdc.addBatch("insert into one__c(tworef__c) values ('" + twoWibblesId + "')");
        sfdc.executeBatch();
        keyRs = sfdc.getGeneratedKeys();
        keyRs.next();
        String oneNormanId = keyRs.getString(1);
        keyRs.next();
        String oneWibblesId = keyRs.getString(1);

        sfdc.execute("insert into three__c(Name, oneref__c) values ('Kerry', '" + oneNormanId + "')");

        // Pull data down into local database
        List<MigrationCriteria> criteriaList = new ArrayList<MigrationCriteria>();
        MigrationCriteria criteria = new MigrationCriteria();
        criteria.tableName = "one__c";
        criteriaList.add(criteria);
        criteria = new MigrationCriteria();
        criteria.tableName = "two__c";
        criteriaList.add(criteria);
        criteria = new MigrationCriteria();
        criteria.tableName = "three__c";
        criteriaList.add(criteria);


        Exporter exporter = new Exporter(null);
        exporter.createLocalSchema(sfconn, h2Conn);
        exporter.downloadData(sfconn, h2Conn, criteriaList);

        // Delete the data from the destination salesforce
        sfdc.execute("delete from one__c");
        sfdc.execute("delete from two__c");
        sfdc.execute("delete from three__c");

        Migrator migrator = new Migrator();
        criteriaList.clear();
        criteria = new MigrationCriteria();
        criteria.tableName = "one__c";
        criteriaList.add(criteria);
        criteria = new MigrationCriteria();
        criteria.tableName = "two__c";
        criteriaList.add(criteria);
        criteria = new MigrationCriteria();
        criteria.tableName = "three__c";
        criteriaList.add(criteria);


        // Push rows back to Salesforce
        migrator.restoreRows(sfconn, h2Conn, criteriaList);

        // Check salesforce has the same data, but with different ids
        ResultSet rs = sfdc.executeQuery("select * from two__c order by Name__c");
        rs.next();
        String newTwoNormanId = rs.getString("Id");
        Assert.assertNotSame(newTwoNormanId, twoNormanId);
        Assert.assertEquals("Norman", rs.getString("Name__c"));
        rs.next();
        String newTwoWibbleId = rs.getString("Id");
        Assert.assertNotSame(newTwoWibbleId, twoWibblesId);
        Assert.assertEquals("Wibbles", rs.getString("Name__c"));
        Assert.assertFalse(rs.next());


        rs = sfdc.executeQuery("select Id, tworef__c from one__c order by twoRef__r.Name");
        rs.next();
        String newOneNormanId = rs.getString("Id");
        Assert.assertNotSame(newOneNormanId, oneNormanId);
        Assert.assertEquals(newTwoNormanId, rs.getString("tworef__c"));  // Do we refer to the new "two__c.Id"?
        rs.next();
        String newOneWibblesId = rs.getString("Id");
        Assert.assertNotSame(newOneWibblesId, oneWibblesId);
        Assert.assertEquals(newTwoWibbleId, rs.getString("tworef__c"));  // Do we refer to the new "two__c.Id"?

        Assert.assertFalse(rs.next());

        rs = sfdc.executeQuery("select Name, OneRef__r.TwoRef__r.Name__c from three__c");
        rs.next();
        Assert.assertEquals("Kerry", rs.getString(1));
        Assert.assertEquals("Norman", rs.getString(2));


    }

    private void populateData(Connection conn) throws Exception {
        /*
       Mary and Bob are users of the system
       Bob has created all records in the database

       There are 4 accounts in the system
       - Pizza Co and Burger Co, both owned by Mary
       - Vege Co, owned by Bob
       - Pizza Head Office, owned by Mary, and Parent Account of Pizza Co.

       Each Account has one contact.
       The contact of Pizza Co reports to the contact of Pizza Head Office

       Pizza Co has one Opportunity, with two Line items.

       The user table is READ ONLY

        */
        PreparedStatement stmt = conn.prepareStatement("insert into \"User\"(Id, Name) values (?,?)");
        stmt.setString(1, nextId());
        stmt.setString(2, "User Bob");
        stmt.execute();

        stmt.setString(1, nextId());
        stmt.setString(2, "User Mary");
        stmt.execute();


    }

    int idGenerator = 0;

    private String nextId() {
        return "ID" + idGenerator++;
    }


    private Connection setupData() throws Exception {
        Class.forName("org.h2.Driver");

        Properties info = new Properties();
//        info.put("user", "salesforce@fidelma.com");
//        info.put("password", "u9SABqa2dQxG0Y3kqWiJQVEwnYtryr1Ja1");
//        info.put("standard", "true");
//        info.put("includes", "Lead,Account");
//        info.put("useLabels", "true");

        // Get a connection to the database
        Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:"
                , info);

//        Statement stmt = conn.createStatement();
//        stmt.execute(
//                "create table customer " +
//                "(Id varchar primary key, extId varchar) ");

        String inFile = "testData/create-tables.txt";
//         InputStream is = getClass().getClassLoader().getResourceAsStream(inFile);
        InputStream is = new FileInputStream(inFile);
        LineNumberReader lineReader = new LineNumberReader(new InputStreamReader(is, "Cp1252"));
        ScriptReader reader = new ScriptReader(lineReader);
        while (true) {
            String sql = reader.readStatement();
            if (sql == null) {
                break;
            }
            sql = sql.trim();
            try {
                if ("@reconnect".equals(sql.toLowerCase())) {
                } else if (sql.length() == 0) {
                    // ignore
                } else if (sql.toLowerCase().startsWith("select")) {
                    ResultSet rs = conn.createStatement().executeQuery(sql);
                    while (rs.next()) {
                        String expected = reader.readStatement().trim();
                        String got = "> " + rs.getString(1);
                        assertEquals(expected, got);
                    }
                } else {
                    conn.createStatement().execute(sql);
//                    System.out.println("Executed " + sql);
                }
            } catch (SQLException e) {
                System.out.println(sql);
                throw e;
            }
        }
        is.close();
        //conn.close();

        return conn;

    }


}
