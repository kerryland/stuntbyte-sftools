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

import junit.framework.Assert;
import org.junit.Test;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.List;

/**
 * Test we can package up code for deployment.
 */
public class DeploymentTests {

    @Test
    public void testDeployment() throws Exception {

        Deployment deployment = new Deployment(22d);
        deployment.addMember("ApexClass", "Wibble", "class Wibble {}", null);
        deployment.addMember("ApexClass", "Wobble", "class Wobble {}", null);
        deployment.dropMember("ApexClass", "DeadMeat");

        String xml = deployment.getPackageXml();
        LineNumberReader lnr = new LineNumberReader(new StringReader(xml));
        String line = lnr.readLine();
        String trimmedXml = "";
        while (line != null) {
            trimmedXml += line;
            line = lnr.readLine();
        }

        Assert.assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                "<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">" +
                        "<types>" +
                        "<members>Wibble</members>" +
                        "<members>Wobble</members>" +
                        "<name>ApexClass</name>" +
                        "</types>" +
                        "<version>22.0</version></Package>",

                trimmedXml);

        List<DeploymentResource> resources = deployment.getDeploymentResources();
        for (DeploymentResource resource : resources) {
            Assert.assertTrue(resource.getFilepath().startsWith("classes/W"));
            Assert.assertTrue(resource.getFilepath().endsWith("bble.cls"));
            Assert.assertTrue(resource.getCode().startsWith("class W"));
//            Assert.assertEquals(
//                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
//                    "<ApexClass xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n" +
//                    "    <apiVersion>20.0</apiVersion>\n" +
//                    "    <status>Active</status>\n" +
//                    "</ApexClass>",
//                    resource.getMetaData());
        }

        xml = deployment.getDestructiveChangesXml();
        lnr = new LineNumberReader(new StringReader(xml));
        line = lnr.readLine();
        trimmedXml = "";
        while (line != null) {
            trimmedXml += line;
            line = lnr.readLine();
        }

        Assert.assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                "<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">" +
                        "<types>" +
                        "<members>DeadMeat</members>" +
                        "<name>ApexClass</name>" +
                        "</types>" +
                        "<version>22.0</version></Package>",

                trimmedXml);
    }
}
