package com.stuntbyte.salesforce.misc;

import com.stuntbyte.salesforce.jdbc.SfConnection;

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
    public static String licence = "ntpOo9JjXHQ2uOYAlM724w";

    public static String plebUsername = "pleb@fidelma.com";
    public static String plebPassword = "utC1bQWQOa6JsT8mp3dTD4O6vpwpOdQIv";
    public static String plebLicence= "vPNE7-Lg9GuIJQXim9_dyA";


    public static String hackUsername = "kerry@fidelma.com";
    public static String hackPassword = "g2Py8oPzUAsZJ1VuDEw71tfV2pwqMJN5O";
    public static String hackLicence= "3qj6e8qaLfxP72hE5M2noA";



    public static SfConnection getTestConnection() throws SQLException {
        return connect(loginUrl, username, password, licence);
    }

    public static SfConnection getPlebTestConnection() throws SQLException {
        return connect(loginUrl, plebUsername, plebPassword, plebLicence);
    }


    public static SfConnection getHackConnection() throws SQLException {
        return connect(loginUrl, hackUsername, hackPassword, hackLicence);
    }

    public static SfConnection connect(String lloginurl, String lusername, String lpassword, String llicence) throws SQLException {
        try {
            Class.forName("com.stuntbyte.salesforce.jdbc.SfDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(e); // Impossible!
        }

        Properties info = new Properties();
        info.put("user", lusername);
        info.put("password", lpassword);
        info.put("licence", llicence);
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
