package com.stuntbyte.salesforce.jdbc.ddl;

import com.stuntbyte.salesforce.deployment.Deployer;
import com.stuntbyte.salesforce.deployment.Deployment;
import com.stuntbyte.salesforce.deployment.DeploymentEventListener;
import com.stuntbyte.salesforce.misc.Downloader;
import com.stuntbyte.salesforce.misc.FileUtil;
import com.stuntbyte.salesforce.misc.FolderZipper;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.stuntbyte.salesforce.parse.SimpleParser;
import com.sforce.soap.metadata.AsyncResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Auto deployment creator?
 * <p/>
 * DEPLOYMENT wibble CREATE;
 * DEPLOYMENT wibble ADD classes  MagicController
 * DEPLOYMENT wibble ADD classes  MagicControllerTest
 * DEPLOYMENT wibble ADD pages    Magic
 * DEPLOYMENT wibble ADD objects  MagicStore__c
 * DEPLOYMENT wibble ADD objects  MagicStore__c Fields This__c, That__c
 * DEPLOYMENT wibble ADD objects  MagicStore__c WebLinks This__c, That__c
 * DEPLOYMENT wibble ADD objects  MagicStore__c ActionOverrides Edit
 * DEPLOYMENT wibble ADD objects  MagicStore__c ValidationRules DateCheck
 * <p/>
 * DEPLOYMENT wibble STRIP PACKAGE FinancialForce
 * DEPLOYMENT wibble FORCE VERSION 22.0
 * <p/>
 * <p/>
 * <p/>
 * DEPLOYMENT wibble PACKAGE TO "/tmp/package.zip"
 * <p/>
 * DEPLOYMENT UPLOAD PACKAGE FROM "/tmp/package.zip" OUTPUT TO "/tmp/out.txt" [ RUNTESTS | IGNORE_ERRORS | STATUS "<id>" ]
 */
public class DeployCommand {

    static DeployCommand singleton = new DeployCommand();

    private SimpleParser al;
    private Reconnector reconnector;

    // There is only one active at a time...
    private Deployment deployment;
    private File snapshotDirectory;
    private Map<String, Map<String, String>> envVars = new HashMap<String, Map<String, String>>();
    private List<RuleDefinition> ruleDefinitions = new ArrayList<RuleDefinition>();
    private boolean patchApplyDefined = false;
    private Map<String, String> sourceVars;
    private Map<String, String> destVars;

    private DeployCommand() {

    }

//    public DeployCommand(SimpleParser al, Reconnector reconnector) throws Exception {
//        this.al = al;
//        this.reconnector = reconnector;
//    }


    public static void execute(SimpleParser al, Reconnector reconnector) throws SQLException {
        singleton.al = al;
        singleton.reconnector = reconnector;
        singleton.execute();
    }

