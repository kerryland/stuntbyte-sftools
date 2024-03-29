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

import com.stuntbyte.salesforce.misc.BaseDeploymentEventListener;
import com.stuntbyte.salesforce.misc.Downloader;
import com.stuntbyte.salesforce.misc.FileUtil;
import com.stuntbyte.salesforce.misc.LoginHelper;
import com.stuntbyte.salesforce.misc.Reconnector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class for deployment tests
 */
public class DeploymentTestHelper {
    private LoginHelper lh;

    public DeploymentTestHelper(LoginHelper lh) {
        this.lh = lh;
    }

    public DepListen deployCode(String className, String code, Set<Deployer.DeploymentOptions> deploymentOptions) throws Exception {
        Deployment deployment = new Deployment(22d);
        deployment.addMember("ApexClass", className, code,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<ApexClass xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n" +
                        "    <apiVersion>20.0</apiVersion>\n" +
                        "    <status>Active</status>\n" +
                        "</ApexClass>");

        return deploy(deploymentOptions, deployment);
    }

    private DepListen deploy(Set<Deployer.DeploymentOptions> deploymentOptions, Deployment deployment) throws Exception {
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

    public void dropCode(String className) throws Exception {
        Deployment deployment = new Deployment(22d);
        deployment.dropMember("ApexClass", className);

        deploy(new HashSet<Deployer.DeploymentOptions>(), deployment);
    }


    public String downloadCode(String filename) throws Exception {
        Reconnector reconnector = new Reconnector(lh);

        File sourceSchemaDir = FileUtil.createTempDirectory("TEST");

        DeploymentEventListener listener = new BaseDeploymentEventListener();
        Downloader dl = new Downloader(reconnector, sourceSchemaDir, listener, null);
        dl.addPackage("ApexClass", filename.split("\\.")[0]);
        File dir = dl.download();
        FileUtil.delete(dir);

        String code = FileUtil.loadTextFile(new File(sourceSchemaDir, "classes/" + filename));

        FileUtil.delete(sourceSchemaDir);

        return code;
    }


    public class DepListen extends BaseDeploymentEventListener {
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
