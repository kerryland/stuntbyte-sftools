package com.fidelma.salesforce.misc;

import com.sforce.soap.metadata.MetadataConnection;
import org.junit.Test;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 22/05/11
 * Time: 7:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class DownloaderTests {

    @Test
    public void testDownload() throws Exception {

        LoginHelper lh = new LoginHelper(TestHelper.loginUrl,
                TestHelper.username, TestHelper.password);

        String dir = System.getProperty("java.io.tmpdir");
        File crcFile = File.createTempFile("CRC", "x");

        System.out.println(dir);
//        crcFile.deleteOnExit();

        Reconnector rc = new Reconnector(lh);
//        MetadataConnection metaConnection = lh.getMetadataConnection();
        Downloader dl = new Downloader(rc, new File(dir), new Notice(), crcFile);
        dl.addPackage("CustomObject", "Lead");
        dl.addPackage("CustomObject", "aaa__c");
        dl.download();

        // TODO: Check content of CRC and disk file

    }

    private class Notice implements DeploymentEventListener {

        public void error(String message) {
            System.out.println("BANG " + message);

        }

        public void finished(String message) {
            System.out.println("DONE " + message);
        }

        public void progress(String message) {

        }
    }
}
