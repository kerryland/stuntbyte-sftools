package com.fidelma.salesforce.misc;

import com.sforce.soap.metadata.*;
import com.sforce.soap.metadata.Package;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Download Salesforce metadata
 */
public class Downloader {
    private MetadataConnection metaDataConnection;
    private File srcDir;
    private DeploymentEventListener listener;
    private File crcFile;
    private Properties crcs = new Properties();

    private Map<String, List<String>> metaDataFiles = new HashMap<String, List<String>>();
    private File zipFile;


    public Downloader(MetadataConnection metaDataConnection,
                      File srcDir,
                      DeploymentEventListener listener,
                      File crcFile) throws IOException {
        this.metaDataConnection = metaDataConnection;
        this.srcDir = srcDir;
        this.listener = listener;
        this.crcFile = crcFile;

        if ((crcFile != null) && (crcFile.exists())) {
            crcs.load(new FileReader(crcFile));
        }
    }


    // http://www.salesforce.com/us/developer/docs/daas/Content/daas_package.htm
    public void addPackage(String metadataType, String name) {
        List<String> files = metaDataFiles.get(metadataType);
        if (files == null) {
            files = new ArrayList<String>();
            metaDataFiles.put(metadataType, files);
        }

        files.add(name);
    }

    public void download() throws Exception {
        System.out.println("HERE " + metaDataFiles.keySet().size());
        for (String metadataType : metaDataFiles.keySet()) {
            List<String> files = metaDataFiles.get(metadataType);

            // TODO: Split out package type members so we create one big file
            // and only do one request/download/unzip regardless of number of metadatatypes
            com.sforce.soap.metadata.Package p = createPackage(metadataType, files);
            RetrieveRequest retrieveRequest = prepareRequest(true, null, p);

            zipFile = retrieveZip(retrieveRequest, listener);
            // Find our file and rewrite the local one
            unzipFile(srcDir, zipFile);
            if (crcFile != null) {
                updateCrcs(crcs, zipFile);
                crcs.store(new FileWriter(crcFile), "Generated file");
            }
        }
        System.out.println("HERE DONE");
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
    }


    private com.sforce.soap.metadata.Package createPackage(String mType, List itemSet) {
        com.sforce.soap.metadata.Package p = new com.sforce.soap.metadata.Package();
        PackageTypeMembers pd = new PackageTypeMembers();
        pd.setName(mType);
        pd.setMembers((String[]) itemSet.toArray(new String[itemSet.size()]));
        p.setTypes(new PackageTypeMembers[]{
                pd
        });
        return p;
    }


    private RetrieveRequest prepareRequest(boolean isSinglePackage, String packageNames[], Package componentManifest) {
        RetrieveRequest retrieveRequest = new RetrieveRequest();
        retrieveRequest.setApiVersion(LoginHelper.WSDL_VERSION);
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


    public File retrieveZip(RetrieveRequest retrieveRequest, DeploymentEventListener listener) throws Exception {
//        RetrieveRequest retrieveRequest = new RetrieveRequest();
//        retrieveRequest.setApiVersion(WSDL_VERSION);
//        setUnpackaged(retrieveRequest);

        File resultsFile = File.createTempFile("SFDC", "DOWN");

        AsyncResult asyncResult = metaDataConnection.retrieve(retrieveRequest);
        // Wait for the retrieve to complete

        int poll = 0;
        long waitTimeMilliSecs = ONE_SECOND;
        while (!asyncResult.isDone()) {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration

            waitTimeMilliSecs *= 2;
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new Exception("Request timed out. Make or may not have completed successfully...");
            }
            asyncResult = metaDataConnection.checkStatus(
                    new String[]{asyncResult.getId()})[0];
//            System.out.println("Status is: " + asyncResult.getState());
        }

        if (asyncResult.getState() != AsyncRequestState.Completed) {
            throw new Exception(asyncResult.getStatusCode() + " msg: " +
                    asyncResult.getMessage());
        }

        RetrieveResult result = metaDataConnection.checkRetrieveStatus(asyncResult.getId());

        // Print out any warning messages

        StringBuilder buf = new StringBuilder();
        if (result.getMessages() != null) {
            for (RetrieveMessage rm : result.getMessages()) {
                buf.append(rm.getFileName() + " - " + rm.getProblem());
            }
        }
        if (buf.length() > 0) {
            listener.finished("Retrieve warnings:\n" + buf);
        }

        // Write the zip to the file system

//        System.out.println("Writing results to zip file");
        ByteArrayInputStream bais = new ByteArrayInputStream(result.getZipFile());
        FileOutputStream os = new FileOutputStream(resultsFile);
        try {
            ReadableByteChannel src = Channels.newChannel(bais);
            FileChannel dest = os.getChannel();
            copy(src, dest);

            listener.finished("Results written to " + resultsFile.getAbsolutePath());
        } finally {
            os.close();
        }

        return resultsFile;
    }


    public void unzipFile(File srcDir, File zipFile) throws Exception {
        BufferedOutputStream out = null;
        ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        ZipEntry entry;
        while ((entry = in.getNextEntry()) != null) {
            int count;
            byte data[] = new byte[1000];
            File outputFile = new File(srcDir, entry.getName());
//            outputFile.createNewFile();
            outputFile.getParentFile().mkdirs();
            out = new BufferedOutputStream(new
                    FileOutputStream(outputFile), 1000);
            while ((count = in.read(data, 0, 1000)) != -1) {
                out.write(data, 0, count);
            }
            out.flush();
            out.close();
        }
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

    public File getZipFile() {
        return zipFile;
    }
}
