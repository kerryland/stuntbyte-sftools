package com.fidelma.salesforce.misc;

import com.fidelma.salesforce.jdbc.SfConnection;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 22/05/11
 * Time: 7:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestHelper {
    public static String loginUrl = "https://login.salesforce.com";
    public static String username = "salesforce@fidelma.com";
    public static String password = "u9SABqa2dQxG0Y3kqWiJQVEwnYtryr1Ja1";

    public static String hackUsername = "kerry@fidelma.com";
    public static String hackPassword = "g2Py8oPzUAsZJ1VuDEw71tfV2pwqMJN5O";


    public static SfConnection getTestConnection() throws ClassNotFoundException, SQLException {
        return connect(loginUrl, username, password);
    }

    public static SfConnection getHackConnection() throws ClassNotFoundException, SQLException {
        return connect(loginUrl, hackUsername, hackPassword);
    }

    private static SfConnection connect(String lloginurl, String lusername, String lpassword) throws ClassNotFoundException, SQLException {
        Class.forName("com.fidelma.salesforce.jdbc.SfDriver");

        Properties info = new Properties();
        info.put("user", lusername);
        info.put("password", lpassword);
//        info.put("standard", "true");
//        info.put("includes", "Lead,Account");
//        info.put("useLabels", "true");

        // Get a connection to the database
        SfConnection sourceSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:" + lloginurl
                , info);

        return sourceSalesforce;
    }


}
