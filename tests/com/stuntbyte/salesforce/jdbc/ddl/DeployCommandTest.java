package com.stuntbyte.salesforce.jdbc.ddl;

import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.misc.TestHelper;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.sql.Statement;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 23/06/11
 * Time: 7:34 PM
 * To change this template use File | Settings | File Templates.
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



    }

}