    public void execute() throws SQLException {
        if (!reconnector.getLicence().supportsDeploymentFeature()) {
            throw new SQLException("Your Stunt Byte licence does not allow use of the deployment tool.");
        }
        try {
            String value = al.getValue();
            if (value.equalsIgnoreCase("start")) {
                doStart();

            } else if (value.equalsIgnoreCase("upload")) {
                doUpload();

            } else if (deployment == null) {
                throw new SQLException("'deploy start' not yet called");

            } else if (value.equalsIgnoreCase("add")) {
                doAdd(false);

            } else if (value.equalsIgnoreCase("drop")) {
                doAdd(true);

            } else if (value.equalsIgnoreCase("commit")) {
                doCommit();

            } else if (value.equalsIgnoreCase("package")) {
                doPackage();

            } else if (value.equalsIgnoreCase("patch")) {
                doPatch();

            } else {
                throw new SQLException("Unexpected token: '" + value + "'");
            }
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private void doPatch() throws Exception {
        String command = al.getValue("Incomplete patch command").toLowerCase();

        if (command.equals("var")) {
            definePatchVariable();

        } else if (command.equals("rule")) {
            definePatchRule();

        } else if (command.equals("apply")) {
            definePatchApplication();

        } else {
            throw new SQLException("Unexpected token: '" + command + "'");
        }
    }

    private void definePatchApplication() throws Exception {
        if (patchApplyDefined) {
            throw new SQLException("only one 'apply' can be defined per deployment");
        }
        al.getToken("from");
        String sourceEnv = al.getValue("Missing source environment name").toLowerCase();
        sourceVars = getVariables("source environment", sourceEnv);

        al.getToken("to");
        String destEnv = al.getValue("Missing destination environment name").toLowerCase();
        destVars = getVariables("destination environment", destEnv);

        patchApplyDefined = true;
    }

    private Map<String, String> getVariables(String name, String sourceEnv) throws SQLException {
        Map<String, String> vars = envVars.get(sourceEnv);
        if (vars == null) {
            throw new SQLException(name + " '" + sourceEnv + "' has no defined variables");
        }
        return vars;
    }

    private void definePatchRule() throws Exception {
        al.getToken("replace");
        String variableName = al.getValue("Missing variable name");
        String[] split = variableName.split("\\.");
        if (split.length != 1) {
            throw new SQLException("Variable name in rule definition should not include environment name");
        }
        al.getToken("in");

        String type = getObjectType();
        String name = null;
        if (type.equals("*")) {
            if (al.getValue() != null) {
                throw new SQLException("No object name should be defined when using object type of *");
            }
        } else {
            name = getObjectName();
        }

        RuleDefinition rd = new RuleDefinition(variableName, type, name);
        ruleDefinitions.add(rd);
    }

    private class RuleDefinition {
        String variableName;
        String objectType;
        String objectName;
        private String directoryName;

        private RuleDefinition(String variableName, String objectType, String objectName) {
            this.variableName = variableName;
            this.objectType = objectType;
            this.objectName = objectName;
            if (!objectType.equals("*")) {
                directoryName = FileUtil.determineDirectoryName(objectType);
            }
        }

        public boolean appliesToDirectory(File directory) {
            return (objectType.equals("*") || directoryName.equals(directory.getName()));
        }
    }

    private void definePatchVariable() throws Exception {
        String variableName = al.getValue("Missing variable name");
        String[] split = variableName.split("\\.");
        if (split.length != 2) {
            throw new SQLException("Variable name does not include environment name");
        }
        String env = split[0].toLowerCase();
        variableName = split[1].toLowerCase();

        Map<String, String> vars = envVars.get(env);
        if (vars == null) {
            vars = new HashMap<String, String>();
            envVars.put(env, vars);
        }

        al.getToken("=");
        String value = al.readLine();
        vars.put(variableName.toLowerCase(), value);
    }


    private void doUpload() throws Exception {
        al.read("PACKAGE");
        al.read("FROM");
        String fileName = al.getValue("Filename not defined");
        al.read("TO");
        String output = al.getValue("Output file not defined");

        Set<Deployer.DeploymentOptions> options = new HashSet<Deployer.DeploymentOptions>();

        String deploymentId = null;

        String next = al.getValue();
        if (next != null) {
            if (next.equalsIgnoreCase("STATUS")) {
                deploymentId = al.getValue("Deployment id missing");

            } else if (next.equalsIgnoreCase("CHECKONLY")) {
                options.add(Deployer.DeploymentOptions.CHECK_ONLY);
            } else if (next.equalsIgnoreCase("ALLTESTS")) {
                options.add(Deployer.DeploymentOptions.ALL_TESTS);
            } else if (next.equalsIgnoreCase("RUNTESTS")) {
                options.add(Deployer.DeploymentOptions.UNPACKAGED_TESTS);
            } else if (next.equalsIgnoreCase("IGNORE_ERRORS")) {
                options.add(Deployer.DeploymentOptions.IGNORE_ERRORS);
            } else if (next.equalsIgnoreCase("IGNORE_WARNINGS")) {
                options.add(Deployer.DeploymentOptions.IGNORE_WARNINGS);
            } else {
                throw new Exception("Expected ALLTESTS, RUNTESTS, IGNORE_WARNINGS, IGNORE_ERRORS, CHECKONLY, or STATUS, not " + next);
            }
        }

        Deployer deployer = new Deployer(reconnector);
        EventFileWriter deploymentEventListener = new EventFileWriter(output, deploymentId != null);

        try {
            if (deploymentId == null) {
                deploymentId = deployer.deployZip(new File(fileName), options);
            }
            deployer.checkDeploymentComplete(deploymentId, deploymentEventListener);
            deploymentEventListener.message("Done");

        } finally {
            deploymentEventListener.close();
        }

        if (deploymentEventListener.getErrors().size() != 0) {
            throw new SQLException(deploymentEventListener.getErrors().toString());
        }
    }

    private class EventFileWriter implements DeploymentEventListener {
        private FileWriter fw;
        private List<String> errors = new ArrayList<String>();

        public List<String> getErrors() {
            return errors;
        }

        public EventFileWriter(String filename, Boolean append) throws IOException {
            fw = new FileWriter(filename, append);
        }

        public void error(String message) throws Exception {
            errors.add(message);
            errors.add("\n");
            write(message);
        }

        private void write(String message) throws IOException {
            fw.write(message);
            fw.write("\n");
            fw.flush();
            System.out.println(message);
        }

        public void message(String message) throws IOException {
            write(message);
        }

        public void setAsyncResult(AsyncResult asyncResult) {

        }

        public void progress(String message) throws IOException {
            write(message);
        }

        public void close() throws IOException {
            fw.close();
        }
    }

    private void doPackage() throws Exception {
        al.read("TO");
        String fileName = al.getValue("Filename not defined");

        FolderZipper zipper = new FolderZipper();
        zipper.zipFolder(snapshotDirectory, fileName);

        FileUtil.delete(snapshotDirectory);
    }


    private void doCommit() throws Exception {
        snapshotDirectory = FileUtil.createTempDirectory("Snapshot");

        StringBuilder errors = new StringBuilder();
        DdlDeploymentListener deploymentEventListener = new DdlDeploymentListener(errors, null);

        Map<String, Set<String>> types = deployment.getTypesToDeploy();

        Downloader dl = new Downloader(reconnector, snapshotDirectory, deploymentEventListener, null);

        for (String type : types.keySet()) {
            Set<String> members = types.get(type);
            for (String member : members) {
                dl.addPackage(type, member);
            }
        }
        File downZip = dl.download();
        FileUtil.delete(downZip);

        if (deploymentEventListener.errors.length() > 0) {
            throw new SQLException(deploymentEventListener.errors.toString());
        }

        // Walk the downloaded files, and "fix" them if needed
        if (patchApplyDefined) {
            File[] children = snapshotDirectory.listFiles();
            for (File child : children) {
                if (child.isDirectory()) {
                    patchDirectory(child, deploymentEventListener);
                }
            }
        }

        File originalPackage = new File(snapshotDirectory, "package.xml");
        if (!originalPackage.renameTo(new File(snapshotDirectory, "original-package.xml"))) {
            throw new SQLException("Unable to rename original package.xml");
        }

        FileWriter fw = new FileWriter(new File(snapshotDirectory, "package.xml"));
        fw.write(deployment.getPackageXml());
        fw.close();

        if (deployment.hasDestructiveChanges()) {
            fw = new FileWriter(new File(snapshotDirectory, "destructiveChanges.xml"));
            fw.write(deployment.getDestructiveChangesXml());
            fw.close();
        }
    }

    private void patchDirectory(File objectDirectory, DdlDeploymentListener deploymentEventListener) throws Exception {
        for (RuleDefinition ruleDefinition : ruleDefinitions) {
            if (ruleDefinition.appliesToDirectory(objectDirectory)) {
                String oldValue = sourceVars.get(ruleDefinition.variableName);
                String newValue = destVars.get(ruleDefinition.variableName);

                File[] objectFiles = objectDirectory.listFiles();
                for (File sourceFile : objectFiles) {

                    String source = FileUtil.loadTextFile(sourceFile);

                    String after;
                    if (source.startsWith("<?xml")) {
                        String sfIdentifierForXml = removePrefix(ruleDefinition.objectName);

                        after = changeXml(
                                ruleDefinition.objectType,
                                sfIdentifierForXml,
                                source, oldValue, newValue);
                    } else {
                        after = source.replace(oldValue, newValue);
                    }

                    if (!after.equals(source)) {
                        deploymentEventListener.message("Replaced '" + oldValue + "' with '" + newValue + "' in " + sourceFile.getName());
                    }

                    FileWriter fw = new FileWriter(sourceFile);
                    fw.write(after);
                    fw.close();
                }
            }
        }
    }

    // TODO: Move to a generic class
    private String removePrefix(String name) {
        if (name != null) {
            int dotPos = name.indexOf(".");
            if (dotPos != -1) {
                name = name.substring(dotPos + 1);
            }
        }
        return name;
    }

    private String changeXml(String componentType, String sfIdentifier, String source, String original, String replacement) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new ByteArrayInputStream(source.getBytes()));

        String xmlLabel = determineXmlLabel(componentType);

        NodeList downOne;
        if (xmlLabel == null) {
            downOne = doc.getChildNodes();
        } else {
            downOne = doc.getElementsByTagName(xmlLabel);
        }
        for (int i = 0; i < downOne.getLength(); i++) {
            NodeList permsList = downOne.item(i).getChildNodes();

            for (int j = 0; j < permsList.getLength(); j++) {
                Node n = permsList.item(j);
                // Identify the field we are changing
                if (xmlLabel == null || n.getNodeName().equalsIgnoreCase("fullName")) {
                    if (xmlLabel == null || n.getTextContent().equalsIgnoreCase(sfIdentifier)) {
                        adjustChildNodes(n.getParentNode(), original, replacement);
                    }
                }
            }
        }

        StringWriter sw = new StringWriter();
        DOMSource sourceDom = new DOMSource(doc);
        StreamResult resultStream = new StreamResult(sw);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(sourceDom, resultStream);

        return sw.toString();
    }

