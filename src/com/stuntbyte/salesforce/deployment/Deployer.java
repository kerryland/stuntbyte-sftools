package com.stuntbyte.salesforce.deployment;

import com.stuntbyte.salesforce.misc.FileUtil;
import com.stuntbyte.salesforce.misc.LoginHelper;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CodeCoverageWarning;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * This class looks after deploying stuff to Salesforce
 */
public class Deployer {

    private Reconnector reconnector;

    public Deployer(Reconnector reconnector) {
        this.reconnector = reconnector;
    }


    public void uploadNonCode(String nonCodeType, String filename,
                              String code,
                              String metaData, DeploymentEventListener listener) throws Exception {

        filename = new File(filename).getName();
        String nonSuffixedFilename = filename.substring(0, filename.lastIndexOf("."));

        Deployment deployment = new Deployment();
        deployment.addMember(nonCodeType, nonSuffixedFilename, code, metaData);

        deploy(deployment, listener);
    }

    public void deploy(Deployment deployment, DeploymentEventListener listener) throws Exception {
        deploy(deployment, listener, new HashSet<DeploymentOptions>());
    }

    public void deploy(Deployment deployment, DeploymentEventListener listener, Set<DeploymentOptions> deploymentOptions) throws Exception {
        File deploymentFile = File.createTempFile("SFDC", "zip");
        // System.out.println("Deployment file " + deploymentFile.getName());

        FileOutputStream fos = new FileOutputStream(deploymentFile);
        ZipOutputStream out = new ZipOutputStream(fos);

        out.putNextEntry(new ZipEntry("package.xml"));
        out.write(deployment.getPackageXml().getBytes());
        out.closeEntry();

        if (deployment.hasDestructiveChanges()) {
            out.putNextEntry(new ZipEntry("destructiveChanges.xml"));
            out.write(deployment.getDestructiveChangesXml().getBytes());
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
        fos.close();

        String deploymentId = deployZip(deploymentFile, deploymentOptions);
        checkDeploymentComplete(deploymentId, listener);
        FileUtil.delete(deploymentFile);
    }


    private static final long ONE_SECOND = 1000;
    // maximum number of attempts to deploy the zip file

    private static final int MAX_NUM_POLL_REQUESTS = 50;

    public enum DeploymentOptions {
        UNPACKAGED_TESTS,
        ALL_TESTS, // TODO
        IGNORE_ERRORS,
        IGNORE_WARNINGS,
        ALLOW_MISSING_FILES,
        DEPLOYED_TESTS,
        CHECK_ONLY
    }


    public String deployZip(File zipFile, Set<DeploymentOptions> deploymentOptions) throws Exception {
        byte zipBytes[] = readZipFile(zipFile);
        DeployOptions deployOptions = new DeployOptions();

        // http://www.salesforce.com/us/developer/docs/api_meta/Content/meta_deploy.htm
        deployOptions.setPerformRetrieve(false);

        boolean ignoreWarnings = deploymentOptions.contains(Deployer.DeploymentOptions.IGNORE_WARNINGS);
        boolean ignoreErrors = deploymentOptions.contains(Deployer.DeploymentOptions.IGNORE_ERRORS);

        Boolean rollbackOnError = !ignoreErrors;

        deployOptions.setIgnoreWarnings(ignoreWarnings);
        deployOptions.setRollbackOnError(rollbackOnError);

        deployOptions.setRunAllTests(deploymentOptions.contains(Deployer.DeploymentOptions.ALL_TESTS));
        deployOptions.setSinglePackage(true);
        deployOptions.setCheckOnly(deploymentOptions.contains(Deployer.DeploymentOptions.CHECK_ONLY));

//        System.out.println("actually....................ignore=" + deployOptions.isIgnoreWarnings() + " rollback=" + deployOptions.isRollbackOnError());

        List<String> testFiles = new ArrayList<String>();

        if (deploymentOptions.contains(DeploymentOptions.DEPLOYED_TESTS)) {
            ZipFile zf = new ZipFile(zipFile);

            for (Enumeration entries = zf.entries(); entries.hasMoreElements();) {
                String zipEntryName = ((ZipEntry) entries.nextElement()).getName();
                if (zipEntryName.startsWith("classes/") && (zipEntryName.endsWith(".cls"))) {
                    testFiles.add(zipEntryName.substring(
                            zipEntryName.indexOf("/") + 1,
                            zipEntryName.lastIndexOf(".")));
                }
            }
            zf.close();
        }

        if (deploymentOptions.contains(DeploymentOptions.UNPACKAGED_TESTS)) {
            ListMetadataQuery mdq = new ListMetadataQuery();
            mdq.setType("ApexClass");

            FileProperties[] codeFiles = reconnector.listMetadata(new ListMetadataQuery[]{mdq}, LoginHelper.SFDC_VERSION);
            for (FileProperties fileProperties : codeFiles) {
                testFiles.add(fileProperties.getFullName());
            }
        }

        if (testFiles.size() > 0) {
            String[] testFileArray = new String[testFiles.size()];
            testFiles.toArray(testFileArray);
            deployOptions.setRunTests(testFileArray);
        }


//           deployOptions.setAllowMissingFiles(true);

        AsyncResult asyncResult = reconnector.deploy(zipBytes, deployOptions);

        String deploymentId = asyncResult.getId();

        return deploymentId;
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
            asyncResult = reconnector.checkStatus(new String[]{deploymentId})[0];

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

            listener.setAsyncResult(asyncResult);

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

        DeployResult result = dumpErrors(deploymentId, listener);

//        if (deploymentOptions.contains(DeploymentOptions.UNPACKAGED_TESTS)) {
        com.sforce.soap.metadata.RunTestsResult res = result.getRunTestResult();
        if (res != null) {
            listener.message("Number of tests: " + res.getNumTestsRun() + "\n");
            listener.message("Number of failures: " + res.getNumFailures() + "\n");
            if (res.getNumFailures() > 0) {
                for (com.sforce.soap.metadata.RunTestFailure rtf : res.getFailures()) {
                    String failMessage = "Failure: " + (rtf.getNamespace() ==
                            null ? "" : rtf.getNamespace() + ".")
                            + rtf.getName() + "." + rtf.getMethodName() + ": "
                            + rtf.getMessage();
                    if (rtf.getStackTrace() != null) {
                        failMessage += "\n" + rtf.getStackTrace() + "\n";
                    }
                    listener.message(failMessage);
                }
            }
            CodeCoverageWarning[] coverageWarnings = res.getCodeCoverageWarnings();
            for (CodeCoverageWarning coverageWarning : coverageWarnings) {
                listener.message("Coverage warning " + coverageWarning.getName() + " " + coverageWarning.getMessage());
            }

//            CodeCoverageResult[] coverage = res.getCodeCoverage();
//            for (CodeCoverageResult codeCoverageResult : coverage) {
//                codeCoverageResult.get
//            }
        }
        return asyncResult;
    }

    private DeployResult dumpErrors(String deploymentId, DeploymentEventListener listener) throws Exception {
        DeployResult result = reconnector.checkDeployStatus(deploymentId);
//        if (!result.isSuccess()) {
            DeployMessage[] errors = result.getMessages();

            for (DeployMessage error : errors) {
                if (!error.getSuccess()) {
                    listener.error(
                            error.getProblemType().name() + ": " + error.getFullName() + " " + error.getProblem() +
                                    " on line " + error.getLineNumber() + " col " + error.getColumnNumber());
                }
            }
//        }
        return result;
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

        AsyncResult asyncResult = reconnector.retrieve(retrieveRequest);
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
            asyncResult = reconnector.checkStatus(
                    new String[]{asyncResult.getId()})[0];
//            System.out.println("Status is: " + asyncResult.getState());
        }

        if (asyncResult.getState() != AsyncRequestState.Completed) {
            throw new Exception(asyncResult.getStatusCode() + " msg: " +
                    asyncResult.getMessage());
        }

        RetrieveResult result = reconnector.checkRetrieveStatus(asyncResult.getId());

        // Print out any warning messages

        StringBuilder buf = new StringBuilder();
        if (result.getMessages() != null) {
            for (RetrieveMessage rm : result.getMessages()) {
                buf.append(rm.getFileName() + " - " + rm.getProblem());
            }
        }
        if (buf.length() > 0) {
            listener.message("Retrieve warnings:\n" + buf);
        }

        // Write the zip to the file system

//        System.out.println("Writing results to zip file");
        ByteArrayInputStream bais = new ByteArrayInputStream(result.getZipFile());
        FileOutputStream os = new FileOutputStream(resultsFile);
        try {
            ReadableByteChannel src = Channels.newChannel(bais);
            FileChannel dest = os.getChannel();
            copy(src, dest);

            listener.message("Results written to " + resultsFile.getAbsolutePath());
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
