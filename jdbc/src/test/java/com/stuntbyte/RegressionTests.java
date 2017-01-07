package com.stuntbyte;

import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.misc.TestHelper;
import org.junit.Test;

import java.sql.*;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * Tests that have failed from one version of Salesforce API to another.
 */
public class RegressionTests {
    private static SfConnection conn = null;
    private static TestHelper testHelper = new TestHelper();

    @Test
    public void testUnableToIdentifyTypeForAddress_API38() throws Exception {

        conn = testHelper.getTestConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "select * from SF.User limit 1");
        ResultSetMetaData rsmd = rs.getMetaData();

        assertEquals(DatabaseMetaData.columnNullable, rsmd.isNullable(1));
        assertEquals(DatabaseMetaData.columnNoNulls, rsmd.isNullable(2));

        assertEquals(-1, stmt.getUpdateCount());

    }
}
