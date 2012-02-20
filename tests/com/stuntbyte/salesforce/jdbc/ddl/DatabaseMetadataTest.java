package com.stuntbyte.salesforce.jdbc.ddl;

import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.misc.TestHelper;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;

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
    public void testDatabaseMetaData() throws Exception {

        DatabaseMetaData metadata = conn.getMetaData();
        
        ResultSet resultSet = metadata.getTables(null, null, null, null);
        while (resultSet.next()) {
            String tableName = resultSet.getString("TABLE_NAME");
            ResultSet rs2 = metadata.getColumns(null, null, tableName, null);

            boolean recordTypeColumnFound = false;

            while (rs2.next()) {
                String name = rs2.getString("COLUMN_NAME");
//                System.out.println(name);
                if (name.equalsIgnoreCase("RecordTypeId")) {
                    System.out.println(tableName + " " + rs2.getString("REMARKS"));
                }
            }

            
        }

        
        
        resultSet = metadata.getColumns(null, null, "ContentVersion", null);
        
        boolean recordTypeColumnFound = false;
        
        while (resultSet.next()) {
            String name = resultSet.getString("COLUMN_NAME");
            System.out.println(name);
            if (name.equalsIgnoreCase("RecordTypeId")) {
                Assert.assertTrue(resultSet.getString("REMARKS").startsWith("RecordTypes"));
                recordTypeColumnFound = true;
            }
        }
        
        Assert.assertTrue(recordTypeColumnFound);

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
}
