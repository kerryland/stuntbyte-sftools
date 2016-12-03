package com.stuntbyte.salesforce.misc;

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
                TestHelper.username, TestHelper.password, TestHelper.licence);

        String dir = System.getProperty("java.io.tmpdir");
        File crcFile = File.createTempFile("CRC", "x");

//        crcFile.deleteOnExit();

        Reconnector rc = new Reconnector(lh);
        Downloader dl = new Downloader(rc, new File(dir), new Notice(), crcFile);
        dl.addPackage("CustomObject", "Lead");
        File dlDir = dl.download();
        FileUtil.delete(dlDir);

        Properties properties = new Properties();
        FileReader fr = new FileReader(crcFile);
        properties.load(fr);
        fr.close();
        Assert.assertTrue(properties.getProperty("Lead.object") != null);
        Assert.assertTrue(new File(dir + "/objects/Lead.object").exists());

        FileUtil.delete(crcFile);
    }

    private class Notice extends BaseDeploymentEventListener {
        public void error(String message) {
            throw new RuntimeException("No error should be generated");
        }
    }
}
