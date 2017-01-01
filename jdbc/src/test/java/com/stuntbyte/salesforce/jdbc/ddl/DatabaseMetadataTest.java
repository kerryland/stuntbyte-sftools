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
package com.stuntbyte.salesforce.jdbc.ddl;

import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.misc.TestHelper;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.*;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class DatabaseMetadataTest {

    private static SfConnection conn = null;
    private static TestHelper testHelper = new TestHelper();

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        conn = testHelper.getTestConnection();
    }


    @Test
    public void testDatabaseMetaDataRecordTypeRemarks() throws Exception {
        DatabaseMetaData metadata = conn.getMetaData();

        ResultSet resultSet = metadata.getColumns(null, null, "Campaign", "RecordTypeId");
        Assert.assertTrue(resultSet.next());

        Assert.assertEquals("RecordTypeId", resultSet.getString("COLUMN_NAME"));
        Assert.assertTrue(resultSet.getString("REMARKS").startsWith("RecordTypes"));
    }


    @Test
    public void testDatabaseMetaDataRecordTypeNames() throws Exception {
        checkDataTypeName("api", "string");
        checkDataTypeName("ui", "Text");
        checkDataTypeName("sql92", "varchar");
        
        // And again, using a URL parameter
        
        String url = testHelper.getLoginUrl() + "?datatypes=ui";

        SfConnection conn = testHelper.connect(
                url,
                testHelper.getUsername(),
                testHelper.getPassword(),
                new Properties()
        );
        checkDataTypeName("Text", conn);
        
    }


    private void checkDataTypeName(String datatypesFormat, String expectedStringType) throws SQLException {

        Properties info = new Properties();
        info.put("datatypes", datatypesFormat);
        SfConnection conn = testHelper.getTestConnection(info);

        checkDataTypeName(expectedStringType, conn);
    }

    private void checkDataTypeName(String expectedStringType, SfConnection conn) throws SQLException {
        DatabaseMetaData metadata = conn.getMetaData();

        ResultSet resultSet = metadata.getColumns(null, null, "Campaign", "Name");

        Assert.assertTrue(resultSet.next());
        Assert.assertEquals(expectedStringType, resultSet.getString("TYPE_NAME"));
    }


    @Test
    public void testGetImportedKeys() throws Exception {

        DatabaseMetaData metadata = conn.getMetaData();

        ResultSet resultSet = metadata.getImportedKeys(null, null, "Account");
        ResultSetMetaData rsmd = resultSet.getMetaData();

        Assert.assertEquals("string", rsmd.getColumnTypeName(3));
        Assert.assertEquals("int", rsmd.getColumnTypeName(9));

        Assert.assertEquals(Types.VARCHAR, rsmd.getColumnType(3));
        Assert.assertEquals(Types.INTEGER, rsmd.getColumnType(9));

        while (resultSet.next()) {
            // I'm happy enough if they don't throw an exception
            resultSet.getString("PKTABLE_NAME");
            resultSet.getInt("KEY_SEQ");
        }
    }


    @Test
    public void testGetExportedKeys() throws Exception {

        DatabaseMetaData metadata = conn.getMetaData();

        ResultSet resultSet = metadata.getExportedKeys(null, null, "Account");
        ResultSetMetaData rsmd = resultSet.getMetaData();

        Assert.assertEquals("string", rsmd.getColumnTypeName(3));
        Assert.assertEquals("int", rsmd.getColumnTypeName(9));

        Assert.assertEquals(Types.VARCHAR, rsmd.getColumnType(3));
        Assert.assertEquals(Types.INTEGER, rsmd.getColumnType(9));

        while (resultSet.next()) {
            // I'm happy enough if they don't throw an exception
            resultSet.getString("PKTABLE_NAME");
            resultSet.getInt("KEY_SEQ");
        }
    }


//    @Test
//    public void testCatalogsDatabaseMetaData() throws Exception {
//
//        DatabaseMetaData metadata = conn.getMetaData();
//
//        ResultSet resultSet = metadata.getCatalogs();
//        while (resultSet.next()) {
//            String catalog = resultSet.getString("TABLE_CAT");
//            System.out.println("KJS CT1=" + catalog);
//        }
//
//        resultSet = metadata.getSchemas();
//        while (resultSet.next()) {
//            String catalog = resultSet.getString("TABLE_CATALOG");
//            System.out.println("KJS CT2=" + catalog);
//
//        }
//
//    }
}
