package com.fidelma.salesforce.deployment;

import com.fidelma.salesforce.misc.LoginHelper;
import com.fidelma.salesforce.misc.Reconnector;
import com.fidelma.salesforce.misc.TestHelper;
import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Test we can deploy code.
 */
public class DeployerTests {

    @Test
    public void testDeployment() throws Exception {

        Deployment deployment = new Deployment();
        deployment.addMember("ApexClass", "Wibble", "public class Wibble {}",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ApexClass xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n" +
                "    <apiVersion>20.0</apiVersion>\n" +
                "    <status>Active</status>\n" +
                "</ApexClass>");

        LoginHelper lh = new LoginHelper(TestHelper.loginUrl,
                TestHelper.username, TestHelper.password);

        DepListen depListen = new DepListen();

        Reconnector reconnector = new Reconnector(lh);
        Deployer deployer = new Deployer(reconnector);
        deployer.deploy(deployment, depListen);

        for (String error : depListen.errors) {
            System.out.println("ERROR: " + error);
        }
        Assert.assertEquals(0, depListen.errors.size());

    }

    private class DepListen implements DeploymentEventListener {
        List<String> errors = new ArrayList<String>();
        List<String> messages = new ArrayList<String>();

        public void progress(String message) throws IOException {

        }

        public void error(String message) throws Exception {
            errors.add(message);
        }

        public void message(String message) throws IOException {
            messages.add(message);
        }
    }


    @Test
    public void testDeploymentWithError() throws Exception {

        Deployment deployment = new Deployment();
        deployment.addMember("ApexClass", "Wibble", "class Wibble { I_AM_A_COMPILATION_ERROR }", null);



    }


    @Test
    public void testDeploymentWithErrorIgnored() throws Exception {
    }

    @Test
    public void testDeploymentWithWarning() throws Exception {
    }


    @Test
    public void testDeploymentWithWarningIgnored() throws Exception {
    }


}
