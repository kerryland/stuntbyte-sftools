package com.fidelma.salesforce.misc;

import com.fidelma.salesforce.deployment.DeploymentEventListener;
import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

/**
 */
public class DownloaderTests {

    @Test
    public void testDownload() throws Exception {

        LoginHelper lh = new LoginHelper(TestHelper.loginUrl,
                TestHelper.username, TestHelper.password);

        String dir = System.getProperty("java.io.tmpdir");
        File crcFile = File.createTempFile("CRC", "x");

        crcFile.deleteOnExit();

        Reconnector rc = new Reconnector(lh);
        Downloader dl = new Downloader(rc, new File(dir), new Notice(), crcFile);
        dl.addPackage("CustomObject", "Lead");
        dl.download();

        Properties properties = new Properties();
        properties.load(new FileReader(crcFile));
        Assert.assertTrue(properties.getProperty("Lead.object") != null);
        Assert.assertTrue(new File(dir + "/objects/Lead.object").exists());
    }

    private class Notice extends BaseDeploymentEventListener {
        public void error(String message) {
            throw new RuntimeException("No error should be generated");
        }
    }
}
