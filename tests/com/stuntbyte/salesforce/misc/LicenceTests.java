package com.stuntbyte.salesforce.misc;

import com.sforce.soap.partner.sobject.SObject;
import com.stuntbyte.salesforce.jdbc.LicenceException;
import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 */
public class LicenceTests {


    @Test
    public void testFeaturesFromByte() throws Exception {
        Licence licence = new Licence(1000, "Kerry Sainsbury", Calendar.getInstance());
        licence.setJdbcFeature(true);

        Assert.assertEquals(1000, licence.getCustomerNumber());
        Assert.assertEquals("Kerry Sainsbury", licence.getName());
        Assert.assertTrue(licence.supportsJdbcFeature());
        Assert.assertFalse(licence.supportsPersonalLicence());
        licence.generateNameHash();

        byte[] bytes = licence.getBytes();

        // Now reconstitute the licence from bytes
        Licence second = new Licence(bytes, "Kerry Sainsbury");
        Assert.assertEquals(1000, second.getCustomerNumber());
        Assert.assertEquals("Kerry Sainsbury", second.getName());
        Assert.assertTrue(licence.supportsJdbcFeature());
        Assert.assertFalse(licence.supportsPersonalLicence());

        Assert.assertTrue(Arrays.equals(licence.getStoredNameHash(), second.getStoredNameHash()));
    }

    @Test
    public void testNoUsefulLicence() throws Exception {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.MONTH, 1);

        Licence licence = new Licence(0, "", tomorrow);

        SfConnection conn = connectWithLicence(licence);

        doNoSqlTests(conn);
    }

    @Test
    public void testJdbcLicence() throws Exception {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.MONTH, 1);

        Licence licence = new Licence(0, "", tomorrow);
        licence.setJdbcFeature(true);

        SfConnection conn = connectWithLicence(licence);

        ResultSet rs = conn.getMetaData().getSchemas();
        Assert.assertTrue(rs.next());
        Assert.assertFalse(rs.next());

        rs = conn.getMetaData().getTables(null, null, null, null);
        Assert.assertTrue(rs.next());

        PreparedStatement stmt = conn.prepareStatement("select * from user limit 2");
        rs = stmt.executeQuery();
        Assert.assertTrue(rs.next());
    }

    @Test
    public void testDeployLicence() throws Exception {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.MONTH, 1);

        Licence licence = new Licence(0, "", tomorrow);
        licence.setDeploymentFeature(true);

        SfConnection conn = connectWithLicence(licence);

        ResultSet rs = conn.getMetaData().getSchemas();
        Assert.assertTrue(rs.next());
        Assert.assertEquals(ResultSetFactory.DEPLOYABLE, rs.getString("TABLE_SCHEM"));
        Assert.assertFalse(rs.next());

        rs = conn.getMetaData().getTables(null, null, null, null);
        Assert.assertTrue(rs.next());
        do {
            Assert.assertEquals(ResultSetFactory.DEPLOYABLE, rs.getString("TABLE_SCHEM"));

        } while (rs.next());

        try {
            PreparedStatement stmt = conn.prepareStatement("select * from user limit 2");
            rs = stmt.executeQuery();
            Assert.fail("Exception should be thrown");
        } catch (SQLException e) {
            Assert.assertEquals("Unknown table user in schema null", e.getMessage());
        }

        PreparedStatement stmt = conn.prepareStatement("select * from deployable.Profile");
        rs = stmt.executeQuery();
        Assert.assertTrue(rs.next());
    }


    private void doNoSqlTests(SfConnection conn) throws SQLException {
        ResultSet rs = conn.getMetaData().getSchemas();
        Assert.assertFalse(rs.next());

        rs = conn.getMetaData().getTables(null, null, null, null);
        Assert.assertFalse(rs.next());

        PreparedStatement stmt = conn.prepareStatement("select * from user limit 1");
        try {
            stmt.execute();
            Assert.fail("Licence exception should be thrown");
        } catch (SQLException e) {
            Assert.assertEquals("Licence does not support JDBC", e.getMessage());// Good!
        }
    }


    public static SfConnection connectWithLicence(Licence licence) throws Exception {
        Class.forName("com.stuntbyte.salesforce.jdbc.SfDriver");

        licence.setName("Kerry Sainsbury");
        String licenceKey = KeyGen.generateKey(licence);

        Properties info = new Properties();
        info.put("user", TestHelper.username);
        info.put("password", TestHelper.password);
        info.put("licence", licenceKey);

        // Get a connection to the database
        SfConnection conn = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:" + TestHelper.loginUrl
                , info);
        return conn;
    }

}
