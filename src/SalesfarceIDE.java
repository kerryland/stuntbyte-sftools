import com.fidelma.salesforce.misc.LoginHelper;
import com.sforce.soap.apex.*;
import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.Package;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RetrieveMessage;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ctags --extra=+f --langmap=java:.cls.trigger --recurse=yes
 * <p/>
 * <p/>
 * :set makeprg=smak.bat\ %
 * :set errorformat=%f>%l:%c:%t:%n:%m
 * <p/>
 * vim -c "set makeprg=smak.bat\ %" -c "set errorformat=%f>%l:%c:%t:%n:%m"
 * <p/>
 * <p/>
 * <p/>
 * :cl
 */
public class SalesfarceIDE {

    private static final double WSDL_VERSION = 20D;
    private double apiversion = 20d;

    private LoginHelper loginHelper;


    /*

    ARGS:

    -downloadAll == download 'everything' (TODO: As defined somewhere, and in one zip!)
    -download    == download just this file
    -force       == force upload, regardless of crc

     */

    public static void main(String[] args) throws Exception {
        String filename = args[0];
        SalesfarceIDE ide = new SalesfarceIDE();

//        for (String arg : args) {
//            System.out.println("ARG: " + arg);
//        }

        String arg = "";
        if (args.length > 1) {
            arg = args[args.length - 1];
        }
        ide.doIt(filename, arg);
    }


    private Properties loadConfig() throws Exception {
        Properties prop = new Properties();
        prop.load(new FileReader("build.properties"));
        prop.load(new FileReader("local.build.properties"));
        String server = prop.getProperty("sf.serverurl");
        String username = prop.getProperty("sf.username");
        String password = prop.getProperty("sf.password");

        loginHelper = new LoginHelper(server, username, password);
        return prop;
    }

    private void doIt(String filename, String arg) throws Exception {
        Properties prop = loadConfig();
        String src = prop.getProperty("src.dir");
        String debugFile = prop.getProperty("debug.file");
        String crcFile = prop.getProperty("crc.file");

        String filenameNoPath = new File(filename).getName();

        if (arg.equals("-download")) {
            downloadFile(src, filename, new File(crcFile));
            return;
        }
        if (arg.equals("-downloadall")) {
            downloadFiles(src, new File(crcFile));
            return;
        }

        if (arg.equals("-diff")) {
            doDiff();
            return;
        }

        if (arg.equals("-runtests")) {
            runTests(src + "/classes");
            return;
        }


        StringBuilder sourceCode = new StringBuilder();
        {
            LineNumberReader read = new LineNumberReader(new FileReader(filename));
            String line = read.readLine();
            while (line != null) {
                sourceCode.append(line).append('\n');
                line = read.readLine();
            }
        }

        // Download latest for checksum
        Properties crcs = new Properties();
        crcs.load(new FileReader(crcFile));

        PackageTypeMembers mems = new PackageTypeMembers();

        String typeName = detemineApexType(filename);

        mems.setName(typeName);
        String noSuffix = getNoSuffix(filename);
        mems.setMembers(new String[]{noSuffix});

//        System.out.println("DOWNLOADING " + typeName + " - " + mems.getMembers()[0]);

        Package p = new Package();
        p.setTypes(new PackageTypeMembers[]{mems});
        RetrieveRequest retrieveRequest = prepareRequest(true, null, p);

        File result = retrieveZip(retrieveRequest);
        ZipFile zip = new ZipFile(result);
        CrcResults crcResults = pullCrcs(zip, crcs, filenameNoPath);

        if ((crcResults.serverCrc != null) && (crcResults.localCrc != null) && (!crcResults.serverCrc.equals(crcResults.localCrc))) {
            if (arg.equals("-force")) {
                System.out.println("Saving even though checksums mismatch");

            } else {
                err(filename, -1, -1, "E", "Code NOT saved due to checksum issue. Use -download to get latest or -force to force upload");
                return;
            }
        }

        if (filename.endsWith(".trigger") || filename.endsWith(".cls")) {
            compileAndUploadCode(filename, prop, src, debugFile, sourceCode);
        } else {
            uploadNonCode(filename, src, sourceCode.toString());
        }

        // Get latest CRCs
        result = retrieveZip(retrieveRequest);
        zip = new ZipFile(result);
        crcResults = pullCrcs(zip, crcs, filenameNoPath);

        // Store checksum locally
        crcs.setProperty(crcResults.crcKey, String.valueOf(crcResults.serverCrc));
        crcs.store(new FileWriter(crcFile), "Automatically generated for " + crcResults.crcKey);
//        System.out.println("STORED CRC OF " + crcResults.serverCrc);


    }

