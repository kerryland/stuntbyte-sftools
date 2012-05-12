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
        Calendar expires = Calendar.getInstance();
        expires.add(Calendar.YEAR, 1);
        Licence licence = new Licence(1000, "Kerry Sainsbury", expires);
        licence.setDeploymentFeature(true);
        licence.setPersonalLicence(true);

        Assert.assertEquals(1000, licence.getCustomerNumber());
        Assert.assertEquals("Kerry Sainsbury", licence.getName());
        Assert.assertFalse(licence.supportsJdbcFeature());
        Assert.assertTrue(licence.supportsPersonalLicence());
        licence.generateNameHash();

        String key = KeyGen.generateKey(licence);

        LicenceService ls = new LicenceService();

        LicenceResult licenceResult = ls.checkLicence("Kerry Sainsbury",
                "My Co", // Doesn't matter for personal licence
                key);

        Licence second = licenceResult.getLicence();

        // Now reconstitute the licence from bytes
        Assert.assertEquals(1000, second.getCustomerNumber());
        Assert.assertEquals("kerry sainsbury", second.getName());
        Assert.assertFalse(licence.supportsJdbcFeature());
        Assert.assertTrue(licence.supportsDeploymentFeature());
        Assert.assertTrue(licence.supportsPersonalLicence());

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
