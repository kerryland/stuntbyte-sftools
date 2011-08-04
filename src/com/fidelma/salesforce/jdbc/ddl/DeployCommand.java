package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.Deployment;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.misc.DeploymentEventListenerImpl;
import com.fidelma.salesforce.misc.Downloader;
import com.fidelma.salesforce.misc.FolderZipper;
import com.fidelma.salesforce.misc.LoginHelper;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;

import java.io.File;
import java.io.FileWriter;
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
 * DEPLOYMENT UPLOAD PACKAGE FROM "/tmp/package.zip" OUTPUT TO "/tmp/out.txt"
 */
public class DeployCommand {


    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private MetadataConnection metadataConnection;

    // There is only one active at a time...
    private static Deployment deployment;
    private static Deployment dropDeployment;
    private static File sourceSchemaDir;
    private static File snapshot;

    public DeployCommand(SimpleParser al, ResultSetFactory metaDataFactory, MetadataConnection metadataConnection) throws Exception, SQLException {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.metadataConnection = metadataConnection;
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

        String next = al.getValue();
        if (next != null) {
            if (next.equalsIgnoreCase("RUNTESTS")) {
                options.add(Deployer.DeploymentOptions.UNPACKAGED_TESTS);
            } else {
                throw new Exception("Expected RUNTESTS, not " + next);
            }
        }

        Deployer deployer = new Deployer(metadataConnection);
        DeploymentEventListenerImpl deploymentEventListener = new DeploymentEventListenerImpl();

        try {
            deployer.deployZip(new File(fileName), deploymentEventListener, options);

        } finally {
            FileWriter fw = new FileWriter(output);
            fw.write(deploymentEventListener.getMessages().toString());
            fw.write("\n");
            fw.write(deploymentEventListener.getErrors().toString());
            fw.write("Done\n");
            fw.close();
        }

        if (deploymentEventListener.getErrors().length() != 0) {
            throw new SQLException(deploymentEventListener.getErrors().toString());
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
        Map<String, List<String>> types = deployment.getTypes();

        Downloader dl = new Downloader(metadataConnection, snapshot, deploymentEventListener, null);

        for (String type : types.keySet()) {
            List<String> members = types.get(type);
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

        if (dropDeployment.hasContent()) {
            fw = new FileWriter(new File(snapshot, "destructiveChanges.xml"));
            fw.write(dropDeployment.getPackageXml());
            fw.close();
        }


//        Deployer deployer = new Deployer(metadataConnection);
//        deployer.packageZip(sourceSchemaDir);
//        deployer.deployZip();

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
            dropDeployment.addMember(type, name, null, null);
        } else {
            deployment.addMember(type, name, null, null);
        }
    }


    private void doStart() throws Exception {
        deployment = new Deployment();
        dropDeployment = new Deployment();

        if (1 != 3) {
            return;
        }

        final StringBuilder errors = new StringBuilder();

        DeploymentEventListener deploymentEventListener = new DeploymentEventListener() {
            public void error(String message) {
                errors.append(message).append(". ");
            }

            public void finished(String message) {
            }

            public void progress(String message) {

            }
        };

        sourceSchemaDir = new File(System.getProperty("java.io.tmpdir"),
                "SF-SRC" + System.currentTimeMillis());  // TODO: This must be unique
        sourceSchemaDir.mkdir();


        Downloader downloader = new Downloader(metadataConnection, sourceSchemaDir, deploymentEventListener, null);

//        Deployment deployment = new Deployment();


        List<String> metaDataToDownload = new ArrayList<String>();

        metaDataToDownload.add("ApexClass");
        metaDataToDownload.add("ApexComponent");
        metaDataToDownload.add("ApexPage");
        metaDataToDownload.add("ApexTrigger");

        metaDataToDownload.add("FieldSet");
        metaDataToDownload.add("RecordType");
        metaDataToDownload.add("StaticResource");
        metaDataToDownload.add("Layout");
        metaDataToDownload.add("Workflow");
        metaDataToDownload.add("CustomLabels");

        metaDataToDownload.add("ArticleType");

        // TODO: More of these are useful...

//        metaDataToDownload.add("CustomApplication");

//        metaDataToDelete.add("CustomObject"); (handled below)

        /*
        metaDataToDownload.add("CustomObjectTranslation");
        metaDataToDownload.add("CustomPageWebLink");
        metaDataToDownload.add("CustomSite");
        metaDataToDownload.add("CustomTab");
        metaDataToDownload.add("DataCategoryGroup");
        metaDataToDownload.add("EntitlementTemplate");
        metaDataToDownload.add("HomePageComponent");
        metaDataToDownload.add("HomePageLayout");
        metaDataToDownload.add("Portal");                        // Can I even do this?
        metaDataToDownload.add("Profile");
        metaDataToDownload.add("RemoteSiteSetting");
        metaDataToDownload.add("ReportType");
        metaDataToDownload.add("Scontrol");
        metaDataToDownload.add("Translations");
        */

        Map<String, List<String>> objectsByMetaDataType = new HashMap<String, List<String>>();

        List<ListMetadataQuery> queryList = new ArrayList<ListMetadataQuery>();
        for (String m : metaDataToDownload) {
            ListMetadataQuery mq = new ListMetadataQuery();
            mq.setType(m);
            queryList.add(mq);
            // Salesforce only lets us call with 3 at a time. Thanks Salesforce!
            if (queryList.size() == 3) {
                addToDownloadList(metadataConnection, objectsByMetaDataType, queryList);
            }
        }


        List<Table> tables = metaDataFactory.getTables();
        for (Table table : tables) {
            if (table.getType().equals("TABLE")) {
                if (table.isCustom()) {
                    addToDownloadList(objectsByMetaDataType, "CustomObject", table.getName());
                } else {
                    for (Column column : table.getColumns()) {
                        if (column.isCustom()) {
                            addToDownloadList(objectsByMetaDataType, "CustomField", table.getName() + "." + column.getName());
                        }
                    }
                }
            }
        }

        for (String metaData : objectsByMetaDataType.keySet()) {
            for (String value : objectsByMetaDataType.get(metaData)) {
                downloader.addPackage(metaData, value);
            }
        }

        downloader.download();

        if (errors.length() != 0) {
            throw new SQLException(errors.toString());
        }

    }

    private void addToDownloadList(Map<String, List<String>> objectsByMetaDataType,
                                   String metaDataType, String name) throws Exception {

//        System.out.println("ADDING " + metaDataType + " " + name);

        List<String> objects = objectsByMetaDataType.get(metaDataType);
        if (objects == null) {
            objects = new ArrayList<String>();
            objectsByMetaDataType.put(metaDataType, objects);
        }
        objects.add(name);

    }

    private void addToDownloadList(MetadataConnection metadataConnection,
                                   Map<String, List<String>> objectsByMetaDataType,
                                   List<ListMetadataQuery> queryList) throws Exception {

        ListMetadataQuery[] queries = new ListMetadataQuery[queryList.size()];
        queryList.toArray(queries);

        FileProperties[] props = metadataConnection.listMetadata(
                queries,
                LoginHelper.SFDC_VERSION);

        for (FileProperties prop : props) {
            addToDownloadList(objectsByMetaDataType, prop.getType(), prop.getFullName());
        }
        queryList.clear();
    }


}
