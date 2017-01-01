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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Simple helper class for unit tests
 */
public class MigrationTestHelper {
    private Properties testProperties = null;
    private Properties connectionProperties = new Properties();

    public MigrationTestHelper() {
        try {
            connectionProperties.setProperty("deployable", "false");

            testProperties = new Properties();
            InputStream stream;

            String propertyFile = System.getProperty("test.properties");
            if (propertyFile != null) {
                stream = new FileInputStream(new File(propertyFile));
            } else {
                stream = getClass().getResourceAsStream("/test.properties");
            }
            testProperties.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getSourceLoginUrl() {
        return getProperty("sourceLoginUrl");
    }

    private String getSourceUsername() {
        return getProperty("sourceUsername");
    }

    private String getSourcePassword() {
        return getProperty("sourcePassword");
    }

    private String getDestLoginUrl() {
        return getProperty("destLoginUrl");
    }

    private String getDestUsername() {
        return getProperty("destUsername");
    }

    private String getDestPassword() {
        return getProperty("destPassword");
    }


    private String getProperty(String propertyName) {
        String result = testProperties.getProperty(propertyName);
        if (result == null) {
            throw new IllegalArgumentException(propertyName + " is not defined in test.properties");
        }
        return result;
    }


    public SfConnection getDestConnection() throws SQLException {
        return connect(getDestLoginUrl(), getDestUsername(), getDestPassword(), connectionProperties);
    }

    public SfConnection getSourceConnection() throws SQLException {
        return connect(getSourceLoginUrl(), getSourceUsername(), getSourcePassword(), connectionProperties);
    }


    private SfConnection connect(String lloginurl, String lusername, String lpassword, Properties info) throws SQLException {
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
