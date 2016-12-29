/**
 * The MIT License
 * Copyright © 2011-2017 Kerry Sainsbury
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.stuntbyte.salesforce.deployment;

import com.stuntbyte.salesforce.misc.LoginHelper;
import com.stuntbyte.salesforce.misc.TestHelper;
import junit.framework.Assert;
import org.junit.Test;

import java.util.HashSet;
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

        LoginHelper lh = new LoginHelper(TestHelper.loginUrl,
                TestHelper.username, TestHelper.password);

        DeploymentTestHelper dth = new DeploymentTestHelper(lh);
        String source = "public class Wibble {\n" +
                "   static testMethod void testIt() {\n" +
                "      System.assertEquals(" + cnt + ", 1000);\n" +
                "   }\n" +
                "}";

        dth.deployCode("Wibble", source, deploymentOptions);

        String back = dth.downloadCode("Wibble.cls");

        boolean deployed = source.trim().equals(back);
        Assert.assertEquals("Test " + cnt + " expected to deploy code for " + msg, shouldDeploy, deployed);
    }
}