    private CrcResults pullCrcs(ZipFile zip, Properties crcs, String filenameNoPath) {
        Enumeration ents = zip.entries();
        CrcResults result = new CrcResults();

        while (ents.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) ents.nextElement();
            if (zipEntry.getName().endsWith("/" + filenameNoPath)) {
                File f = new File(zipEntry.getName());
                result.crcKey = f.getName();

                result.serverCrc = zipEntry.getCrc();
                result.localCrc = Long.valueOf(crcs.getProperty(result.crcKey));

//                System.out.println("SERVER CRC " + result.crcKey + " " + result.serverCrc + " vs stored " + result.localCrc);
                break;
            }
        }
        return result;
    }

    private class CrcResults {
        String crcKey = null;
        Long serverCrc = null;
        Long localCrc = null;
    }

    private String getNoSuffix(String filename) {
        String noSuffix = new File(filename).getName();
        noSuffix = noSuffix.substring(0, noSuffix.lastIndexOf("."));
        return noSuffix;
    }

    private String detemineApexType(String filename) {
        // Grab the directory name
        String directoryName = new File(filename).getParentFile().getName();

        // Fix "classes", because it's special
        if (directoryName.equals("classes")) {
            return "ApexClass";
        }

        // Chop off the last letter
        String typeName = directoryName.substring(0, directoryName.length() - 1);
        typeName = determineTypeName(typeName);
        return typeName;
    }


    private void uploadNonCode(String filename, String src, String code) throws Exception {

        File deploymentFile = File.createTempFile("SFDC", "zip");

        createDeploymentFile(filename, src, code, deploymentFile);

        deployZip(deploymentFile);
    }


    private static final long ONE_SECOND = 1000;
    // maximum number of attempts to deploy the zip file

    private static final int MAX_NUM_POLL_REQUESTS = 50;

    private void deployZip(File zipFile)
            throws RemoteException, Exception {

//           System.out.println("FILE IS " + zipFile.getAbsolutePath());
        MetadataConnection metadatabinding = loginHelper.getMetadataConnection();

        byte zipBytes[] = readZipFile(zipFile);
        DeployOptions deployOptions = new DeployOptions();

        deployOptions.setPerformRetrieve(false);
        deployOptions.setRollbackOnError(true);
        deployOptions.setSinglePackage(true);

//           deployOptions.setAllowMissingFiles(true);

        // TODO: What about the base64 encoding of zipBytes?

//           zipBytes = (new Base64().encode(new String(zipBytes))).getBytes("UTF-8");

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
                System.out.println("ERROR: " + error.getProblem());
            }
//               printErrors(result);            TODO
            throw new Exception("The files were not successfully deployed");
        }

        System.out.println("The file " + zipFile.getName() + " was successfully deployed");
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


    private void createDeploymentFile(String filename, String src, String code, File deploymentFile) throws IOException {
        String parentName = new File(filename).getParentFile().getName() + "/";

        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(deploymentFile));

        String fileAndDirectory = determineDirectory(filename).replace("\\", "/");


        filename = new File(filename).getName();
        String nonSuffixedFilename = filename.substring(0, filename.lastIndexOf("."));

        String typeName = determineTypeName(filename);

        String packageXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n" +
                        "    <types>\n" +
                        "        <members>" + nonSuffixedFilename + "</members>\n" +
                        "        <name>" + typeName + "</name>\n" +
                        "    </types>\n" +
                        "    <version>" + WSDL_VERSION + "</version>\n" +
                        "</Package>";

