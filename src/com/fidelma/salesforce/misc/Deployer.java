package com.fidelma.salesforce.misc;

import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * This class looks after deploying stuff to Salesforce
 */
public class Deployer {

    private MetadataConnection metadatabinding;

    public Deployer(MetadataConnection metadatabinding) {
        this.metadatabinding = metadatabinding;
    }


    public void uploadNonCode(String nonCodeType, String filename,
                              String srcDirectory, String code,
                              String metaData, DeploymentEventListener listener) throws Exception {

        filename = new File(filename).getName();
        String nonSuffixedFilename = filename.substring(0, filename.lastIndexOf("."));

        Deployment deployment = new Deployment();
        deployment.addMember(nonCodeType, nonSuffixedFilename, code, metaData);

        deploy(deployment, listener);
    }

    public void deploy(Deployment deployment, DeploymentEventListener listener) throws Exception {
        deploy(deployment, listener, "package.xml");
    }

    public void undeploy(Deployment deployment, DeploymentEventListener listener) throws Exception {
        deploy(deployment, listener, "destructiveChanges.xml");
    }


    private void deploy(Deployment deployment, DeploymentEventListener listener, String packageXmlName) throws Exception {
//        deployment.assemble();
        File deploymentFile = File.createTempFile("SFDC", "zip");
        System.out.println("Deployment file " + deploymentFile.getName());

        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(deploymentFile));

        out.putNextEntry(new ZipEntry(packageXmlName));
        out.write(deployment.getPackageXml().getBytes());
        out.closeEntry();

        // Destructive changes need an empty package.xml
        if (packageXmlName.equals("destructiveChanges.xml")) {
            Deployment dummy = new Deployment();
//            dummy.assemble();
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

        String deploymentId = deployZip(deploymentFile, new HashSet<DeploymentOptions>());
        checkDeploymentComplete(deploymentId, listener);
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

    public enum DeploymentOptions {
        UNPACKAGED_TESTS,
        ALL_TESTS, // TODO
        IGNORE_ERRORS

    }


    public String deployZip(File zipFile, Set<DeploymentOptions> deploymentOptions) throws Exception {
        byte zipBytes[] = readZipFile(zipFile);
        DeployOptions deployOptions = new DeployOptions();

        deployOptions.setPerformRetrieve(false);
        deployOptions.setRollbackOnError(!deploymentOptions.contains(Deployer.DeploymentOptions.IGNORE_ERRORS));
        deployOptions.setSinglePackage(true);

        if (deploymentOptions.contains(DeploymentOptions.UNPACKAGED_TESTS)) {
            ListMetadataQuery mdq = new ListMetadataQuery();
            mdq.setType("ApexClass");

            FileProperties[] codeFiles = metadatabinding.listMetadata(new ListMetadataQuery[]{mdq}, LoginHelper.SFDC_VERSION);
            List<String> testFiles = new ArrayList<String>();
            for (FileProperties fileProperties : codeFiles) {
                if ((fileProperties.getFullName().endsWith("Test")) ||
                        (fileProperties.getFullName().endsWith("Tests"))) {
                    testFiles.add(fileProperties.getFullName());
                }
            }
            String[] testFileArray = new String[testFiles.size()];
            testFiles.toArray(testFileArray);
            deployOptions.setRunTests(testFileArray);
        }

//           deployOptions.setAllowMissingFiles(true);

        AsyncResult asyncResult = metadatabinding.deploy(zipBytes, deployOptions);

        String deploymentId = asyncResult.getId();

        return deploymentId;


        //checkDeploymentComplete(deploymentId, listener);
    }


    public AsyncResult checkDeploymentComplete(String deploymentId, DeploymentEventListener listener) throws Exception {
        // Wait for the deploy to complete
        listener.progress("Awaiting deployment of id=" + deploymentId + "\n");

        long lastChangeTime = System.currentTimeMillis();
        String lastChangeValue = "";

        long waitTimeMilliSecs = ONE_SECOND;
        long FIVE_MINUTES = 60000 * 5;

        AsyncResult asyncResult = null;

        boolean finished = false;

        while (!finished) {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration,
            // but no more that 10 secs per iteration

            if (waitTimeMilliSecs >= 8000) {
                waitTimeMilliSecs = 10000;
            } else {
                waitTimeMilliSecs *= 2;
            }

            if (System.currentTimeMillis() > lastChangeTime + FIVE_MINUTES) {
                throw new Exception("Request timed out. Check deployment state within Salesforce");
            }
            asyncResult = metadatabinding.checkStatus(new String[]{deploymentId})[0];

            String msg = asyncResult.getState() + " " +
                    "Deployed: " + asyncResult.getNumberComponentsDeployed() + " of " +
                    asyncResult.getNumberComponentsTotal() + " (errors " +
                    asyncResult.getNumberComponentErrors() + ") " +

                    "Tests: " + asyncResult.getNumberTestsCompleted() + " of " +
                    asyncResult.getNumberTestsTotal() + " (errors " +
                    asyncResult.getNumberTestErrors() + ")";
            if (asyncResult.getMessage() != null) {
                msg += asyncResult.getMessage();
            }

            listener.progress(msg);

            String thisChangeValue = asyncResult.getMessage() +
                    asyncResult.getState() +
                    asyncResult.getNumberComponentsDeployed() +
                    asyncResult.getNumberComponentErrors() +
                    asyncResult.getNumberTestsCompleted() +
                    asyncResult.getNumberTestErrors();

            if (!thisChangeValue.equals(lastChangeValue)) {
                lastChangeTime = System.currentTimeMillis();
                lastChangeValue = thisChangeValue;
            }

            finished = asyncResult.isDone();
        }

        if (asyncResult.getState() != AsyncRequestState.Completed) {
            throw new Exception(asyncResult.getStatusCode() + " msg: " +
                    asyncResult.getMessage());
        }

        DeployResult result = metadatabinding.checkDeployStatus(deploymentId);
        if (!result.isSuccess()) {
            DeployMessage[] errors = result.getMessages();

            for (DeployMessage error : errors) {
                if (!error.getSuccess()) {
                    listener.error(error.getProblemType().name() + ": " + error.getFullName() + " " + error.getProblem());
                }
            }
        }

//        if (deploymentOptions.contains(DeploymentOptions.UNPACKAGED_TESTS)) {
        com.sforce.soap.metadata.RunTestsResult res = result.getRunTestResult();
        if (res != null) {
            listener.finished("Number of tests: " + res.getNumTestsRun() + "\n");
            listener.finished("Number of failures: " + res.getNumFailures() + "\n");
            if (res.getNumFailures() > 0) {
                for (com.sforce.soap.metadata.RunTestFailure rtf : res.getFailures()) {
                    listener.finished("Failure: " + (rtf.getNamespace() ==
                            null ? "" : rtf.getNamespace() + ".")
                            + rtf.getName() + "." + rtf.getMethodName() + ": "
                            + rtf.getMessage() + "\n" + rtf.getStackTrace() + "\n");
                }
            }
        }
        return asyncResult;
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


    // TODO: Use downloader.retrieveZip instead
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
