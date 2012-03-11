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

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        conn = TestHelper.getTestConnection();
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
        
        String url = TestHelper.loginUrl + "?datatypes=ui";

        SfConnection conn = TestHelper.connect(
                url,
                TestHelper.username,
                TestHelper.password,
                TestHelper.licence,
                new Properties()
        );
        checkDataTypeName("Text", conn);
        
    }


    private void checkDataTypeName(String datatypesFormat, String expectedStringType) throws SQLException {

        Properties info = new Properties();
        info.put("datatypes", datatypesFormat);
        SfConnection conn = TestHelper.getTestConnection(info);

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


    @Test
    public void testCatalogsDatabaseMetaData() throws Exception {

        DatabaseMetaData metadata = conn.getMetaData();

        ResultSet resultSet = metadata.getCatalogs();
        while (resultSet.next()) {
            String catalog = resultSet.getString("TABLE_CAT");
            System.out.println("KJS CT1=" + catalog);
        }

        resultSet = metadata.getSchemas();
        while (resultSet.next()) {
            String catalog = resultSet.getString("TABLE_CATALOG");
            System.out.println("KJS CT2=" + catalog);

        }

    }
}
