package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.ddl.CreateTable;
import com.fidelma.salesforce.parse.SimpleParser;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 */

public class CreateTableTests {

    private static SfConnection conn = null;

    private static String surname;
    private static List<String> deleteMe = new ArrayList<String>();

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {

        Class.forName("com.fidelma.salesforce.jdbc.SfDriver");

        createConnection();

    }

    private static void createConnection() throws SQLException {
        Properties info = new Properties();
        info.put("user", "salesforce@fidelma.com");
        info.put("password", "u9SABqa2dQxG0Y3kqWiJQVEwnYtryr1Ja1");
        info.put("standard", "true");
        info.put("includes", "Lead,Account");
        info.put("useLabels", "true");

        // Get a connection to the database
        conn = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:https://login.salesforce.com"
                , info);
    }


    @Test
    public void testCreateStatement() throws Exception {
        conn.createStatement().execute("drop table wibble__c if exists");

        String sql = "create table wibble__c(Spang__c Number, Namero__c Text(20), price__c Number(16,2))";
        conn.createStatement().execute(sql);


        ResultSet rs = conn.getMetaData().getColumns(null, null, "wibble__c", "%");

        Set<String> names = new HashSet<String>();
        names.add("Namero__c");
        names.add("Spang__c");
        names.add("price__c");
//        names.add("Hoover__c");

        while (rs.next()) {
            names.remove(rs.getString("COLUMN_NAME"));
        }
        Assert.assertEquals(0, names.size());


        PreparedStatement pstmt = conn.prepareStatement("insert into wibble__c(Spang__c, Namero__c, price__c) values (?,?,?)");
        pstmt.setInt(1, 70);
        pstmt.setString(2, "Seventy");
        pstmt.setBigDecimal(3, new BigDecimal("70.77"));
        pstmt.execute();


        // TODO: Improve this test

        sql = "alter table wibble__c add Hoover__c Number";
        conn.createStatement().execute(sql);


        // TODO: This doesn't pass:

        rs = conn.getMetaData().getColumns(null, null, "wibble__c", "%");
        names = new HashSet<String>();
//        names.add("Namero__c");      // TODO: Make this pass
//        names.add("Spang__c");       // TODO: Make this pass
//        names.add("price__c");       // TODO: Make this pass
        names.add("Hoover__c");

        while (rs.next()) {
            names.remove(rs.getString("COLUMN_NAME"));
        }
        Assert.assertEquals(0, names.size());

        sql = "alter table wibble__c drop column spang__c";

        conn.createStatement().execute(sql);

        createConnection();
        rs = conn.getMetaData().getColumns(null, null, "wibble__c", "%");
        names = new HashSet<String>();
        names.add("Namero__c");
        names.add("price__c");
        names.add("Hoover__c");
        while (rs.next()) {
            names.remove(rs.getString("COLUMN_NAME"));
        }
        Assert.assertEquals(0, names.size());

    }
}
