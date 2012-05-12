package com.stuntbyte.salesforce.misc;

import com.stuntbyte.salesforce.jdbc.SfConnection;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Simple helper class for unit tests
 */
public class TestHelper {
    public static String loginUrl = "https://login.salesforce.com";
    public static String username = "salesforce@fidelma.com";
    public static String password = "u9SABqa2dQxG0Y3kqWiJQVEwnYtryr1Ja1";
//    public static String licence = "MxjetovtygmPUHNqoPAXGQ"; // Demo licence
    public static String licence = "ntpOo9JjXHTmYiYIyaG0XQ"; // Kerry uber-licence

    public static String plebUsername = "pleb@fidelma.com";
    public static String plebPassword = "utC1bQWQOa6JsT8mp3dTD4O6vpwpOdQIv";
    public static String plebLicence= "x-N-WQkDO8ANK2YNjcXkoA";


    public static String hackUsername = "kerry@fidelma.com";
    public static String hackPassword = "g2Py8oPzUAsZJ1VuDEw71tfV2pwqMJN5O";
    public static String hackLicence= "3qj6e8qaLfxP72hE5M2noA";



    public static SfConnection getTestConnection() throws SQLException {
        return connect(loginUrl, username, password, licence, new Properties());
    }

    public static SfConnection getTestConnection(Properties info) throws SQLException {
        return connect(loginUrl, username, password, licence, info);
    }


    public static SfConnection getPlebTestConnection() throws SQLException {
        return connect(loginUrl, plebUsername, plebPassword, plebLicence, new Properties());
    }


    public static SfConnection getHackConnection() throws SQLException {
        return connect(loginUrl, hackUsername, hackPassword, hackLicence, new Properties());
    }

    public static SfConnection connect(String lloginurl, String lusername, String lpassword, String llicence, Properties info) throws SQLException {
        try {
            Class.forName("com.stuntbyte.salesforce.jdbc.SfDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(e); // Impossible!
        }


        info.put("user", lusername);
        info.put("password", lpassword);
        info.put("licence", llicence);
//        info.put("standard", "true");
//        info.put("includes", "Lead,Account");

        // Get a connection to the database
        SfConnection sourceSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:" + lloginurl
                , info);

        return sourceSalesforce;
    }


}
