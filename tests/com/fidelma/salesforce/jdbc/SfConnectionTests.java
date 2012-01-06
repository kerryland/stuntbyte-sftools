package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.misc.TestHelper;
import junit.framework.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

/**
 */
public class SfConnectionTests {

    @Test
    public void testConnectionWithLicenceInProperties() throws Exception {
        Properties info = new Properties();
        info.setProperty("licence", TestHelper.licence);
        SfConnection conn = new SfConnection(
                TestHelper.loginUrl,
                TestHelper.username,
                TestHelper.password,
                info);

        Assert.assertFalse(conn.isClosed());
    }


    @Test
    public void testConnectionWithLicenceInPassword() throws Exception {
        Properties info = new Properties();

        String password = "licence(" + TestHelper.licence + ")sfdc(" + TestHelper.password + ")";
        SfConnection conn = new SfConnection(
                TestHelper.loginUrl,
                TestHelper.username,
                password,
                info);

        Assert.assertFalse(conn.isClosed());
    }


    @Test
    public void testConnectionWithExpiredLicence() throws Exception {
        Properties info = new Properties();
        info.setProperty("licence", "8Y5Ez6AiYm1cYvbB4MMqog");

        try {
            SfConnection conn = new SfConnection(
                    TestHelper.loginUrl,
                    TestHelper.username,
                    TestHelper.password,
                    info);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Assert.assertTrue(sw.toString().contains("JDBC Licence has expired"));
        }

    }

    @Test
    public void testConnectionWithNoLicence() throws Exception {
        Properties info = new Properties();
//        info.setProperty("licence", "8Y5Ez6AiYm1cYvbB4MMqog");

        try {
            SfConnection conn = new SfConnection(
                    TestHelper.loginUrl,
                    TestHelper.username,
                    TestHelper.password,
                    info);
            Assert.fail("Should not have connected!");
        } catch (Exception e) {

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Assert.assertTrue(sw.toString().contains("No licence information found"));
        }

    }


}
