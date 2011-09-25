package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.deployment.Deployer;
import com.fidelma.salesforce.deployment.Deployment;
import com.fidelma.salesforce.deployment.DeploymentEventListener;
import com.fidelma.salesforce.deployment.DeploymentEventListenerImpl;
import com.fidelma.salesforce.misc.Downloader;
import com.fidelma.salesforce.misc.FolderZipper;
import com.fidelma.salesforce.misc.Reconnector;
import com.fidelma.salesforce.parse.SimpleParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
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


    private SimpleParser al;
    private Reconnector reconnector;
//    private MetadataConnection metadataConnection;

    // There is only one active at a time...
    private static Deployment deployment;
    private static File snapshot;

    public DeployCommand(SimpleParser al, Reconnector reconnector) throws Exception {
        this.al = al;
        this.reconnector = reconnector;
    }

    public void execute() throws SQLException {
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

            } else {
                throw new SQLException("Unexpected token: '" + value + "'");
            }
        } catch (Exception e) {
            throw new SQLException(e);
        }
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

            } else if (next.equalsIgnoreCase("ALLTESTS")) {
                options.add(Deployer.DeploymentOptions.ALL_TESTS);
            } else if (next.equalsIgnoreCase("RUNTESTS")) {
                options.add(Deployer.DeploymentOptions.UNPACKAGED_TESTS);
            } else if (next.equalsIgnoreCase("IGNORE_ERRORS")) {
                options.add(Deployer.DeploymentOptions.IGNORE_ERRORS);
            } else if (next.equalsIgnoreCase("IGNORE_WARNINGS")) {
                options.add(Deployer.DeploymentOptions.IGNORE_WARNINGS);
            } else {
                throw new Exception("Expected ALLTESTS, RUNTESTS, IGNORE_WARNINGS or IGNORE_ERRORS, not " + next);
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
        zipper.zipFolder(snapshot, fileName);
    }


    private void doCommit() throws Exception {
        snapshot = new File(System.getProperty("java.io.tmpdir"),
                "SF-SNAPSHOT" + System.currentTimeMillis());

        snapshot.mkdir();

        DeploymentEventListenerImpl deploymentEventListener = new DeploymentEventListenerImpl();
        Map<String, Set<String>> types = deployment.getTypesToDeploy();

        Downloader dl = new Downloader(reconnector, snapshot, deploymentEventListener, null);

        for (String type : types.keySet()) {
            Set<String> members = types.get(type);
            for (String member : members) {
                dl.addPackage(type, member);
            }
        }
        dl.download();

        if (deploymentEventListener.getErrors().length() > 0) {
            throw new SQLException(deploymentEventListener.getErrors().toString());
        }

        // TODO: Copy xml files from sourceSchemaDir to snapshot?
        File originalPackage = new File(snapshot, "package.xml");
        if (!originalPackage.renameTo(new File(snapshot, "original-package.xml"))) {
            throw new SQLException("Unable to rename original package.xml");
        }

        FileWriter fw = new FileWriter(new File(snapshot, "package.xml"));
        fw.write(deployment.getPackageXml());
        fw.close();

        if (deployment.hasDestructiveChanges()) {
            fw = new FileWriter(new File(snapshot, "destructiveChanges.xml"));
            fw.write(deployment.getDestructiveChangesXml());
            fw.close();
        }
    }


    private void doAdd(boolean drop) throws Exception {
        String type = al.getValue(); // TODO: Could validate
        if (type == null) {
            throw new SQLException("type not defined");
        }
        String name = al.getValue(); // TODO: Could validate
        if (name == null) {
            throw new SQLException("name not defined");
        }

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


    private void doStart() throws Exception {
        deployment = new Deployment();
    }
}
