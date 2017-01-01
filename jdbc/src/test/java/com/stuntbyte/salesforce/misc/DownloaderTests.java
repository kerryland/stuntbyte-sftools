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
package com.stuntbyte.salesforce.misc;

import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

/**
 */
public class DownloaderTests {

    private static TestHelper testHelper = new TestHelper();

    @Test
    public void testDownload() throws Exception {

        LoginHelper lh = new LoginHelper(testHelper.getLoginUrl(),
                testHelper.getUsername(), testHelper.getPassword());

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
