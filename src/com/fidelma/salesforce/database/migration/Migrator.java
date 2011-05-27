package com.fidelma.salesforce.database.migration;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.Deployment;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.misc.Downloader;

import java.io.File;
import java.sql.Connection;
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
                "SF-SRC" + System.currentTimeMillis()) ;  // TODO: This must be unique
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

        // Obliterate the destination instance
        // Decide what to remove
        Deployment undeploy = new Deployment();
        tables = targetInstance.getMetaDataFactory().getTables();
        for (Table table : tables) {
            if (table.getType().equals("TABLE")) {
                for (Column column : table.getColumns()) {
                    if (table.isCustom()) {
                        undeploy.addMember("CustomObject", table.getName(), null);
                    } else if (column.isCustom()) {
                        undeploy.addMember("CustomField", table.getName() + "." + column.getName(), null);
                    }
                }
            }
        }

        // Remove everything!
        Deployer targetDeployer = new Deployer(targetInstance.getHelper().getMetadataConnection());
        targetDeployer.undeploy(undeploy, del);

        // Upload schema to destination instance
        targetDeployer.deployZip(sourceDownloader.getZipFile(), del);

        // Insert all data from local machine to target instance
        // map the inserted ids to the source ids

        // Update all relationship references to the new ids



       // sourceSchemaDir.delete();
    }

}
