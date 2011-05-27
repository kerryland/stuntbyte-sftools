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
import java.util.List;
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


    public void uploadNonCode(String nonCodeType, String filename,
                              String srcDirectory, String code,
                              DeploymentEventListener listener) throws Exception {

        filename = new File(filename).getName();
        String nonSuffixedFilename = filename.substring(0, filename.lastIndexOf("."));

        Deployment deployment = new Deployment();
        deployment.addMember(nonCodeType, nonSuffixedFilename, code);
        deployment.assemble();

        deploy(deployment, listener);
    }

    public void dropNonCode(String nonCodeType, String filename, DeploymentEventListener listener) throws Exception {
        filename = new File(filename).getName();
        String nonSuffixedFilename = filename.substring(0, filename.lastIndexOf("."));

        Deployment deployment = new Deployment();
        deployment.addMember(nonCodeType, nonSuffixedFilename, null);
        deployment.assemble();

        undeploy(deployment, listener);
    }

    public void deploy(Deployment deployment, DeploymentEventListener listener) throws Exception {
        deploy(deployment, listener, "package.xml");
    }

    public void undeploy(Deployment deployment, DeploymentEventListener listener) throws Exception {
        deploy(deployment, listener, "destructiveChanges.xml");
    }


    private void deploy(Deployment deployment, DeploymentEventListener listener, String packageXmlName) throws Exception {
        File deploymentFile = File.createTempFile("SFDC", "zip");

        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(deploymentFile));

        out.putNextEntry(new ZipEntry(packageXmlName));
        out.write(deployment.getPackageXml().getBytes());
        out.closeEntry();

        // Destructive changes need an empty package.xml
        if (packageXmlName.equals("destructiveChanges.xml")) {
            Deployment dummy = new Deployment();
            dummy.assemble();
            out.putNextEntry(new ZipEntry("package.xml"));
            out.write(dummy.getPackageXml().getBytes());
            out.closeEntry();
        }


        List<DeploymentResource> resources = deployment.getDeploymentResources();
        for (DeploymentResource resource : resources) {
            String metaData = resource.getMetaData();
            if (metaData != null) {
                ByteArrayInputStream meta = new ByteArrayInputStream(metaData.getBytes());

                int bytes = meta.read();
                out.putNextEntry(new ZipEntry(resource.getFilepath() + "-meta.xml"));
                while (bytes != -1) {
                    out.write(bytes);
                    bytes = meta.read();
                }
                out.closeEntry();
            }

            if (resource.getCode() != null) {
                out.putNextEntry(new ZipEntry(resource.getFilepath()));
                out.write(resource.getCode().getBytes());
                out.closeEntry();
            }
        }

        out.close();

        deployZip(deploymentFile, listener);
    }


//    private String determineDirectory(String filename) {
//        File parent = new File(filename).getParentFile();
//        File child = new File(parent.getName(), new File(filename).getName());
//
//        return child.getPath();
//    }


    private static final long ONE_SECOND = 1000;
    // maximum number of attempts to deploy the zip file

    private static final int MAX_NUM_POLL_REQUESTS = 50;


    public void deployZip(File zipFile, DeploymentEventListener listener) throws Exception {
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
                if (!error.getSuccess()) {
                    listener.error(error.getProblemType().name() + ": " + error.getFullName() + " " + error.getProblem());
                }
            }
//               printErrors(result);            TODO

//            throw new Exception("The files were not successfully deployed");
        }

//        listener.finished("The file " + zipFile.getName() + " was successfully deployed");
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
                throw new Exception("Request timed out. Make or may not have completed successfully...");
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
