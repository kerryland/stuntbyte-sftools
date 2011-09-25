package com.fidelma.salesforce.deployment;

import com.fidelma.salesforce.misc.BaseDeploymentEventListener;
import com.fidelma.salesforce.misc.Downloader;
import com.fidelma.salesforce.misc.FileUtil;
import com.fidelma.salesforce.misc.LoginHelper;
import com.fidelma.salesforce.misc.Reconnector;
import com.fidelma.salesforce.misc.TestHelper;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.management.monitor.StringMonitor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test we can deploy code.
 */
public class DeployerTests {

    @Test
    public void testDeployment() throws Exception {

        Set<Deployer.DeploymentOptions> deploymentOptions;

        deploymentOptions = new HashSet<Deployer.DeploymentOptions>();
        checkit(1, "Default, no test", deploymentOptions, true);    // Deploys

        deploymentOptions = new HashSet<Deployer.DeploymentOptions>();
        deploymentOptions.add(Deployer.DeploymentOptions.IGNORE_ERRORS);
        checkit(2, "Ignore errors, no test", deploymentOptions, true); // Deploys

        deploymentOptions = new HashSet<Deployer.DeploymentOptions>();
        deploymentOptions.add(Deployer.DeploymentOptions.IGNORE_WARNINGS);
        checkit(3, "Ignore Warnings, no test", deploymentOptions, true); // Deploys

        deploymentOptions = new HashSet<Deployer.DeploymentOptions>();
        deploymentOptions.add(Deployer.DeploymentOptions.IGNORE_WARNINGS);
        checkit(4, "Ignore Warnings and errors, no test", deploymentOptions, true); // Deploys

        //--------------------------------

        deploymentOptions = new HashSet<Deployer.DeploymentOptions>();
        deploymentOptions.add(Deployer.DeploymentOptions.DEPLOYED_TESTS);
        checkit(5, "Default with test", deploymentOptions, false); // Should fail

        deploymentOptions = new HashSet<Deployer.DeploymentOptions>();
        deploymentOptions.add(Deployer.DeploymentOptions.IGNORE_ERRORS);
        deploymentOptions.add(Deployer.DeploymentOptions.DEPLOYED_TESTS);
        checkit(6, "Ignore errors with test", deploymentOptions, true); // Deploys

        deploymentOptions = new HashSet<Deployer.DeploymentOptions>();
        deploymentOptions.add(Deployer.DeploymentOptions.IGNORE_WARNINGS);
        deploymentOptions.add(Deployer.DeploymentOptions.DEPLOYED_TESTS);
        checkit(7, "Ignore Warnings with test", deploymentOptions, false); // Does NOT Deploy

        deploymentOptions = new HashSet<Deployer.DeploymentOptions>();
        deploymentOptions.add(Deployer.DeploymentOptions.IGNORE_WARNINGS);
        deploymentOptions.add(Deployer.DeploymentOptions.IGNORE_ERRORS);
        deploymentOptions.add(Deployer.DeploymentOptions.DEPLOYED_TESTS);
        checkit(8, "Ignore Errors and Warnings with test", deploymentOptions, true); // Does deploy
    }


    private void checkit(int cnt, String msg, Set<Deployer.DeploymentOptions> deploymentOptions, boolean shouldDeploy) throws Exception {

        String source = "public class Wibble {\n" +
                "   static testMethod void testIt() {\n" +
                "      System.assertEquals(" + cnt + ", 1000);\n" +
                "   }\n" +
                "}";

        deployCode(source, deploymentOptions);

        String back = downloadCode();

        boolean deployed = source.trim().equals(back);
        Assert.assertEquals("Test " + cnt + " expected to deploy code for " + msg, shouldDeploy, deployed);
    }


    private DepListen deployCode(String code, Set<Deployer.DeploymentOptions> deploymentOptions) throws Exception {
        Deployment deployment = new Deployment();
        deployment.addMember("ApexClass", "Wibble", code,
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
        deployer.deploy(deployment, depListen, deploymentOptions);

        for (String error : depListen.errors) {
            System.out.println("ERROR: " + error);
        }
        for (String error : depListen.messages) {
            System.out.println("MESSAGE: " + error);
        }

        return depListen;
    }


    private String downloadCode() throws Exception {
        Reconnector reconnector = new Reconnector(new LoginHelper(TestHelper.loginUrl,
                TestHelper.username,
                TestHelper.password
        ));

        File sourceSchemaDir = FileUtil.createTempDirectory("TEST");

        DeploymentEventListener listener = new BaseDeploymentEventListener();
        Downloader dl = new Downloader(reconnector, sourceSchemaDir, listener, null);
        dl.addPackage("ApexClass", "Wibble");

        dl.download();

        return FileUtil.loadTextFile(new File(sourceSchemaDir, "classes/Wibble.cls"));
    }


    private class DepListen extends BaseDeploymentEventListener {
        List<String> errors = new ArrayList<String>();
        List<String> messages = new ArrayList<String>();

        public void error(String message) throws Exception {
            errors.add(message);
        }

        public void message(String message) throws IOException {
            messages.add(message);
        }
    }

}
