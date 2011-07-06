package com.fidelma.salesforce.misc;

import junit.framework.Assert;
import org.junit.Test;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 1/05/11
 * Time: 7:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class DeploymentTests {

    @Test
    public void testDeployment() throws Exception {

        Deployment deployment = new Deployment();
        deployment.addMember("ApexClass", "Wibble", "class Wibble {}", null);
        deployment.addMember("ApexClass", "Wobble", "class Wobble {}", null);
//        deployment.assemble();


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

    }

}
