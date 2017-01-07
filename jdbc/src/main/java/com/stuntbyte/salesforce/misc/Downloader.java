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

import com.stuntbyte.salesforce.deployment.DeploymentEventListener;
import com.sforce.soap.metadata.*;
import com.sforce.soap.metadata.Package;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Download Salesforce metadata
 */
public class Downloader {
    private Reconnector reconnector;
    private File srcDir;
    private DeploymentEventListener listener;
    private File crcFile;
    private Properties crcs = new Properties();

    private Map<String, Set<String>> metaDataFiles = new HashMap<String, Set<String>>();


    /**
     *
     * @param reconnector  Salesforce connection object
     * @param srcDir       Where do we put the downloaded files. Optional
     * @param listener     Who wants to know about the download progress
     * @param crcFile      Where do we write crc values for the downloaded files. Optional
     * @throws IOException
     */
    public Downloader(Reconnector reconnector,
                      File srcDir,
                      DeploymentEventListener listener,
                      File crcFile) throws IOException {
        this.reconnector = reconnector;
        this.srcDir = srcDir;
        this.listener = listener;
        this.crcFile = crcFile;

        if ((crcFile != null) && (crcFile.exists())) {
            FileReader fr = new FileReader(crcFile);
            crcs.load(fr);
            fr.close();
        }
    }


    // http://www.salesforce.com/us/developer/docs/daas/Content/daas_package.htm
    public void addPackage(String metadataType, String name) {
        Set<String> files = metaDataFiles.get(metadataType);
        if (files == null) {
            files = new HashSet<String>();
            metaDataFiles.put(metadataType, files);
        }

        files.add(name);
    }

    /**
     * Downloads a zip file containing the requested content.
     * Also unzips content to srcDir, if provided.
     * Also updates crc files to crcFile, if provided.
     *
     * @return zipfile with content
     * @throws Exception
     */
    public File download() throws Exception {
        com.sforce.soap.metadata.Package p = new com.sforce.soap.metadata.Package();

        PackageTypeMembers[] packageTypeMembers = new PackageTypeMembers[metaDataFiles.keySet().size()];
        int i = 0;
        for (String metadataType : metaDataFiles.keySet()) {
            Set<String> files = metaDataFiles.get(metadataType);

            PackageTypeMembers pd = new PackageTypeMembers();
            pd.setName(metadataType);
            pd.setMembers(files.toArray(new String[files.size()]));
            packageTypeMembers[i++] = pd;
        }
        p.setTypes(packageTypeMembers);
        RetrieveRequest retrieveRequest = prepareRequest(true, null, p);

        File zipFile = retrieveZip(retrieveRequest, listener);
        // Find our file and rewrite the local one

        if (srcDir != null) {
            unzipFile(srcDir, zipFile);
            if (crcFile != null) {
                updateCrcs(crcs, zipFile);
                FileWriter fw = new FileWriter(crcFile);
                crcs.store(fw, "Generated file");
                fw.close();
            }
        }
        return zipFile;
    }

