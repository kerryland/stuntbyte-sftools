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
package com.stuntbyte.salesforce.misc;

import com.stuntbyte.salesforce.jdbc.SfConnection;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Simple helper class for unit tests
 */
public class TestHelper {

    private Properties prop = null;

    public TestHelper() {
        try {
            prop = new Properties();
            InputStream stream;

            String testpropertiesfile = System.getProperty("test.properties");
            if (testpropertiesfile == null)
            {
                stream = getClass().getResourceAsStream( "/test.properties" );
            }
            else
            {
                stream = new FileInputStream(testpropertiesfile);
            }
            if (stream == null)
            {
                throw new RuntimeException("Unable to find test.properties via -Dtest.properties or on classpath");
            }
            prop.load( stream );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLoginUrl() {
        return getProperty("loginUrl");
    }

    public String getUsername() {
        return getProperty("username");
    }

    public String getPassword() {
        return getProperty("password");
    }

    public String getPlebUsername() {
        return getProperty("plebUsername");
    }

    public String getPlebPassword() {
        return getProperty("plebPassword");
    }

    private String getProperty(String propertyName) {
        String result = prop.getProperty(propertyName);
        if (result == null) {
            throw new IllegalArgumentException(propertyName + " is not defined in test.properties");
        }
        return result;
    }
    
    public SfConnection getTestConnection() throws SQLException {
        return connect(getLoginUrl(), getUsername(), getPassword(), new Properties());
    }

    public SfConnection getPlebConnection() throws SQLException {
        return connect(getLoginUrl(), getPlebUsername(), getPlebPassword(), new Properties());
    }

    public SfConnection getTestConnection(Properties info) throws SQLException {
        return connect(getLoginUrl(), getUsername(), getPassword(), info);
    }


    public SfConnection connect(String lloginurl, String lusername, String lpassword, Properties info) throws SQLException {
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