    private void adjustChildNodes(Node n, String original, String replacement) {
        NodeList nodeList = n.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeValue() != null) {

                if (node.getNodeValue().equals(original)) {
                    node.setTextContent(replacement);
                }
            }
            if (node.hasChildNodes()) {
                adjustChildNodes(node, original, replacement);
            }
        }
    }


    private void doAdd(boolean drop) throws Exception {
        String type = getObjectType();
        String name = getObjectName();

        String maybeWith = al.getValue();
        if (maybeWith != null) {
            if (maybeWith.equalsIgnoreCase("with")) {
                al.read("ListViews");
                // TODO: Handle non-stripping of ListViews
            } else {
                throw new SQLException("Unexpected parameter found: " + maybeWith);
            }
        }

        if (drop) {
            deployment.dropMember(type, name);
        } else {
            deployment.addMember(type, name, null, null);
        }
    }

    private String getObjectName() throws Exception {
        String name = al.getValue(); // TODO: Could validate
        if (name == null) {
            throw new SQLException("name not defined");
        }
        return name;
    }

    private String getObjectType() throws Exception {
        String type = al.getValue(); // TODO: Could validate
        if (type == null) {
            throw new SQLException("type not defined");
        }
        return type;
    }


    private void doStart() throws Exception {
        deployment = new Deployment(reconnector.getSfVersion());
    }


    // TODO: Move to a generic class
    private String determineXmlLabel(String componentType) throws Exception {
        if (componentType.equals("BusinessProcess")) return "businessProcesses";
        if (componentType.equals("CustomLabel")) return "labels";
        if (componentType.equals("CustomField")) return "fields";
        if (componentType.equals("ListView")) return "listViews";
        if (componentType.equals("NamedFilter")) return "namedFilters";
        if (componentType.equals("RecordType")) return "recordTypes";
        if (componentType.equals("ValidationRule")) return "validationRules";
        if (componentType.equals("WebLink")) return "webLinks";
        if (componentType.equals("WorkflowAlert")) return "alerts";
        if (componentType.equals("WorkflowFieldUpdate")) return "fieldUpdates";
        if (componentType.equals("WorkflowOutboundMessage")) return "outboundMessages";
        if (componentType.equals("WorkflowRule")) return "rules";
        if (componentType.equals("WorkflowTask")) return "tasks";

        return null;
    }

}
