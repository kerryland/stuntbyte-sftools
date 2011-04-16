package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.ddl.CreateTable;
import com.fidelma.salesforce.parse.SimpleParser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.DriverManager;
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
        SimpleParser al = new SimpleParser(sql);
        al.read("create");
        al.read("table");

        CreateTable ct = new CreateTable(al, null, conn.getHelper().getMetadataConnection());
        ct.execute();

    }
}
