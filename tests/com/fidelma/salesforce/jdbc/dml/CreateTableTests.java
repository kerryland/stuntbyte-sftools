package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.ddl.CreateTable;
import com.fidelma.salesforce.parse.SimpleParser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 */

public class CreateTableTests {

    private static SfConnection conn = null;

    private static String surname;
    private static List<String> deleteMe = new ArrayList<String>();

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {

        Class.forName("com.fidelma.salesforce.jdbc.SfDriver");

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
        String sql = "create table wibble__c(Spang__c integer, Namero__c VARCHAR, price__c decimal(16,2))";
//        conn.createStatement().execute(sql);
//
//        PreparedStatement pstmt = conn.prepareStatement("insert into wibble__c(Spang__c, Namero__c, price__c) values (?,?,?)");
//        pstmt.setInt(1, 70);
//        pstmt.setString(2, "Seventy");
//        pstmt.setBigDecimal(3, new BigDecimal("70.77"));
//        pstmt.execute();

        // TODO: Improve this test
        // TODO: Refresh caches after create and drop table
        // TODO: Implement alter table add
        // TODO: Implement alter table drop column
        // TODO: Decide if we should throw away the __c nonsense....

        sql = "create table wibble__c(Hoover__c integer)";
        conn.createStatement().execute(sql);


//        conn.createStatement().execute("drop table wibble__c");
    }
}
