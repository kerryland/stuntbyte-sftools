package com.fidelma.salesforce.database.migration;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.Deployment;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.misc.Downloader;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    public void restoreRows(SfConnection destination,
                            Connection localDb,
                            List<MigrationCriteria> restoreRequests) throws SQLException {

        Exporter exporter = new Exporter(null);

        // Insert all data, except for references and unwritable fields

        // Capture the new ids and map them to the old ids
        Statement localDbStatement = localDb.createStatement();
        localDbStatement.execute("drop table keyMap if exists");
        localDbStatement.execute("create table keyMap(tableName varchar(50), oldId varchar(18), newId varchar(18))");

        final PreparedStatement insertKeymap = localDb.prepareStatement(
                "insert into keyMap(tableName, oldId, newId) values (?,?,?)");

        // Create fresh master-detail records
        final ResultSetFactory salesforceMetadata = destination.getMetaDataFactory();

        ResultSetCallback callback = new ResultSetCallback() {
            public void onRow(ResultSet rs) {

            }

            public void afterBatchInsert(String tableName, List<String> sourceIds, PreparedStatement pinsert) throws SQLException {
                // Update local db with the keys from newly inserted rows
                ResultSet keys = pinsert.getGeneratedKeys();
                int row = 0;
                while (keys.next()) {
                    insertKeymap.setString(1, tableName);
                    insertKeymap.setString(2, sourceIds.get(row++));
                    insertKeymap.setString(3, keys.getString("Id"));

                    insertKeymap.execute();
                }
            }

            public boolean shouldInsert(String tableName, ResultSet rs, int col) throws SQLException {
                boolean result = true;

                if (tableName.endsWith("__s")) {
                    tableName = tableName.substring(0, tableName.length() - 3);
                }
                Table table = salesforceMetadata.getTable(tableName);
                String columnName = rs.getMetaData().getColumnName(col);
                String dataType = table.getColumn(columnName).getType();

                if (table.getColumn(columnName).isCalculated()) {
                    result = false;
//                } else if (dataType.equalsIgnoreCase("Reference") || (dataType.equalsIgnoreCase("masterrecord"))) {
                } else if (dataType.equalsIgnoreCase("Reference")) {
                    result = false;
                }
                return result;
            }
        };


        // TODO: Disable triggers and workflow


        // Copy the base data to Salesforce, but not the relationships
        for (MigrationCriteria restoreRequest : restoreRequests) {
            if (!tableContainsMasterDetail(salesforceMetadata.getTable(restoreRequest.tableName))) {
                String sql = "select * from " + restoreRequest.tableName + " " + restoreRequest.sql;

                PreparedStatement sourceStmt = localDb.prepareStatement(sql);
                ResultSet rs = sourceStmt.executeQuery();

                exporter.copyResultSetToTable(
                        destination,
                        restoreRequest.tableName,
                        rs,
                        callback
                );
            }
        }

        // Copy all data for "masterrecord" rows, including relationships all at once.
        for (MigrationCriteria restoreRequest : restoreRequests) {
            Table table = salesforceMetadata.getTable(restoreRequest.tableName);
            if (tableContainsMasterDetail(table)) {
                correctReferences(destination, localDbStatement, table, callback);
            }
        }

        // TODO: Handle cyclic dependencies that may exist for masterrecords

        // Now correct the relationships for non-masterrecord tables
        for (MigrationCriteria request : restoreRequests) {
            Table table = salesforceMetadata.getTable(request.tableName);
            if (!tableContainsMasterDetail(table)) {
                correctReferences(destination, localDbStatement, table, null);
            }
        }
    }

    private void correctReferences(Connection destination, Statement stmt, Table table, ResultSetCallback callback) throws SQLException {
        boolean hasMasterDetail = tableContainsMasterDetail(table);

        boolean first = true;
        StringBuilder selectColumns = new StringBuilder();
        StringBuilder selectJoins = new StringBuilder();

        selectColumns.append("select ");
        selectJoins.append(" left join keyMap as new");
        selectJoins.append(table.getName());
        selectJoins.append(" on new");
        selectJoins.append(table.getName());
        selectJoins.append(".tableName = '");
        selectJoins.append(table.getName());
        selectJoins.append("' and new");
        selectJoins.append(table.getName());
        selectJoins.append(".oldId =");
        selectJoins.append(table.getName());
        selectJoins.append(".Id");

        String updateRefs;
        StringBuilder insertCols = new StringBuilder();
        StringBuilder insertValues = new StringBuilder();

        if (hasMasterDetail) {
            updateRefs = "insert into " + table.getName() + " (";
        } else {
            updateRefs = "update " + table.getName() + " set ";
        }
        int colCount = 0;
        for (Column column : table.getColumns()) {
            if (column.isCalculated()) {
                continue;
            }
            String joinTable = column.getReferencedTable();
            if (joinTable != null || hasMasterDetail) {
                colCount++;
                if (!first) {
                    if (hasMasterDetail) {
                        insertCols.append(",");
                        insertValues.append(",");
                    } else {
                        updateRefs += ",";
                    }
                }
                first = false;

                if (joinTable != null) {
                    selectColumns.append(joinTable);
                    selectColumns.append(".newId as ");
                    selectColumns.append(column.getName());
                    selectColumns.append(",");

                    selectJoins.append(" left join keyMap as ");
                    selectJoins.append(joinTable);
                    selectJoins.append(" on ");
                    selectJoins.append(joinTable);
                    selectJoins.append(".tableName = '");
                    selectJoins.append(joinTable);
                    selectJoins.append("' and ");
                    selectJoins.append(joinTable);
                    selectJoins.append(".oldId =");
                    selectJoins.append(table.getName());
                    selectJoins.append(".");
                    selectJoins.append(column.getName());
                } else {
                    selectColumns.append(column.getName());
                    selectColumns.append(",");
                }

                if (hasMasterDetail) {
                    insertCols.append(column.getName());
                    insertValues.append("?");
                } else {
                    updateRefs += column.getName() + "=?";
                }
            }
        }
        if (!hasMasterDetail) {
            updateRefs += " where id = ?";
        } else {
            updateRefs += insertCols.toString() + ") values (" + insertValues.toString() + ")";
        }

        PreparedStatement updateStmt = destination.prepareStatement(updateRefs);

        if (!hasMasterDetail) {
            selectColumns.append(" new").append(table.getName()).append(".newId as Id");
        } else {
            selectColumns.append(table.getName()).append(".Id as Id");
        }
        selectColumns.append(" from ");
        selectColumns.append(table.getName());
        selectColumns.append(selectJoins);

        if (colCount > 0) {
            colCount++;

            List<String> sourceIds = new ArrayList<String>();
            ResultSet rs = stmt.executeQuery(selectColumns.toString());
            while (rs.next()) {
                for (int i = 1; i <= colCount; i++) {
                    if ((i == colCount) && hasMasterDetail) {
                        sourceIds.add(rs.getString(i));
                    } else {
                        updateStmt.setString(i, rs.getString(i));
                    }
                }
                updateStmt.addBatch();
            }
            updateStmt.executeBatch();

            if (hasMasterDetail) {
                callback.afterBatchInsert(table.getName(), sourceIds, updateStmt);
            }
        }
    }

    private boolean tableContainsMasterDetail(Table table) {
        boolean result = false;
        for (Column column : table.getColumns()) {
            if (column.getType().equalsIgnoreCase("masterrecord")) {
                result = true;
                break;
            }
        }
        return result;
    }

}
