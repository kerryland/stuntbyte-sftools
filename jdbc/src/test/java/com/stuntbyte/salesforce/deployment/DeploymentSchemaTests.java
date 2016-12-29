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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class DeploymentSchemaTests {

    private static SfConnection conn = null;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        Class.forName("com.stuntbyte.salesforce.jdbc.SfDriver");

        Properties info = new Properties();
        info.put("user", TestHelper.username);
        info.put("password", TestHelper.password);
        info.put("standard", "true");
        info.put("useLabels", "true");

        // Get a connection to the database
        conn = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:" + TestHelper.loginUrl
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

        for (String metadataType : metadataService.getMetadataTypes()) {
            ResultSet rs;
            try {
                rs = stmt.executeQuery("select count(*) from " + ResultSetFactory.DEPLOYABLE + "." + metadataType);
            } catch (Exception e) {
                if (e.getMessage().matches(".*Cannot use.*in this version")) {
                    continue; // fine. likely will work in another version
                } else if (e.getMessage().matches(".*Cannot use.*in this organization")) {
                    continue; // fine. likely will work in another organisation
                } else {
                    Assert.fail("Unexpected error encountered: " + e.getMessage());
                    continue;
                }
            }
            assertEquals("Weird column count for " + metadataType, 1, rs.getMetaData().getColumnCount());
            assertTrue(rs.next());
            assertEquals("COUNT", rs.getMetaData().getColumnName(1));
            assertEquals("COUNT", rs.getMetaData().getColumnLabel(1));
        }

    }

}
