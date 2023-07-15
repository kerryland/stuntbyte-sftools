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
package com.stuntbyte.salesforce.deployment;

import com.stuntbyte.salesforce.core.metadata.MetadataService;
import com.stuntbyte.salesforce.core.metadata.MetadataServiceImpl;
import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.misc.TestHelper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeploymentSchemaTests {

    private static SfConnection conn = null;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        Class.forName("com.stuntbyte.salesforce.jdbc.SfDriver");
        TestHelper testHelper = new TestHelper();

        Properties info = new Properties();
        info.put("user", testHelper.getUsername());
        info.put("password", testHelper.getPassword());
        info.put("standard", "true");
        info.put("useLabels", "true");
        info.put("deployable", "true");

        // Get a connection to the database
        conn = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:" + testHelper.getLoginUrl()
                , info);
    }


    @Test
    public void testCount() throws SQLException {

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select count(*) from " + ResultSetFactory.DEPLOYABLE + ".Letterhead");
        assertEquals(1, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals("COUNT", rs.getMetaData().getColumnName(1));
        assertEquals("COUNT", rs.getMetaData().getColumnLabel(1));

        assertEquals(0, rs.getInt(1));
        assertEquals(0, rs.getInt("COUNT"));
    }



    @Test
    public void testMetadataTypesCanBeQueried() throws SQLException {

        MetadataService metadataService = new MetadataServiceImpl(null);
        Statement stmt = conn.createStatement();

        int okCount = 0;

        for (String metadataType : metadataService.getMetadataTypes()) {
            ResultSet rs;
            try {
                rs = stmt.executeQuery("select count(*) from " + ResultSetFactory.DEPLOYABLE + "." + metadataType);
                assertEquals("Weird column count for " + metadataType, 1, rs.getMetaData().getColumnCount());
                assertTrue(rs.next());
                assertEquals("COUNT", rs.getMetaData().getColumnName(1));
                assertEquals("COUNT", rs.getMetaData().getColumnLabel(1));

                okCount++;
            } catch (Exception e) {
                if (e.getMessage().matches(".*Cannot use.*in this version")) {
                    continue; // fine. likely will work in another version
                } else if (e.getMessage().matches(".*Cannot use.*in this organization")) {
                    continue; // fine. likely will work in another organisation
                } else {
                    System.out.println("Unexpected error encountered when querying " + metadataType + ": " + e.getMessage());
                    continue;
                }
            }

            assertTrue(okCount > 0);
        }

    }

}
