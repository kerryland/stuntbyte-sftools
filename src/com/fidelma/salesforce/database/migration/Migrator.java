package com.fidelma.salesforce.database.migration;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.Deployment;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.misc.Downloader;
import org.hibernate.repackage.cglib.asm.attrs.StackMapType;
import org.omg.CORBA.StructMember;

import java.io.File;
import java.lang.management.LockInfo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 28/05/11
 * Time: 9:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class Migrator {


    public void replicate(SfConnection sourceInstance, SfConnection targetInstance, Connection localdb) throws Exception {

        // Pull source schema down to local database
        Exporter exporter = new Exporter(null);
        List<Table> tables = exporter.createLocalSchema(sourceInstance, localdb);

        /*
        // Pull source data down to local machine
        List<ExportCriteria> criteriaList = new ArrayList<ExportCriteria>();
        for (Table table : tables) {
            if (table.getType().equals("TABLE")) {
                ExportCriteria criteria = new ExportCriteria();
                criteria.tableName = table.getName();
                criteria.whereClause = "limit 4";
                criteriaList.add(criteria);
            }
        }
        exporter.downloadData(sourceInstance, localdb, criteriaList);
         */
        // Copy metadata from source instance to target instance
        File sourceSchemaDir = new File(System.getProperty("java.io.tmpdir"),
                "SF-SRC" + System.currentTimeMillis());  // TODO: This must be unique
        sourceSchemaDir.mkdir();

        DeploymentEventListener del = new DeploymentEventListener() {
            public void error(String message) {
                System.out.println("ERROR: " + message);
            }

            public void finished(String message) {
                System.out.println("FINISHED: " + message);

            }
        };

        Downloader sourceDownloader = new Downloader(sourceInstance.getHelper().getMetadataConnection(), sourceSchemaDir, del, null);
        for (Table table : tables) {
            if (table.getType().equals("TABLE")) {
                sourceDownloader.addPackage("CustomObject", table.getName());
            }
        }
        sourceDownloader.download();

        // Remove everything!
        Deployer targetDeployer = deleteAllTables(targetInstance, del);


        // Upload schema to destination instance
        targetDeployer.deployZip(sourceDownloader.getZipFile(), del);

        // Insert all data from local machine to target instance
        // map the inserted ids to the source ids

        // Update all relationship references to the new ids


        // sourceSchemaDir.delete();
    }

    public Deployer deleteAllTables(SfConnection targetInstance, DeploymentEventListener del) throws Exception {

        // TODO: Insufficient to just delete tables. Must delete code, case escalation rules, EVERYTHING

        // Decide what to remove
        Deployment undeploy = new Deployment();

        // TODO: * does not work. Need to name them explicitly
        // simplest option prob to download via * and then
        // just renamed package.xml to destructiveChanges.xml

//        undeploy.addMember("ApexClass", "*", null);
//        undeploy.addMember("ArticleType", "*", null);
//        undeploy.addMember("ApexComponent", "*", null);
//        undeploy.addMember("ApexPage", "*", null);
//        undeploy.addMember("ApexTrigger", "*", null);
//        undeploy.addMember("CustomApplication", "*", null);
//        undeploy.addMember("CustomLabels", "*", null);
//        undeploy.addMember("CustomObjectTranslation", "*", null);
//        undeploy.addMember("CustomPageWebLink", "*", null);
//        undeploy.addMember("CustomSite", "*", null);
//        undeploy.addMember("CustomTab", "*", null);
//        undeploy.addMember("DataCategoryGroup", "*", null);
//        undeploy.addMember("EntitlementTemplate", "*", null);
//        undeploy.addMember("FieldSet", "*", null);
//        undeploy.addMember("HomePageComponent", "*", null);
//        undeploy.addMember("HomePageLayout", "*", null);
//        undeploy.addMember("Layout", "*", null);
//        undeploy.addMember("Portal", "*", null);
//        undeploy.addMember("Profile", "*", null);
//        undeploy.addMember("RecordType", "*", null);
//        undeploy.addMember("RemoteSiteSetting", "*", null);
//        undeploy.addMember("ReportType", "*", null);
//        undeploy.addMember("Scontrol", "*", null);
//        undeploy.addMember("StaticResource", "*", null);
//        undeploy.addMember("Translations", "*", null);
//        undeploy.addMember("Workflow", "*", null);

        List<Table> tables = targetInstance.getMetaDataFactory().getTables();
        for (Table table : tables) {
            if (table.getType().equals("TABLE")) {
                if (table.isCustom()) {
                    undeploy.addMember("CustomObject", table.getName(), null);
                } else {
                    for (Column column : table.getColumns()) {
                        if (column.isCustom()) {
                            undeploy.addMember("CustomField", table.getName() + "." + column.getName(), null);
                        }
                    }

                }
            }
        }

        Deployer targetDeployer = new Deployer(targetInstance.getHelper().getMetadataConnection());
        targetDeployer.undeploy(undeploy, del);
        return targetDeployer;
    }

    public class RestoreRequest {
        String tableName;
        String sql;
    }

    public void restoreRows(SfConnection destination,
                            Connection localDb,
                            List<RestoreRequest> restoreRequests) throws SQLException {

        for (RestoreRequest restoreRequest : restoreRequests) {
            PreparedStatement stmt = localDb.prepareStatement(restoreRequest.sql);

            List<String> rowsToRestore = new ArrayList<String>();

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString("Id");
                rowsToRestore.add(id);
            }

            // What about master detail?


            // Look for rows to restore in destination.
            // If they exist, then we are upserting

            // If they don't exist, we are inserting



        }


    }

}
