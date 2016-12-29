/**
 * The MIT License
 * Copyright © 2011-2017 Kerry Sainsbury
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.stuntbyte.database.migration;

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
//    public static String licence = "ntpOo9JjXHTmYiYIyaG0XQ"; // Kerry uber-licence

    public static String plebUsername = "pleb@fidelma.com";
    public static String plebPassword = "utC1bQWQOa6JsT8mp3dTD4O6vpwpOdQIv";
//    public static String plebLicence= "x-N-WQkDO8ANK2YNjcXkoA";


    public static String hackUsername = "kerry@fidelma.com";
    public static String hackPassword = "g2Py8oPzUAsZJ1VuDEw71tfV2pwqMJN5O";
//    public static String hackLicence= "3qj6e8qaLfxP72hE5M2noA";


    public static SfConnection getTestConnection() throws SQLException {
        return connect(loginUrl, username, password, new Properties());
    }

    public static SfConnection getTestConnection(Properties info) throws SQLException {
        return connect(loginUrl, username, password, info);
    }


    public static SfConnection getPlebTestConnection() throws SQLException {
        return connect(loginUrl, plebUsername, plebPassword, new Properties());
    }


    public static SfConnection getHackConnection() throws SQLException {
        return connect(loginUrl, hackUsername, hackPassword, new Properties());
    }

    public static SfConnection connect(String lloginurl, String lusername, String lpassword, Properties info) throws SQLException {
        try {
            Class.forName("com.stuntbyte.salesforce.jdbc.SfDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(e); // Impossible!
        }


        info.put("user", lusername);
        info.put("password", lpassword);
//        info.put("standard", "true");
//        info.put("includes", "Lead,Account");

        // Get a connection to the database
        SfConnection sourceSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:" + lloginurl
                , info);

        return sourceSalesforce;
    }


}
