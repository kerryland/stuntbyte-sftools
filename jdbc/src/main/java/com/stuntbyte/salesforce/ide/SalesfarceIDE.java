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
package com.stuntbyte.salesforce.ide;

import com.sforce.soap.apex.*;
import com.stuntbyte.salesforce.deployment.Deployer;
import com.stuntbyte.salesforce.deployment.DeploymentEventListener;
import com.stuntbyte.salesforce.misc.Downloader;
import com.stuntbyte.salesforce.misc.FileUtil;
import com.stuntbyte.salesforce.misc.LoginHelper;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.Package;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.ws.ConnectionException;

import java.io.*;
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


    private LoginHelper loginHelper;
    private Deployer deployer;
    private SimpleListener listener = new SimpleListener();
    private Reconnector reconnector;


    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage("");

        } else {

            String projectFile = args[0];

            if (!new File(projectFile).exists()) {
                System.err.println("Project file does not exist: " + projectFile) ;
                usage("");

            } else {
                String tagsFile = args[1];
                String command = args[2];
                String filename = null;
                if (args.length == 4) {
                    filename = args[3];
                }

                SalesfarceIDE ide = new SalesfarceIDE();

                ide.doIt(projectFile, tagsFile, filename, command);
            }
        }
    }

    private static void usage(String arg) {
        String msg = "Unknown argument [" + arg + "]. Supported arguments:\n" +
                        "<project-file> <tag-file> -compile  <filename> == upload and compile/test file\n" +
                        "                          -force    <filename> == upload and compile/test file, regardless of crc failure\n" +
                        "                          -downloadall         == download 'everything'\n" +
                        "                          -download <filename> == download just this file\n" +
                        "                          -delete <filename>   == delete this file\n" +
                        "                          -runtests            == run all tests for downloaded code\n" +
                        "                          -tag                 == regenerate tags";
        System.err.println(msg);
    }


    private Properties loadConfig(String projectFile) throws Exception {
        Properties prop = new Properties();
        prop.load(new FileReader(projectFile));
        String server = prop.getProperty("sf.serverurl");
        String username = prop.getProperty("sf.username");
        String password = prop.getProperty("sf.password");

        loginHelper = new LoginHelper(server, username, password);

        reconnector = new Reconnector(loginHelper);
        deployer = new Deployer(reconnector);

        return prop;
    }


    private void doIt(String projectFile, String tagsFile, String filename, String command) throws Exception {
        System.out.println("Executing " + command);
        Properties prop = loadConfig(projectFile);
        String projectDir = new File(projectFile).getParent();
        String srcDirectory = determineFilename(prop, projectDir, "src.dir", "force");
        File srcDirFile = new File(srcDirectory);
        srcDirFile.mkdirs();

        String debugFile = determineFilename(prop, srcDirectory, "debug.file", "debug.log");

        String crcFileName = prop.getProperty("crc.file");
        if (crcFileName == null || crcFileName.trim().length() == 0) {
            crcFileName = determineFilename(prop, srcDirectory, "crc.file", "crcFile");
        }
        String ctags = prop.getProperty("ctags");

        File crcFile = new File(crcFileName);
        if (!crcFile.exists()) {
            crcFile.createNewFile();
        }

//        File messageLogFile = File.createTempFile("vim", "log");
//        messageLogFile.deleteOnExit();

//        messageLog = new PrintWriter(messageLogFile);

//        System.out.println(command + " with tagfile " + tagsFile);

        try {
            if (command.equals("-download")) {
                downloadFile(srcDirectory, filename, crcFile);

            } else if (command.equals("-downloadall")) {
                downloadFiles(srcDirectory, crcFile);

            } else  if (command.equals("-runtests")) {
                runTests(srcDirectory + "/classes");

            } else if (command.equals("-tag")) {
                generateTags(srcDirectory, ctags, tagsFile);

            } else if (command.equals("-compile") || command.equals("-force")) {
                String filenameNoPath = new File(filename).getName();

                doCompile(filename, command, prop, srcDirectory, debugFile, crcFile, filenameNoPath);

            } else {
                usage(command);
            }

        } finally {
//            messageLog.close();
        }
    }

    private String determineFilename(Properties prop, String srcDirectory, String propertyName, String defaultFilename) throws IOException {
        String debugFile = prop.getProperty(propertyName);
        if (debugFile == null || debugFile.trim().length() == 0) {
            File debugAsFile = new File(srcDirectory, defaultFilename);
            debugFile = debugAsFile.getAbsolutePath();
        }
        return debugFile;
    }

    private void doCompile(String filename, String arg, Properties prop, String srcDirectory, String debugFile, File crcFile, String filenameNoPath) throws Exception {
        if (filename.equals("")) {
            throw new NullPointerException("Filename is undefined. arg is " + arg);
        }
        // Download latest for checksum
        PackageTypeMembers mems = new PackageTypeMembers();
        String typeName = determineApexType(filename);

        mems.setName(typeName);
        String noSuffix = getNoSuffix(filename);
        mems.setMembers(new String[]{noSuffix});

        Package p = new Package();
        p.setTypes(new PackageTypeMembers[]{mems});
        RetrieveRequest retrieveRequest = prepareRequest(true, null, p);

        Properties crcs = new Properties();
        crcs.load(new FileReader(crcFile));

//        System.out.println("DOWNLOADING " + typeName + " - " + mems.getMembers()[0]);
        CrcResults crcResults = recalcCrcs(filenameNoPath, retrieveRequest, crcs);


        boolean runTests = true;
        boolean uploadCode = true;

        if ((crcResults.serverCrc != null) && (crcResults.localCrc != null) &&
                (!crcResults.serverCrc.equals(crcResults.localCrc))) {
            if (arg.equals("-force")) {
                output("Saving even though checksums mismatch");
                runTests = false;

            } else {
                err(filename, -1, -1, "E", "Code NOT saved due to checksum issue. Use -download to get latest or -force to force upload");
                uploadCode = false;
            }
        }

        if (uploadCode) {
            uploadCode(filename, prop, srcDirectory, debugFile, filenameNoPath, crcs, retrieveRequest, runTests);
            crcs.store(new FileWriter(crcFile), "Automatically generated for " + crcResults.crcKey);
        }
    }

    private CrcResults recalcCrcs(String filenameNoPath, RetrieveRequest retrieveRequest, Properties crcs) throws Exception {
        // TODO: Use downloader.retrieveZip instead
        File result = deployer.retrieveZip(retrieveRequest, listener);
        ZipFile zip = new ZipFile(result);
        CrcResults crcResults = pullCrcs(zip, crcs, filenameNoPath);
        zip.close();
        FileUtil.delete(result);
        return crcResults;
    }

    private void generateTags(String srcDirectory, String ctags, String tagsFile) throws Exception {
        Runtime r = Runtime.getRuntime();
        String cmd = ctags + " -f " + tagsFile + " " + srcDirectory ;
        Process p = r.exec(cmd);
        p.waitFor();
        BufferedReader b = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String line = "";

        while ((line = b.readLine()) != null) {
            System.out.println(line);
        }
    }

    private void uploadCode(String filename, Properties prop, String srcDirectory, String debugFile,
                            String filenameNoPath,
                            Properties crcs, RetrieveRequest retrieveRequest, boolean runTests) throws Exception {
        File result;
        ZipFile zip;
        CrcResults crcResults;
        String sourceCode = FileUtil.loadTextFile(new File(filename));

        File metaFile = new File(filename + "-meta.xml");
        if (!metaFile.exists()) {
            err(filename, -1, -1, "E", "Code NOT saved due to missing -meta.xml");
            return;
        }

        String metaData = FileUtil.loadTextFile(metaFile);

        if (filename.endsWith(".trigger") || filename.endsWith(".cls")) {
            compileAndUploadCode(filename, prop, srcDirectory, debugFile, sourceCode, runTests);
        } else {

            String partFilename = new File(filename).getName();
            String aTypeName = determineTypeName(partFilename);

            deployer.uploadNonCode(aTypeName, filename, sourceCode.toString(), metaData, listener);
        }

        // Get latest CRCs
        // TODO: Use downloader.retrieveZip instead

        crcResults = recalcCrcs(filenameNoPath, retrieveRequest, crcs);

//        result = deployer.retrieveZip(retrieveRequest, listener);
//        zip = new ZipFile(result);
//        crcResults = pullCrcs(zip, crcs, filenameNoPath);
//        zip.close();
//        FileUtil.delete(result);

        // Store checksum locally
        if (crcResults.serverCrc == null) { // ie: File doesn't exist on server
            crcResults.serverCrc = -999l;
            crcResults.crcKey = filenameNoPath;
        }
        crcs.setProperty(crcResults.crcKey, String.valueOf(crcResults.serverCrc));
    }


    public String determineApexType(String filename) {
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
                String crc = crcs.getProperty(result.crcKey);
                if (crc == null) {
                    crc = "-999";
                }
                result.localCrc = Long.valueOf(crc);

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


    private void compileAndUploadCode(String filename, Properties prop, String src, String debugFile,
                                      String sourceCode, boolean runTests) throws ConnectionException, IOException {
        String[] triggers = new String[0];
        String[] classes = new String[0];

        if (filename.endsWith(".trigger")) {
            triggers = new String[1];
            triggers[0] = sourceCode;

        } else if (filename.endsWith(".cls")) {
            classes = new String[1];
            classes[0] = sourceCode;
        }

        CompileAndTestRequest request;

        request = new CompileAndTestRequest();
        request.setClasses(classes);
        request.setTriggers(triggers);
        request.setCheckOnly(false);

        SoapConnection connection = reconnector.getApexConnection();

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


//        connection.setDebuggingHeader(lis, LogType.valueOf("None"));
        connection.setDebuggingHeader(lis, LogType.Detail);

        CompileAndTestResult compileTestResult = connection.compileAndTest(request);

        if (name.contains("Test") && runTests) {
            FileWriter fw = new FileWriter(new File(debugFile));
            fw.write(connection.getDebuggingInfo().getDebugLog());
            fw.close();
        }

        if (!compileTestResult.isSuccess()) {

            CompileClassResult[] cr = compileTestResult.getClasses();
            for (CompileClassResult compileClassResult : cr) {
                String failname = findSourceFile(src, compileClassResult.getName(), filename);
                if (compileClassResult.getProblem() != null) {

                    err(failname, compileClassResult.getLine(),
                            compileClassResult.getColumn(),
                            "E",
                            compileClassResult.getProblem().replaceAll("\n", " "));
                }

                CompileIssue[] warnings = compileClassResult.getWarnings();
                for (CompileIssue warning : warnings) {
                    err(failname, compileClassResult.getLine(),
                            compileClassResult.getColumn(),
                            "W",
                            warning.getMessage().replaceAll("\n", " "));
                }
            }

            CompileTriggerResult[] tr = compileTestResult.getTriggers();
            for (CompileTriggerResult compileClassResult : tr) {
                String failname = findSourceFile(src, compileClassResult.getName(), filename);
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
                output("TESTS PASS!");
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

                        String failname = findSourceFile(src, nm.group(1), filename);
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

        String metadataType = determineApexType(filename);

        String filenameNoPath = new File(filename).getName();
        filenameNoPath = getNoSuffix(filenameNoPath);

        Downloader downloader = new Downloader(reconnector, new File(srcDir), listener, crcFile);
        downloader.addPackage(metadataType, filenameNoPath);
        downloader.download();
    }


    public void downloadFiles(String srcDir, File crcFile) throws Exception {
        crcFile.createNewFile();

        String[] metadataTypes = new String[]{
                "ApexClass",
                "ApexComponent",
                "ApexPage",
                "ApexTrigger"};

        Downloader downloader = new Downloader(reconnector, new File(srcDir), listener, crcFile);

        for (String metadataType : metadataTypes) {
            downloader.addPackage(metadataType, "*");
        }

        downloader.download();
    }


    private RetrieveRequest prepareRequest(boolean isSinglePackage, String packageNames[], Package componentManifest) {
        RetrieveRequest retrieveRequest = new RetrieveRequest();
        retrieveRequest.setApiVersion(loginHelper.getSfVersion());
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

                                                       /*
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
    */


    private void err(String filename, int line, int col, String ew, String msg) {
        String err = filename + ">" + line + ":" + col + ":" + ew + ":0:" + msg;
        output(err);
    }

    private String findSourceFile(String src, final String searchname, String compileFile) {
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

              /*
    private void doDiff() throws Exception {

        MetadataConnection metadataConnection = loginHelper.getMetadataConnection();
        PartnerConnection partnerConnection = loginHelper.getPartnerConnection();

//        DescribeSObjectResult[] resu = partnerConnection.describeSObjects(new String[]{"Account"});
//        for (DescribeSObjectResult describeSObjectResult : resu) {
//            Field[] fields = describeSObjectResult.getFields();
//            for (Field field : fields) {
//                System.out.println("FIELD: " + field.getName());
//            }
//        }

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

*/

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
//            System.out.println(testClasses[i]);

        }

        SoapConnection connection = reconnector.getApexConnection();

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
            int grandTotal = 0;
            int grandNot = 0;
            for (CodeCoverageResult ccr : res.getCodeCoverage()) {
                int not = ccr.getNumLocationsNotCovered();
                int total = ccr.getNumLocations();

                grandTotal += total;
                grandNot += not;

                Float percentage = (((float)total - (float)not) / (float)total) * 100;

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
            Float percentage = (((float)grandTotal - (float)grandNot) / (float)grandTotal) * 100;

            System.out.println("Code coverage totals: " +
                        + grandNot
                        + " locations not covered out of "
                        + grandTotal + " " + percentage + "%"

                );

        }
    }

    private void output(String val) {
        System.out.println(val);
//        messageLog.println(val);
    }

    private class SimpleListener implements DeploymentEventListener {

        public void error(String message) {
            output(message);
        }

        public void message(String message) {
            output(message);
        }

        public void setAsyncResult(AsyncResult asyncResult) {

        }

        public void progress(String message) {
            output(message);
        }
    }
}