//        System.out.println(packageXml);
        out.putNextEntry(new ZipEntry("package.xml"));
        out.write(packageXml.getBytes());
        out.closeEntry();


//        out.putNextEntry(new ZipEntry(parentName));
//        out.closeEntry();

        FileInputStream meta = new FileInputStream(new File(src, fileAndDirectory + "-meta.xml"));


        int bytes = meta.read();
        out.putNextEntry(new ZipEntry(fileAndDirectory + "-meta.xml"));
        while (bytes != -1) {
            out.write(bytes);
            bytes = meta.read();
        }
        out.closeEntry();

        out.putNextEntry(new ZipEntry(fileAndDirectory));
        out.write(code.getBytes());
        out.closeEntry();
        out.close();
    }

    private String determineTypeName(String filename) {
        String name = filename.substring(filename.lastIndexOf(".") + 1);
//        System.out.println("TYPE=" + name);
        String niceCase = name.substring(0, 1).toUpperCase() +
                name.substring(1);
        // System.out.println("LETTER=" + letter   );
        //String nicename = (name.substring(1,1).toUpperCase()) + name.substring(2);
        return "Apex" + niceCase;
    }

    private String determineDirectory(String filename) {
        File parent = new File(filename).getParentFile();
        File child = new File(parent.getName(), new File(filename).getName());

        return child.getPath();
    }


    private void compileAndUploadCode(String filename, Properties prop, String src, String debugFile, StringBuilder sourceCode) throws ConnectionException, IOException {
        String[] triggers = new String[0];
        String[] classes = new String[0];

        if (filename.endsWith(".trigger")) {
            triggers = new String[1];
            triggers[0] = sourceCode.toString();

        } else if (filename.endsWith(".cls")) {
            classes = new String[1];
            classes[0] = sourceCode.toString();
        }

        CompileAndTestRequest request;

        request = new CompileAndTestRequest();
        request.setClasses(classes);
        request.setTriggers(triggers);
        request.setCheckOnly(false);

        SoapConnection connection = loginHelper.getApexConnection();

        String name = new File(filename).getName();

        if (name.contains("Test")) {
            RunTestsRequest rtr = new RunTestsRequest();
            rtr.setClasses(new String[]{name.replace(".cls", "")});
            request.setRunTestsRequest(rtr);
        }

        // Set logging levels (doesn't work!)
        List<LogInfo> logInfos = new ArrayList<LogInfo>();

        for (Object key : prop.keySet()) {
            String keyName = (String) key;
            if (keyName.startsWith("log.category.")) {
                String cat = keyName.substring("log.category.".length());
                String level = prop.getProperty(keyName);

//                System.out.println("cat=" + cat + "=" + level);
                LogInfo li = new LogInfo();
                li.setCategory(LogCategory.valueOf(cat));
                li.setLevel(LogCategoryLevel.valueOf(level));
                logInfos.add(li);
            }
        }


        LogInfo[] lis = new LogInfo[logInfos.size()];
        logInfos.toArray(lis);

        connection.setDebuggingHeader(lis, LogType.valueOf("None"));

        CompileAndTestResult compileTestResult = connection.compileAndTest(request);

        FileWriter fw = new FileWriter(new File(debugFile));
        fw.write(connection.getDebuggingInfo().getDebugLog());
        fw.close();

        if (!compileTestResult.isSuccess()) {
            CompileClassResult[] cr = compileTestResult.getClasses();
            for (CompileClassResult compileClassResult : cr) {
                String failname = findFile(src, compileClassResult.getName(), filename);
                if (compileClassResult.getProblem() != null) {

                    err(failname, compileClassResult.getLine(),
                            compileClassResult.getColumn(),
                            "E",
                            compileClassResult.getProblem());
                }

                String[] warnings = compileClassResult.getWarnings();
                for (String warning : warnings) {
                    err(failname, compileClassResult.getLine(),
                            compileClassResult.getColumn(),
                            "W",
                            warning);
                }
            }
        }
        // \\s.*
        Pattern linePat = Pattern.compile(".* line ([0-9]*).*column ([0-9]*).*");
        Pattern namePat = Pattern.compile("^\\w.*\\.(\\w.*)\\..*");


        RunTestsResult testResult = compileTestResult.getRunTestsResult();


        if (testResult.getNumTestsRun() > 0) {


            RunTestFailure[] fails = testResult.getFailures();

            if (fails.length == 0) {
                System.out.println("TESTS PASS!");
            }

            for (RunTestFailure fail : fails) {

//                System.out.println(fail.toString());
//                System.out.println(">>>> " + fail.getStackTrace() + "<<<<");

                LineNumberReader rr = new LineNumberReader(new StringReader(fail.getStackTrace()));
                String stack = rr.readLine();
                while (stack != null) {
//                    System.out.println("STACKx : " + stack);
                    Matcher nm = namePat.matcher(stack);
//                    System.out.println("NM " + nm.matches());

                    if (nm.matches()) {
//                        System.out.println("FILE: " + nm.group(1));

                        Matcher m = linePat.matcher(stack);

                        String failname = findFile(src, nm.group(1), filename);

                        int line = -1;
                        int column = -1;
                        if (m.matches() && m.groupCount() == 2) {
                            try {
                                line = Integer.decode(m.group(1));
                                column = Integer.decode(m.group(2));
                            } catch (NumberFormatException e) {
                                err(failname, -1, -1, "W", "Internal farce.ide error decoding: " + fail.getStackTrace());
                            }
                        }
                        err(failname, line, column, "E", fail.getMessage());


                    }
                    stack = rr.readLine();

                }
            }
        }

        if (compileTestResult.isSuccess()) {
            System.out.println("COMPILED OK!");
        }
    }


    private void downloadFile(String srcDir, String filename, File crcFile) throws Exception {
        Properties crcs = new Properties();
        if (crcFile.exists()) {
            crcs.load(new FileReader(crcFile));
        }

        System.out.println("DOWNLOAD FILE IS " + filename);
        String metadataType = detemineApexType(filename);
//        String metadataType = determineTypeName(filename);

        String filenameNoPath = new File(filename).getName();
        filenameNoPath = getNoSuffix(filenameNoPath);

        List<String> files = new ArrayList<String>();
        files.add(filenameNoPath);

        Package p = createPackage(metadataType, files);
        RetrieveRequest retrieveRequest = prepareRequest(true, null, p);

        File result = retrieveZip(retrieveRequest);
        // Find our file and rewrite the local one
        unzipFile(srcDir, result);

        updateCrcs(crcs, result);
        crcs.store(new FileWriter(crcFile), "Generated file");

    }


    public void downloadFiles(String srcDir, File crcFile) throws Exception {
        Properties crcs = new Properties();

        String[] metadataTypes = new String[]{
                "ApexClass",
                "ApexComponent",
                "ApexPage",
                "ApexTrigger"};

        for (String metadataType : metadataTypes) {
            List<String> files = new ArrayList<String>();
            files.add("*");
            Package p = createPackage(metadataType, files);
            RetrieveRequest retrieveRequest = prepareRequest(true, null, p);

            File result = retrieveZip(retrieveRequest);
            updateCrcs(crcs, result);
            unzipFile(srcDir, result);
        }

        crcs.store(new FileWriter(crcFile), "Generated file");

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


    protected RetrieveRequest prepareRequest(boolean isSinglePackage, String packageNames[], Package componentManifest) {
        RetrieveRequest retrieveRequest = new RetrieveRequest();
        retrieveRequest.setApiVersion(apiversion);
        retrieveRequest.setSinglePackage(isSinglePackage);
        if (packageNames != null && packageNames.length > 0)
            retrieveRequest.setPackageNames(packageNames);
        if (componentManifest != null) {
            retrieveRequest.setUnpackaged(componentManifest);
        }
        return retrieveRequest;
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


    private List<String> generateItemList(String metadataType, String folderName) throws Exception {
        List<String> items = new ArrayList<String>();

        ListMetadataQuery query = new ListMetadataQuery();
        query.setFolder(folderName);
        query.setType(metadataType);
        MetadataConnection metadataStub = loginHelper.getMetadataConnection();
        FileProperties properties[] = metadataStub.listMetadata(new ListMetadataQuery[]{
                query
        }, apiversion);
        if (properties != null && properties.length > 0) {
            int len$ = properties.length;
            for (int i$ = 0; i$ < len$; i$++) {
                FileProperties prop = properties[i$];
                items.add(prop.getFullName());
            }

        }
        return items;
    }


    private void err(String filename, int line, int col, String ew, String msg) {
        String err = filename + ">" + line + ":" + col + ":" + ew + ":0:" + msg;
        System.out.println(err);
    }

    private String findFile(String src, final String searchname, String compileFile) {
        System.out.println("SRC=" + src);
        File[] files = findDiskFile(src, "classes", searchname);

        if (files.length == 0) {
            files = findDiskFile(src, "triggers", searchname);
        }

        if (files.length == 0) {
            // System.out.println("FAILED TO FIND FILE FOR " + searchname);
            return compileFile;
        }
        return files[0].getAbsolutePath();
    }

    private File[] findDiskFile(String src, String subdir, final String searchname) {
        File root = new File(src, subdir);


        return root.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.startsWith(searchname + "."));
            }
        });
    }


    private File retrieveZip(RetrieveRequest retrieveRequest) throws RemoteException, Exception {
//        RetrieveRequest retrieveRequest = new RetrieveRequest();
//        retrieveRequest.setApiVersion(WSDL_VERSION);
//        setUnpackaged(retrieveRequest);

        File resultsFile = File.createTempFile("SFDC", "DOWN");
//        System.out.println("RETREIVED TO " + resultsFile.getAbsolutePath());

        MetadataConnection metadatabinding = loginHelper.getMetadataConnection();

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
            System.out.println("Retrieve warnings:\n" + buf);
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
        }
        finally {
            os.close();
        }

        return resultsFile;
    }


    private void unzipFile(String srcDir, File zipFile) throws Exception {
        BufferedOutputStream out = null;
        ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        ZipEntry entry;
        while ((entry = in.getNextEntry()) != null) {
            int count;
            byte data[] = new byte[1000];
            out = new BufferedOutputStream(new
                    FileOutputStream(new File(srcDir, entry.getName())), 1000);
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


    private void doDiff() throws Exception {

        MetadataConnection metadataConnection = loginHelper.getMetadataConnection();
        PartnerConnection partnerConnection = loginHelper.getPartnerConnection();

        /*
        DescribeSObjectResult[] resu = partnerConnection.describeSObjects(new String[]{"Account"});
        for (DescribeSObjectResult describeSObjectResult : resu) {
            Field[] fields = describeSObjectResult.getFields();
            for (Field field : fields) {
                System.out.println("FIELD: " + field.getName());
            }
        }
        */


        ListMetadataQuery query = new ListMetadataQuery();
        query.setType("CustomObject");

        ListMetadataQuery[] queries = new ListMetadataQuery[]{query};
        FileProperties[] proprs = metadataConnection.listMetadata(queries, 20.0);


        for (FileProperties propr : proprs) {
            //    System.out.println("---------------------");
            //    System.out.println(propr.getFullName());
            //    System.out.println("---------------------");

            try {
                String[] oname = new String[]{propr.getFullName()};
                DescribeSObjectResult[] resu = partnerConnection.describeSObjects(oname);
                for (DescribeSObjectResult describeSObjectResult : resu) {

                    Field[] fields = describeSObjectResult.getFields();
                    for (Field field : fields) {
                        System.out.println(describeSObjectResult.getName() + " : " + field.getName());
                    }
                }
            } catch (Exception e) {
                System.out.println(propr.getFullName() + " BLEW UP WITH " + e.getMessage());

            }

            //http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_describesobjects.htm#topic-title
        }


    }


    private void runTests(String directory) throws Exception {
        File dir = new File(directory);
        String[] testClasses = dir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith("Test.cls") || (name.endsWith("Tests.cls")));
//                return (name.equals("AgencyAccountControllerTests.cls"));
            }
        });

        for (int i = 0; i < testClasses.length; i++) {
            testClasses[i] = testClasses[i].substring(0, testClasses[i].indexOf(".cls"));
            System.out.println(testClasses[i]);

        }

        SoapConnection connection = loginHelper.getApexConnection();

        RunTestsRequest request = new RunTestsRequest();
        request.setClasses(testClasses);

        RunTestsResult res = connection.runTests(request);


        System.out.println("Number of tests: " + res.getNumTestsRun());
        System.out.println("Number of failures: " + res.getNumFailures());
        if (res.getNumFailures() > 0) {
            for (RunTestFailure rtf : res.getFailures()) {
                System.out.println("Failure: " + (rtf.getNamespace() ==
                        null ? "" : rtf.getNamespace() + ".")
                        + rtf.getName() + "." + rtf.getMethodName() + ": "
                        + rtf.getMessage() + "\n" + rtf.getStackTrace());
            }
        }
        if (res.getCodeCoverage() != null) {
            for (CodeCoverageResult ccr : res.getCodeCoverage()) {
                Float not = Float.parseFloat("" + ccr.getNumLocationsNotCovered());
                Float total = Float.parseFloat("" + ccr.getNumLocations());

                Float percentage = ((total - not) / total) * 100;

                System.out.println("Code coverage for " + ccr.getType() + " " +
                        (ccr.getNamespace() == null ? "" : ccr.getNamespace() + ".")
                        + ccr.getName() + ": "
                        + ccr.getNumLocationsNotCovered()
                        + " locations not covered out of "
                        + ccr.getNumLocations() + " " + percentage + "%"

                );
//                if (ccr.getNumLocationsNotCovered() > 0) {
//                    for (CodeLocation cl : ccr.getLocationsNotCovered())
//                        System.out.println("\tLine " + cl.getLine() + ", column "
//                                + cl.getColumn());
//                }
                /*
                if (ccr.getSoqlInfo() != null) {
                    System.out.println(" SOQL profiling");
                    for (CodeLocation cl : ccr.getSoqlInfo())
                        System.out.println("\tLine " + cl.getLine() + ", column "
                                + cl.getColumn() + ": " + cl.getNumExecutions()
                                + " time(s) in " + cl.getTime() + " ms");
                }
                if (ccr.getDmlInfo() != null) {
                    System.out.println(" DML profiling");
                    for (CodeLocation cl : ccr.getDmlInfo())
                        System.out.println("\tLine " + cl.getLine() + ", column "
                                + cl.getColumn() + ": " + cl.getNumExecutions()
                                + " time(s) in " + cl.getTime() + " ms");
                }
                if (ccr.getMethodInfo() != null) {
                    System.out.println(" Method profiling");
                    for (CodeLocation cl : ccr.getMethodInfo())
                        System.out.println("\tLine " + cl.getLine() + ", column "
                                + cl.getColumn() + ": " + cl.getNumExecutions()
                                + " time(s) in " + cl.getTime() + " ms");
                }
                */
            }
        }
    }

}