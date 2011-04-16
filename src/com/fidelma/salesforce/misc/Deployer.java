package com.fidelma.salesforce.misc;

import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.RetrieveMessage;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.rmi.RemoteException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * This class looks after deploying stuff to Salesforce
 */
public class Deployer {

    private static final double WSDL_VERSION = 20D;
    private MetadataConnection metadatabinding;

    public Deployer(MetadataConnection metadatabinding) {
        this.metadatabinding = metadatabinding;
    }


    public void uploadNonCode(String nonCodeType, String filename, String srcDirectory, String code, DeploymentEventListener listener) throws Exception {
        File deploymentFile = File.createTempFile("SFDC", "zip");
        createDeploymentFile(nonCodeType, filename, srcDirectory, code, deploymentFile, "package.xml");
        deployZip(deploymentFile, listener);
    }

    public void dropNonCode(String nonCodeType, String filename, DeploymentEventListener listener) throws Exception {
        File deploymentFile = File.createTempFile("SFDC", "zip");
        createDeploymentFile(nonCodeType, filename, null, null, deploymentFile, "destructiveChanges.xml");
        deployZip(deploymentFile, listener);
    }


    private void createDeploymentFile(String nonCodeType,
                                      String filename,
                                      String srcDirectory,
                                      String apexCode,
                                      File deploymentFile,
                                      String packageXmlName) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(deploymentFile));

        String fileAndDirectory = determineDirectory(filename).replace("\\", "/");

        filename = new File(filename).getName();
        String nonSuffixedFilename = filename.substring(0, filename.lastIndexOf("."));

        String packageXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n" +
                        "    <types>\n" +
                        "        <members>" + nonSuffixedFilename + "</members>\n" +
                        "        <name>" + nonCodeType + "</name>\n" +
                        "    </types>\n" +
                        "    <version>" + WSDL_VERSION + "</version>\n" +
                        "</Package>";

        out.putNextEntry(new ZipEntry(packageXmlName));
        out.write(packageXml.getBytes());
        out.closeEntry();

        // Destructive changes need an empty package.xml
        if (packageXmlName.equals("destructiveChanges.xml")) {
            packageXml =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n" +
                            "    <version>" + WSDL_VERSION + "</version>\n" +
                            "</Package>";

            out.putNextEntry(new ZipEntry("package.xml"));
            out.write(packageXml.getBytes());
            out.closeEntry();
        }


        // TODO: What about creating meta for a new class?
        if (srcDirectory != null) {
            File metaFile = new File(srcDirectory, fileAndDirectory + "-meta.xml");
            FileInputStream meta = new FileInputStream(metaFile);

            if (metaFile.exists()) {
                int bytes = meta.read();
                out.putNextEntry(new ZipEntry(fileAndDirectory + "-meta.xml"));
                while (bytes != -1) {
                    out.write(bytes);
                    bytes = meta.read();
                }
                out.closeEntry();
            }
        }

        if (apexCode != null) {
            out.putNextEntry(new ZipEntry(fileAndDirectory));
            out.write(apexCode.getBytes());
            out.closeEntry();
        }
        out.close();
    }

    private String determineDirectory(String filename) {
        File parent = new File(filename).getParentFile();
        File child = new File(parent.getName(), new File(filename).getName());

        return child.getPath();
    }


    private static final long ONE_SECOND = 1000;
    // maximum number of attempts to deploy the zip file

    private static final int MAX_NUM_POLL_REQUESTS = 50;


    private void deployZip(File zipFile, DeploymentEventListener listener) throws Exception {
//           System.out.println("FILE IS " + zipFile.getAbsolutePath());
        byte zipBytes[] = readZipFile(zipFile);
        DeployOptions deployOptions = new DeployOptions();

        deployOptions.setPerformRetrieve(false);
        deployOptions.setRollbackOnError(true);
        deployOptions.setSinglePackage(true);

//           deployOptions.setAllowMissingFiles(true);

        AsyncResult asyncResult = metadatabinding.deploy(zipBytes, deployOptions);

        // Wait for the deploy to complete
        int poll = 0;
        long waitTimeMilliSecs = ONE_SECOND;
        while (!asyncResult.isDone()) {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration

            waitTimeMilliSecs *= 2;
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new Exception("Request timed out. If this is a large set " +
                        "of metadata components, check that the time allowed by " +
                        "MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            asyncResult = metadatabinding.checkStatus(
                    new String[]{asyncResult.getId()})[0];
//               System.out.println("Status is: " + asyncResult.getState());
        }

        if (asyncResult.getState() != AsyncRequestState.Completed) {
            throw new Exception(asyncResult.getStatusCode() + " msg: " +
                    asyncResult.getMessage());
        }

        DeployResult result = metadatabinding.checkDeployStatus(asyncResult.getId());
        if (!result.isSuccess()) {
            DeployMessage[] errors = result.getMessages();

            for (DeployMessage error : errors) {
                listener.heyListen("ERROR: " + error.getProblem());
            }
//               printErrors(result);            TODO
            throw new Exception("The files were not successfully deployed");
        }

        listener.heyListen("The file " + zipFile.getName() + " was successfully deployed");
    }


    /**
     * Read in the zip file contents into a byte array.
     *
     * @return byte[]
     * @throws Exception - if cannot find the zip file to deploy
     */
    private byte[] readZipFile(File deployZip)
            throws Exception {
        if (!deployZip.exists() || !deployZip.isFile())
            throw new Exception("Cannot find the zip file to deploy. Looking for " +
                    deployZip.getAbsolutePath());

        FileInputStream fos = new FileInputStream(deployZip);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int readbyte = -1;
        while ((readbyte = fos.read()) != -1) {
            bos.write(readbyte);
        }
        fos.close();
        bos.close();
        return bos.toByteArray();
    }


    public File retrieveZip(RetrieveRequest retrieveRequest, DeploymentEventListener listener) throws Exception {
//        RetrieveRequest retrieveRequest = new RetrieveRequest();
//        retrieveRequest.setApiVersion(WSDL_VERSION);
//        setUnpackaged(retrieveRequest);

        File resultsFile = File.createTempFile("SFDC", "DOWN");

        AsyncResult asyncResult = metadatabinding.retrieve(retrieveRequest);
        // Wait for the retrieve to complete

        int poll = 0;
        long waitTimeMilliSecs = ONE_SECOND;
        while (!asyncResult.isDone()) {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration

            waitTimeMilliSecs *= 2;
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new Exception("Request timed out.  If this is a large set " +
                        "of metadata components, check that the time allowed " +
                        "by MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            asyncResult = metadatabinding.checkStatus(
                    new String[]{asyncResult.getId()})[0];
//            System.out.println("Status is: " + asyncResult.getState());
        }

        if (asyncResult.getState() != AsyncRequestState.Completed) {
            throw new Exception(asyncResult.getStatusCode() + " msg: " +
                    asyncResult.getMessage());
        }

        RetrieveResult result = metadatabinding.checkRetrieveStatus(asyncResult.getId());

        // Print out any warning messages

        StringBuilder buf = new StringBuilder();
        if (result.getMessages() != null) {
            for (RetrieveMessage rm : result.getMessages()) {
                buf.append(rm.getFileName() + " - " + rm.getProblem());
            }
        }
        if (buf.length() > 0) {
            listener.heyListen("Retrieve warnings:\n" + buf);
        }

        // Write the zip to the file system

//        System.out.println("Writing results to zip file");
        ByteArrayInputStream bais = new ByteArrayInputStream(result.getZipFile());
        FileOutputStream os = new FileOutputStream(resultsFile);
        try {
            ReadableByteChannel src = Channels.newChannel(bais);
            FileChannel dest = os.getChannel();
            copy(src, dest);

//            System.out.println("Results written to " + resultsFile.getAbsolutePath());
        } finally {
            os.close();
        }

        return resultsFile;
    }


    public void unzipFile(String srcDir, File zipFile) throws Exception {
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


}
