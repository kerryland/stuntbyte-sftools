package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.ddl.CreateTable;
import com.fidelma.salesforce.parse.SimpleParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 */

public class CreateTableTests {

    private static SfConnection conn = null;

    private static String surname;
    private static List<String> deleteMe = new ArrayList<String>();

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        /*
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
                */
    }


    @Test
    public void testCreateStatement() throws Exception {
//        SfConnection sfConnection = conn;
//        PartnerConnection pc = sfConnection.getHelper().getPartnerConnection();

        String sql = "create table wibble(ID integer, Name VARCHAR)";
        SimpleParser al = new SimpleParser(sql);
        al.read("create");
        al.read("table");

        CreateTable ct = new CreateTable(al, null);
        ct.execute();

    }
}
