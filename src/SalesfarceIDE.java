import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.misc.Downloader;
import com.fidelma.salesforce.misc.LoginHelper;
import com.sforce.soap.apex.CodeCoverageResult;
import com.sforce.soap.apex.CompileAndTestRequest;
import com.sforce.soap.apex.CompileAndTestResult;
import com.sforce.soap.apex.CompileClassResult;
import com.sforce.soap.apex.CompileTriggerResult;
import com.sforce.soap.apex.LogCategory;
import com.sforce.soap.apex.LogCategoryLevel;
import com.sforce.soap.apex.LogInfo;
import com.sforce.soap.apex.LogType;
import com.sforce.soap.apex.RunTestFailure;
import com.sforce.soap.apex.RunTestsRequest;
import com.sforce.soap.apex.RunTestsResult;
import com.sforce.soap.apex.SoapConnection;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.Package;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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


    private double apiversion = 20d;

    private LoginHelper loginHelper;
    private Deployer deployer;
    private SimpleListener listener = new SimpleListener();
    private MetadataConnection metaDataConnection;

//    private PrintWriter messageLog;


    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }

        String argument = "";
        String filename = args[0];
        if (args[0].startsWith("-")) {
            argument = args[0];
            if (args.length > 1) {
                filename = args[1];
            }
        }
        SalesfarceIDE ide = new SalesfarceIDE();

        ide.doIt(filename, argument);
    }

    private static void usage() {
        String msg =
                "ARGS:\n" +
                "\n" +
                "    <filename>           == upload and compile/test file\n" +
                "    -downloadall         == download 'everything'\n" +
                "    -download <filename> == download just this file\n" +
                "    -force    <filename> == force upload, regardless of crc";
        System.err.println(msg);
    }


    private Properties loadConfig() throws Exception {
        Properties prop = new Properties();
        prop.load(new FileReader("build.properties"));
        prop.load(new FileReader("local.build.properties"));
        String server = prop.getProperty("sf.serverurl");
        String username = prop.getProperty("sf.username");
        String password = prop.getProperty("sf.password");

        loginHelper = new LoginHelper(server, username, password);
        metaDataConnection = loginHelper.getMetadataConnection();
        deployer = new Deployer(metaDataConnection);

        return prop;
    }

    private void doIt(String filename, String arg) throws Exception {
        Properties prop = loadConfig();
        String srcDirectory = prop.getProperty("src.dir");
        String debugFile = prop.getProperty("debug.file");
        String crcFile = prop.getProperty("crc.file");

        String filenameNoPath = new File(filename).getName();
//        File messageLogFile = File.createTempFile("vim", "log");
//        messageLogFile.deleteOnExit();

//        messageLog = new PrintWriter(messageLogFile);

        try {
            if (arg.equals("-download")) {
                downloadFile(srcDirectory, filename, new File(crcFile));
                return;
            }
            if (arg.equals("-downloadall")) {
                downloadFiles(srcDirectory, new File(crcFile));
                return;
            }

            if (arg.equals("-diff")) {
                doDiff();
                return;
            }

            if (arg.equals("-runtests")) {
                runTests(srcDirectory + "/classes");
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

            File result = deployer.retrieveZip(retrieveRequest, listener);
            ZipFile zip = new ZipFile(result);
            CrcResults crcResults = pullCrcs(zip, crcs, filenameNoPath);

            boolean runTests = true;

            if ((crcResults.serverCrc != null) && (crcResults.localCrc != null) && (!crcResults.serverCrc.equals(crcResults.localCrc))) {
                if (arg.equals("-force")) {
                    message("Saving even though checksums mismatch");
                    runTests = false;

                } else {
                    err(filename, -1, -1, "E", "Code NOT saved due to checksum issue. Use -download to get latest or -force to force upload");
                    return;
                }
            }

            if (filename.endsWith(".trigger") || filename.endsWith(".cls")) {
                compileAndUploadCode(filename, prop, srcDirectory, debugFile, sourceCode, runTests);
            } else {

                String partFilename = new File(filename).getName();
                String aTypeName = determineTypeName(partFilename);

                deployer.uploadNonCode(aTypeName, filename, srcDirectory, sourceCode.toString(), listener);
            }

            // Get latest CRCs
            result = deployer.retrieveZip(retrieveRequest, listener);
            zip = new ZipFile(result);
            crcResults = pullCrcs(zip, crcs, filenameNoPath);

            // Store checksum locally
            crcs.setProperty(crcResults.crcKey, String.valueOf(crcResults.serverCrc));
            crcs.store(new FileWriter(crcFile), "Automatically generated for " + crcResults.crcKey);
//        System.out.println("STORED CRC OF " + crcResults.serverCrc);
        } finally {
//            messageLog.close();
        }
    }


    public String detemineApexType(String filename) {
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


    private String determineTypeName(String filename) {
        String name = filename.substring(filename.lastIndexOf(".") + 1);
        String niceCase = name.substring(0, 1).toUpperCase() +
                name.substring(1);
        return "Apex" + niceCase;
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



//    private void uploadNonCode(String filename, String srcDirectory, String code) throws Exception {
//        File deploymentFile = File.createTempFile("SFDC", "zip");
//        createDeploymentFile(filename, srcDirectory, code, deploymentFile);
//        deployZip(deploymentFile);
//    }



//    private void createDeploymentFile(String filename, String srcDirectory, String apexCode, File deploymentFile) throws IOException {
//        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(deploymentFile));
//
//        String fileAndDirectory = determineDirectory(filename).replace("\\", "/");
//
//        filename = new File(filename).getName();
//        String nonSuffixedFilename = filename.substring(0, filename.lastIndexOf("."));
//
//        String typeName = determineTypeName(filename);
//
//        String packageXml =
//                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//                        "<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n" +
//                        "    <types>\n" +
//                        "        <members>" + nonSuffixedFilename + "</members>\n" +
//                        "        <name>" + typeName + "</name>\n" +
//                        "    </types>\n" +
//                        "    <version>" + WSDL_VERSION + "</version>\n" +
//                        "</Package>";
//
//        out.putNextEntry(new ZipEntry("package.xml"));
//        out.write(packageXml.getBytes());
//        out.closeEntry();
//
//        FileInputStream meta = new FileInputStream(new File(srcDirectory, fileAndDirectory + "-meta.xml"));
//
//        int bytes = meta.read();
//        out.putNextEntry(new ZipEntry(fileAndDirectory + "-meta.xml"));
//        while (bytes != -1) {
//            out.write(bytes);
//            bytes = meta.read();
//        }
//        out.closeEntry();
//
//        out.putNextEntry(new ZipEntry(fileAndDirectory));
//        out.write(apexCode.getBytes());
//        out.closeEntry();
//        out.close();
//    }



    private void compileAndUploadCode(String filename, Properties prop, String src, String debugFile, StringBuilder sourceCode, boolean runTests) throws ConnectionException, IOException {
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

        if (name.contains("Test") && runTests) {
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

        if (name.contains("Test") && runTests) {
            FileWriter fw = new FileWriter(new File(debugFile));
            fw.write(connection.getDebuggingInfo().getDebugLog());
            fw.close();
        }

        if (!compileTestResult.isSuccess()) {

            CompileClassResult[] cr = compileTestResult.getClasses();
            for (CompileClassResult compileClassResult : cr) {
                String failname = findFile(src, compileClassResult.getName(), filename);
                if (compileClassResult.getProblem() != null) {

                    err(failname, compileClassResult.getLine(),
                            compileClassResult.getColumn(),
                            "E",
                            compileClassResult.getProblem().replaceAll("\n", " "));
                }

                String[] warnings = compileClassResult.getWarnings();
                for (String warning : warnings) {
                    err(failname, compileClassResult.getLine(),
                            compileClassResult.getColumn(),
                            "W",
                            warning.replaceAll("\n", " "));
                }
            }

            CompileTriggerResult[] tr = compileTestResult.getTriggers();
            for (CompileTriggerResult compileClassResult : tr) {
                String failname = findFile(src, compileClassResult.getName(), filename);
                if (compileClassResult.getProblem() != null) {

                    err(failname, compileClassResult.getLine(),
                            compileClassResult.getColumn(),
                            "E",
                            compileClassResult.getProblem());
                }
            }
        }
        Pattern linePat = Pattern.compile(".* line ([0-9]*).*column ([0-9]*).*");
        Pattern namePat = Pattern.compile("^\\w.*\\.(\\w.*)\\.(\\w.*)\\:.*");

        RunTestsResult testResult = compileTestResult.getRunTestsResult();


        if (testResult.getNumTestsRun() > 0) {
            RunTestFailure[] fails = testResult.getFailures();

            if (fails.length == 0) {
                message("TESTS PASS!");
            }

            Pattern trigPat = Pattern.compile("Trigger.(\\w.*)..*");

            for (RunTestFailure fail : fails) {
                LineNumberReader rr = new LineNumberReader(new StringReader(fail.getMessage() + "\n" + fail.getStackTrace()));
                String stack = rr.readLine();
                while (stack != null) {

                    Matcher nm = trigPat.matcher(stack);
                    if (!nm.matches()) {
                        nm = namePat.matcher(stack);
                    }

                    if (nm.matches()) {
                        Matcher m = linePat.matcher(stack);

                        String failname = findFile(src, nm.group(1), filename);
                        String method = "";
                        if (nm.groupCount() > 1) {
                            method = nm.group(2);
                        }

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
//                        err(failname, line, column, "E", fail.getMessage().replaceAll("\n", "|"));
                        err(failname, line, column, "E", "[" + method + "] " + fail.getMessage());
                    }
                    stack = rr.readLine();
                }
            }
        }

        if (compileTestResult.isSuccess()) {
//            message("COMPILED OK!");
        }
    }


    private void downloadFile(String srcDir, String filename, File crcFile) throws Exception {
        Properties crcs = new Properties();
        if (crcFile.exists()) {
            crcs.load(new FileReader(crcFile));
        }

        String metadataType = detemineApexType(filename);

        String filenameNoPath = new File(filename).getName();
        filenameNoPath = getNoSuffix(filenameNoPath);

        Downloader downloader = new Downloader(metaDataConnection, new File(srcDir), listener, crcFile);
        downloader.addPackage(metadataType, filenameNoPath);
        downloader.download();
/*

        // TODOL: Fix ugly code duplication here and in downloadFiles
        List<String> files = new ArrayList<String>();
        files.add(filenameNoPath);

        Package p = createPackage(metadataType, files);
        RetrieveRequest retrieveRequest = prepareRequest(true, null, p);

        File result = deployer.retrieveZip(retrieveRequest, listener);
        // Find our file and rewrite the local one
        deployer.unzipFile(srcDir, result);

        updateCrcs(crcs, result);
        crcs.store(new FileWriter(crcFile), "Generated file");
        */

    }


    public void downloadFiles(String srcDir, File crcFile) throws Exception {
        crcFile.createNewFile();
//        Properties crcs = new Properties();

        String[] metadataTypes = new String[]{
                "ApexClass",
                "ApexComponent",
                "ApexPage",
                "ApexTrigger"};

        Downloader downloader = new Downloader(metaDataConnection, new File(srcDir), listener, crcFile);

        for (String metadataType : metadataTypes) {
            downloader.addPackage(metadataType, "*");
            downloader.download();

//            List<String> files = new ArrayList<String>();
//            files.add("*");
//            Package p = createPackage(metadataType, files);
//            RetrieveRequest retrieveRequest = prepareRequest(true, null, p);
//
//            File result = deployer.retrieveZip(retrieveRequest, listener);
//            updateCrcs(crcs, result);
//            deployer.unzipFile(srcDir, result);
        }

//        crcs.store(new FileWriter(crcFile), "Generated file");

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


    private RetrieveRequest prepareRequest(boolean isSinglePackage, String packageNames[], Package componentManifest) {
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
        message(err);
    }

    private String findFile(String src, final String searchname, String compileFile) {
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

    private void message(String val) {
        System.out.println(val);
//        messageLog.println(val);
    }

    private class SimpleListener implements DeploymentEventListener {

        public void heyListen(String message) {
            message(message);
        }

        public void error(String message) {
            message(message);
        }

        public void finished(String message) {
            message(message);
        }
    }
}