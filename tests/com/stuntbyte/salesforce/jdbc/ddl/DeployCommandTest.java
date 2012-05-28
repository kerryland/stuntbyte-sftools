package com.stuntbyte.salesforce.jdbc.ddl;

import com.stuntbyte.salesforce.deployment.Deployer;
import com.stuntbyte.salesforce.deployment.DeploymentEventListener;
import com.stuntbyte.salesforce.deployment.DeploymentTestHelper;
import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.misc.BaseDeploymentEventListener;
import com.stuntbyte.salesforce.misc.Downloader;
import com.stuntbyte.salesforce.misc.FileUtil;
import com.stuntbyte.salesforce.misc.LoginHelper;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.stuntbyte.salesforce.misc.TestHelper;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Test the deploy tool
 */
public class DeployCommandTest {

    private static SfConnection conn = null;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        conn = TestHelper.getTestConnection();
    }


    @Test
    public void testCreateStatement() throws Exception {

        Statement stmt = conn.createStatement();
        stmt.execute("dep start"); // TODO: Add version number for package.xml
        stmt.execute("dep add ApexClass KerryTest");
        stmt.execute("dep add ApexClass StringUtils");

        stmt.execute("dep add ApexPage Exception");
        stmt.execute("dep add CustomObject abc__c with ListViews");

        stmt.execute("dep add CustomField  aaa__c.auto_number__c");

        stmt.execute("dep add CustomObject aaa__c");
//        stmt.execute("dep add ActionOverride aaa__c.Accept");
        stmt.execute("dep add WorkflowRule 'Lead.Web Directory Opt In Emails - Welcome'");
        stmt.execute("dep add WorkflowFieldUpdate Lead.Copy_physical_address_1_to_postal");

//        stmt.execute("dep drop CustomObject ddd__c");


//        stmt.execute("dep strip package FinancialForce");
//        stmt.execute("dep force version 19.0");
        stmt.execute("dep commit"); // TODO: Test dep rollback

        File packageFile = File.createTempFile("SFDC", "PKG");
        Assert.assertTrue(packageFile.delete());

        File outFile = File.createTempFile("SFDCOUT", "txt");
        Assert.assertTrue(outFile.delete());

        stmt.execute("dep PACKAGE TO '" + packageFile.getAbsolutePath() + "'");
        stmt.execute("dep UPLOAD PACKAGE FROM '" + packageFile.getAbsolutePath() + "' TO '" + outFile.getAbsolutePath() + "'");

        Assert.assertTrue(packageFile.exists());
        Assert.assertTrue(outFile.exists());

        FileUtil.delete(packageFile);
        FileUtil.delete(outFile);
    }


    @Test
    public void testReplacements() throws Exception {

        LoginHelper lh = new LoginHelper(TestHelper.loginUrl,
                TestHelper.username, TestHelper.password, 22d);

        DeploymentTestHelper dth = new DeploymentTestHelper(lh);

        String className = "Wibble" + System.currentTimeMillis();
        String source = "public class " + className + " {\n" +
                "// I like green\n" +
                "   public String getSpecialUserId() {\n" +
                "      return '00590000000dQVo';\n" +  // Dev
                "   }\n" +
                "}";

        Set<Deployer.DeploymentOptions> deploymentOptions = new HashSet<Deployer.DeploymentOptions>();
        dth.deployCode(className, source, deploymentOptions);

        Statement stmt = conn.createStatement();

        File packageFile = File.createTempFile("SFDC", "PKG");
        Assert.assertTrue(packageFile.delete());

        File outFile = File.createTempFile("SFDCOUT", "log");
        Assert.assertTrue(outFile.delete());

        try {
            stmt.execute("create table deploytest__c (\n" +
                    "     name           string(80),\n" +
                    "     cape_colour__c picklist('red', 'reddy', 'blue' default, 'green') sorted,\n" +
                    "     hat_colour__c  picklist('red', 'reddy', 'blue' default, 'green') sorted,\n" +
                    "     shoe_colour__c picklist('red', 'reddy', 'blue' default, 'green') sorted)");

            stmt.execute("dep start");
            stmt.execute("dep patch var dev.special_user=00590000000dQVo");
            stmt.execute("dep patch var uat.special_user=00590000000bWXy");
            stmt.execute("dep patch var prod.special_user=00590000000kPBa");

            stmt.execute("dep patch var dev.blue=blue");
            stmt.execute("dep patch var uat.blue=Test bloo");
            stmt.execute("dep patch var prod.blue=bloo bloo");

            stmt.execute("dep patch var dev.red=red");
            stmt.execute("dep patch var uat.red=Test red");
            stmt.execute("dep patch var prod.red=red red");

            stmt.execute("dep patch var dev.green=green");
            stmt.execute("dep patch var uat.green=Test green");
            stmt.execute("dep patch var prod.green=green green");

            stmt.execute("dep patch rule replace special_user in ApexClass " + className);
            stmt.execute("dep patch rule replace blue in CustomField deploytest__c.cape_colour__c");
            stmt.execute("dep patch rule replace red in CustomObject deploytest__c");
            stmt.execute("dep patch rule replace green in *"); // This is a bit silly. It's really dangerous with code and triggers
            stmt.execute("dep patch apply from dev to uat");

            stmt.execute("dep add ApexClass " + className);
            stmt.execute("dep add CustomField deploytest__c.cape_colour__c");
            stmt.execute("dep add CustomField deploytest__c.hat_colour__c");
            stmt.execute("dep commit");

            stmt.execute("dep PACKAGE TO '" + packageFile.getAbsolutePath() + "'");
            stmt.execute("dep UPLOAD PACKAGE FROM '" + packageFile.getAbsolutePath() + "' TO '" + outFile.getAbsolutePath() + "'");

            Assert.assertTrue(packageFile.exists());
            Assert.assertTrue(outFile.exists());


            // Download from SF
            Reconnector reconnector = new Reconnector(lh);

            File sourceSchemaDir = FileUtil.createTempDirectory("TEST");

            DeploymentEventListener listener = new BaseDeploymentEventListener();
            Downloader dl = new Downloader(reconnector, sourceSchemaDir, listener, null);
            dl.addPackage("ApexClass", className);
            dl.addPackage("CustomObject", "deploytest__c");
            File dir = dl.download();
            FileUtil.delete(dir);

            String content = FileUtil.loadTextFile(new File(sourceSchemaDir, "classes/" + className + ".cls"));

            Assert.assertTrue(content.endsWith("// I like Test green\n" +
                    "   public String getSpecialUserId() {\n" +
                    "      return '00590000000bWXy';\n" +
                    "   }\n" +
                    "}"));

            content = FileUtil.loadTextFile(new File(sourceSchemaDir, "objects/deploytest__c.object"));

            Assert.assertTrue(content.contains("<fullName>Test bloo</fullName>"));
            Assert.assertTrue(content.contains("<fullName>Test green</fullName>"));
            Assert.assertTrue(content.contains("<fullName>Test red</fullName>"));
            Assert.assertTrue(content.contains("<fullName>reddy</fullName>"));
            Assert.assertFalse(content.contains("<fullName>Test reddy</fullName>"));

        } finally {
            // Cleanup
            stmt.execute("dep start");
            stmt.execute("dep drop ApexClass " + className);
            stmt.execute("dep drop CustomObject deploytest__c");
            stmt.execute("dep commit");

            stmt.execute("dep PACKAGE TO '" + packageFile.getAbsolutePath() + "'");
            stmt.execute("dep UPLOAD PACKAGE FROM '" + packageFile.getAbsolutePath() + "' TO '" + outFile.getAbsolutePath() + "'");

            FileUtil.delete(packageFile);
            FileUtil.delete(outFile);

        }
    }


}