    private void updateCrcs(Properties crcs, File result) throws IOException {
        ZipFile zip = new ZipFile(result);
        Enumeration ents = zip.entries();
        while (ents.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) ents.nextElement();
            if (!zipEntry.getName().endsWith("-meta.xml")) {
                File f = new File(zipEntry.getName());
                crcs.setProperty(f.getName(), String.valueOf(zipEntry.getCrc()));
            }
        }
        zip.close();
    }


    private RetrieveRequest prepareRequest(boolean isSinglePackage, String packageNames[], Package componentManifest) {
        RetrieveRequest retrieveRequest = new RetrieveRequest();
        retrieveRequest.setApiVersion(reconnector.getSfVersion());
        retrieveRequest.setSinglePackage(isSinglePackage);
        if (packageNames != null && packageNames.length > 0)
            retrieveRequest.setPackageNames(packageNames);
        if (componentManifest != null) {
            retrieveRequest.setUnpackaged(componentManifest);
        }
        return retrieveRequest;
    }


    private static final long ONE_SECOND = 1000;
    // maximum number of attempts to deploy the zip file

    private static final int MAX_NUM_POLL_REQUESTS = 50;


    private File retrieveZip(RetrieveRequest retrieveRequest, DeploymentEventListener listener) throws Exception {
        File resultsFile = File.createTempFile("SFDC", "DOWN");

        AsyncResult asyncResult = reconnector.retrieve(retrieveRequest);
        RetrieveResult deployResult = null;

        deployResult = reconnector.checkRetrieveStatus(asyncResult.getId());

        // Wait for the retrieve to complete
//        boolean done= asyncResult.getDone();

        int poll = 0;
        long waitTimeMilliSecs = ONE_SECOND;
        while (!deployResult.getDone()) {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration

            if (waitTimeMilliSecs >= 8000) {
                waitTimeMilliSecs = 10000;
            } else {
                waitTimeMilliSecs *= 2;
            }

            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new Exception("Request timed out. Make or may not have completed successfully...");
            }
            deployResult = reconnector.checkRetrieveStatus(asyncResult.getId());
//            done = deployResult.getDone();

            listener.progress("Status is: " + deployResult.getStatus().name());
        }

//        if (asyncResult.getState() != AsyncRequestState.Completed) {
//            throw new Exception(asyncResult.getStatusCode() + " msg: " +
//                    asyncResult.getMessage());
//        }

        // TODO: Is deployResult.isSuccess() enough?
        if (deployResult != null && deployResult.getStatus() != RetrieveStatus.Succeeded) {
            throw new Exception(deployResult.getErrorStatusCode() + " msg: " +
                    deployResult.getErrorMessage());
        }


//        RetrieveResult result = reconnector.checkRetrieveStatus(asyncResult.getId());

        // Print out any warning messages

        StringBuilder buf = new StringBuilder();
        if (deployResult.getMessages() != null) {
            for (RetrieveMessage rm : deployResult.getMessages()) {
                buf.append(rm.getFileName() + " - " + rm.getProblem()+"\n");
            }
        }
        if (buf.length() > 0) {
            listener.message("Retrieve warnings:\n" + buf);
        }

        // Write the zip to the file system

//        System.out.println("Writing results to zip file");
        ByteArrayInputStream bais = new ByteArrayInputStream(deployResult.getZipFile());
        FileOutputStream os = new FileOutputStream(resultsFile);
        FileChannel dest = null;
        ReadableByteChannel src = null;
        try {
            src = Channels.newChannel(bais);
            dest = os.getChannel();
            copy(src, dest);

//            listener.message("Results written to " + resultsFile.getAbsolutePath());
        } finally {
            os.close();
            if (dest != null) {
                dest.close();
            }
            if (src != null) {
                src.close();
            }
        }

        return resultsFile;
    }


    public void unzipFile(File srcDir, File zipFile) throws Exception {

        FileInputStream zis = new FileInputStream(zipFile);
        ZipInputStream in = new ZipInputStream(new BufferedInputStream(zis));
        ZipEntry entry;
        while ((entry = in.getNextEntry()) != null) {
            int count;
            byte data[] = new byte[1000];
            File outputFile = new File(srcDir, entry.getName());
            outputFile.getParentFile().mkdirs();

            FileOutputStream fos = new FileOutputStream(outputFile);
            BufferedOutputStream out = new BufferedOutputStream(fos, 1000);
            while ((count = in.read(data, 0, 1000)) != -1) {
                out.write(data, 0, count);
            }
            out.flush();
            out.close();
            fos.close();

        }
        in.close();
        zis.close();
    }

    /**
     * Helper method to copy from a readable channel to a writable channel,
     * using an in-memory buffer.
     */
    private void copy(ReadableByteChannel src, WritableByteChannel dest)
            throws IOException {
        // use an in-memory byte buffer

        ByteBuffer buffer = ByteBuffer.allocate(8092);
        while (src.read(buffer) != -1) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                dest.write(buffer);
            }
            buffer.clear();
        }
    }

}
