package com.fidelma.salesforce.misc;

import junit.framework.Assert;
import org.junit.Test;

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
        deployment.addMember("ApexClass", "Wibble", "class Wibble {}");
        deployment.addMember("ApexClass", "Wobble", "class Wobble {}");
//        deployment.assemble();

        Assert.assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                "<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">" +
                        "<types>" +
                        "<members>Wibble</members>" +
                        "<members>Wobble</members>" +
                        "<name>ApexClass</name>" +
                        "</types>" +
                        "<version>20.0</version></Package>",

                deployment.getPackageXml());

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
