package com.stuntbyte.salesforce.jdbc;

import com.stuntbyte.salesforce.misc.TestHelper;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class SfResultSetMetaDataTest {

    private static TestHelper testHelper = new TestHelper();

    @Test
    public void testUnableToIdentifyTypeForAddress_API38() throws Exception {
        SfConnection conn  = testHelper.getTestConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "select address, name, division from User limit 1");
        ResultSetMetaData rsmd = rs.getMetaData();

        assertFalse(rsmd.isWritable(1));
        assertFalse(rsmd.isWritable(2));
        assertTrue(rsmd.isWritable(3));
    }
}
