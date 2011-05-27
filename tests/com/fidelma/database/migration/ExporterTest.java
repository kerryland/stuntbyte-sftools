package com.fidelma.database.migration;

import com.fidelma.salesforce.database.migration.Exporter;
import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.misc.TestHelper;
import org.h2.util.ScriptReader;
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
import java.util.Properties;

import static org.junit.Assert.*;

/**
 */
public class ExporterTest {

    @Test
    public void testNotMuch() throws Exception {
        Class.forName("com.fidelma.salesforce.jdbc.SfDriver");

        Properties info = new Properties();
        info.put("user", TestHelper.username);
        info.put("password", TestHelper.password);
        info.put("standard", "true");
        info.put("includes", "Lead,Account");
        info.put("useLabels", "true");

        // Get a connection to the database
        SfConnection sfConn = (SfConnection) DriverManager.getConnection(
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


        Exporter exporter = new Exporter(null);

        exporter.createLocalSchema(sfConn, h2Conn);

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
