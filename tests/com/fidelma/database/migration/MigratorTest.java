package com.fidelma.database.migration;

import com.fidelma.salesforce.database.migration.Exporter;
import com.fidelma.salesforce.database.migration.MigrationCriteria;
import com.fidelma.salesforce.database.migration.Migrator;
import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.misc.StdOutDeploymentEventListener;
import com.fidelma.salesforce.misc.TestHelper;
import org.h2.util.ScriptReader;
import org.junit.Assert;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 */
public class MigratorTest {

    @Test
    public void testDeleteEverything() throws Exception {

        SfConnection conn = TestHelper.getHackConnection();

        conn.createStatement().execute("drop table i_am_going__c if exists");

        // Create a known table in the hack salesforce instance
        PreparedStatement statement = conn.prepareStatement(
                "create table i_am_going__c(hello__c string(20))");

        statement.execute();
        // Prove the known table really exists
        statement.executeQuery("select count(*) from i_am_going__c");

        // Delete all tables from hack instance
        Migrator m = new Migrator();
        m.deleteAllTables(conn, new StdOutDeploymentEventListener());

        // This should now blow up
        try {
            statement.executeQuery("select count(*) from i_am_going__c");
            Assert.fail("Should have deleted the table!");
        } catch (SQLException e) {
            // Good!
        }
    }


    //    @Test
    public void testNotMuch() throws Exception {
        Class.forName("com.fidelma.salesforce.jdbc.SfDriver");

        Properties info = new Properties();
        info.put("user", TestHelper.username);
        info.put("password", TestHelper.password);
//        info.put("standard", "true");
//        info.put("includes", "Lead,Account");
//        info.put("useLabels", "true");

        // Get a connection to the database
        SfConnection sourceSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:" + TestHelper.loginUrl
                , info);

        Class.forName("org.h2.Driver");

        info = new Properties();
//        info.put("user", "salesforce@fidelma.com");
//        info.put("password", "u9SABqa2dQxG0Y3kqWiJQVEwnYtryr1Ja1");
//        info.put("standard", "true");
//        info.put("includes", "Lead,Account");
//        info.put("useLabels", "true");

        // Get a connection to the database
        Connection h2Conn = DriverManager.getConnection(
//                "jdbc:h2:mem:"
                "jdbc:h2:/tmp/sfdc-h2"
                , info);

        info = new Properties();
        info.put("user", "kerry@fidelma.com");
        info.put("password", "g2Py8oPzUAsZJ1VuDEw71tfV2pwqMJN5O");
//        info.put("standard", "true");
//        info.put("includes", "Lead,Account");
//        info.put("useLabels", "true");

        // Get a connection to the database
        SfConnection destSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:" + TestHelper.loginUrl
                , info);

        Migrator migrator = new Migrator();
        migrator.replicate(sourceSalesforce, destSalesforce, h2Conn);
    }

    public static void main(String[] args) throws Exception {

         Class.forName("com.fidelma.salesforce.jdbc.SfDriver");

        Properties info = new Properties();
        info.put("user", "fronde.admin@localist.co.nz");
        info.put("password", "jrP2U0TnW09DesQIaxOmAb3yWiN9lRLu");
        SfConnection sourceSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:https://login.salesforce.com"
                , info);

        Class.forName("org.h2.Driver");

        info = new Properties();
        // Get a connection to the database
        Connection h2Conn = DriverManager.getConnection(
                "jdbc:h2:/tmp/sfdc-prod"
                , info);

        info = new Properties();
        info.put("user", "fronde.admin@localist.co.nz.dev1");
        info.put("password", "jrP2U0TnW09DesQIaxOmAb3yWiN9lRLu");

        // Get a connection to the database
        SfConnection destSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:https://test.salesforce.com"
                , info);

        List<MigrationCriteria> criteriaList = new ArrayList<MigrationCriteria>();

        // TODO: Handle record type and ownerid as special cases

        String[] tables = new String[] {
//                "Reference_Item__c",
                "Location__c",
                "Location_Relationship__c",
                "Term_Or_Condition_Specification__c",
                "localist_product_specification__c",
                "Presence_Category_Group__c",
                "Presence_Category__c",
                "Presence_Category_Group_Member__c",
                "Print_Book_Service__c",
                "product2"
        };

        for (int i = 0; i < tables.length; i++) {
            String table = tables[i];
            destSalesforce.createStatement().execute("delete from " + table);
        }


        for (int i = 0; i < tables.length; i++) {
            String table = tables[i];
            MigrationCriteria criteria = new MigrationCriteria();
            criteria.tableName = table;
            criteria.sql = "";
            criteriaList.add(criteria);
        }
        Exporter exporter = new Exporter();
        exporter.createLocalSchema(sourceSalesforce, h2Conn);
        exporter.downloadData(sourceSalesforce, h2Conn, criteriaList);


        Migrator migrator = new Migrator();
        migrator.restoreRows(destSalesforce, h2Conn, criteriaList);
    }
}
