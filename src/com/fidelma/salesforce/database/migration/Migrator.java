package com.fidelma.salesforce.database.migration;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.Deployment;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.misc.Downloader;
import com.fidelma.salesforce.misc.LoginHelper;
import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CustomField;
import com.sforce.soap.metadata.CustomObject;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        // TODO: Document inability to delete "case assignment rules" and "case escalation rules"

        List<String> metaDataToDelete = new ArrayList<String>();

        metaDataToDelete.add("ApexClass");
        metaDataToDelete.add("ApexComponent");
        metaDataToDelete.add("ApexPage");
        metaDataToDelete.add("ApexTrigger");

        metaDataToDelete.add("FieldSet");
        metaDataToDelete.add("RecordType");
        metaDataToDelete.add("StaticResource");
        metaDataToDelete.add("Layout");
        metaDataToDelete.add("Workflow");


        metaDataToDelete.add("ArticleType");
        metaDataToDelete.add("CustomApplication");
        metaDataToDelete.add("CustomLabels");
//        metaDataToDelete.add("CustomObject"); (handled below)
        metaDataToDelete.add("CustomObjectTranslation");
        metaDataToDelete.add("CustomPageWebLink");
        metaDataToDelete.add("CustomSite");
        metaDataToDelete.add("CustomTab");
        metaDataToDelete.add("DataCategoryGroup");
        metaDataToDelete.add("EntitlementTemplate");
        metaDataToDelete.add("HomePageComponent");
        metaDataToDelete.add("HomePageLayout");
        metaDataToDelete.add("Portal");                        // Can I even do this?
        metaDataToDelete.add("Profile");
        metaDataToDelete.add("RemoteSiteSetting");
        metaDataToDelete.add("ReportType");
        metaDataToDelete.add("Scontrol");
        metaDataToDelete.add("Translations");


// Decide what to remove
        Deployment undeploy = new Deployment();
        MetadataConnection metadataConnection = targetInstance.getHelper().getMetadataConnection();

        List<ListMetadataQuery> queryList = new ArrayList<ListMetadataQuery>();
        for (String m : metaDataToDelete) {
            ListMetadataQuery mq = new ListMetadataQuery();
            mq.setType(m);
            queryList.add(mq);
            // Salesforce only lets us call with 3 at a time. Thanks Salesforce!
            if (queryList.size() == 3) {
                addToUndeploymentList(metadataConnection, undeploy, queryList);
            }
        }


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

        /*
        CustomField cf = new CustomField();
        cf.setFullName("Account.SLA__c");
        AsyncResult[] result = metadataConnection.delete(new Metadata[]{cf});
        AsyncResult asyncResult = result[0];
        long waitTimeMilliSecs = 1000;
        while (!asyncResult.isDone()) {
            Thread.sleep(waitTimeMilliSecs);
// double the wait time for the next iteration
            waitTimeMilliSecs *= 2;
            asyncResult = metadataConnection.checkStatus(
                    new String[]{asyncResult.getId()})[0];
            System.out.println("Status is: " + asyncResult.getState());
        }
        if (asyncResult.getState() != AsyncRequestState.Completed) {
            System.out.println(asyncResult.getStatusCode() + " msg: " +
                    asyncResult.getMessage());
        }
        System.out.println("Done!");
          */
        return targetDeployer;
    }

    private void addToUndeploymentList(MetadataConnection metadataConnection,
                                       Deployment undeploy,
                                       List<ListMetadataQuery> queryList) throws Exception {

        ListMetadataQuery[] queries = new ListMetadataQuery[queryList.size()];
        queryList.toArray(queries);

        FileProperties[] props = metadataConnection.listMetadata(
                queries,
                LoginHelper.SFDC_VERSION);

        for (FileProperties prop : props) {
            undeploy.addMember(prop.getType(), prop.getFullName(), null);
        }
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

        Set<String> processedTables = new HashSet<String>();

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
                        callback);

                processedTables.add(restoreRequest.tableName.toUpperCase());
            }
        }


        // Record the names of tables with master/detail references
        Set<String> masterRecordTables = new HashSet<String>();

        for (MigrationCriteria restoreRequest : restoreRequests) {
            Table table = salesforceMetadata.getTable(restoreRequest.tableName);
            if (tableContainsMasterDetail(table)) {
                masterRecordTables.add(table.getName().toUpperCase());
            }
        }


        // Process master/detail references
        boolean processingOccurred = true;
        while ((masterRecordTables.size() > 0) && (processingOccurred)) {
            Set<String> workSet = new HashSet<String>(masterRecordTables);
            processingOccurred = false;
            for (String tableName : workSet) {
                Table table = salesforceMetadata.getTable(tableName);
                Boolean ok = true;
                for (Column col : table.getColumns()) {
                    if ((col.getType().equalsIgnoreCase("masterrecord")) &&
                            (!processedTables.contains(col.getReferencedTable().toUpperCase()))) {
                        ok = false;
                        break;
                    }
                }

                if (ok) {
                    System.out.println("Processing " + table.getName());
                    correctReferences(destination, localDbStatement, table, callback);
                    masterRecordTables.remove(tableName.toUpperCase());
                    processedTables.add(tableName.toUpperCase());
                    processingOccurred = true;
                } else {
                    System.out.println("Skipping " + table.getName());
                }
            }
        }

        if ((!processingOccurred) && (masterRecordTables.size() > 0)) {
            throw new SQLException("Unable to migrate data in " + masterRecordTables.iterator().next());
        }


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
