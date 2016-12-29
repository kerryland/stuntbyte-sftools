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
package com.stuntbyte.salesforce.database.migration;

import com.stuntbyte.salesforce.deployment.Deployer;
import com.stuntbyte.salesforce.deployment.Deployment;
import com.stuntbyte.salesforce.deployment.DeploymentEventListener;
import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.jdbc.ddl.DdlDeploymentListener;
import com.stuntbyte.salesforce.jdbc.metaforce.Column;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.jdbc.metaforce.Table;
import com.stuntbyte.salesforce.misc.Downloader;
import com.stuntbyte.salesforce.misc.FileUtil;
import com.stuntbyte.salesforce.misc.FolderZipper;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Migrate data from one instance of Salesforce to another
 */
public class Migrator {

    /**
     * Migrate Salesforce data
     *
     * @param sourceInstance Salesforce instance to pull data from
     * @param targetInstance Salesforce to push data into
     * @param localdb        Local JDBC database to use a a temporary store
     * @throws Exception
     */
    public void replicate(SfConnection sourceInstance, SfConnection targetInstance, Connection localdb) throws Exception {

        // Pull source schema down to local database
        Exporter exporter = new Exporter();
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
        File sourceSchemaDir = FileUtil.createTempDirectory("SourceInstance");

        DeploymentEventListener del = new DeploymentEventListener() {
            public void error(String message) {
                System.out.println("ERROR: " + message);
            }

            public void message(String message) {
                System.out.println(message);
            }

            public void setAsyncResult(AsyncResult asyncResult) {

            }

            public void progress(String message) {

            }
        };

        Reconnector reconnector = new Reconnector(sourceInstance.getHelper());

        Downloader sourceDownloader = new Downloader(reconnector, sourceSchemaDir, del, null);
        for (Table table : tables) {
            if (table.getType().equals("TABLE")) {
                sourceDownloader.addPackage("CustomObject", table.getName());
            }
        }
        File zipFile = sourceDownloader.download();

        // Remove everything!
        Deployer targetDeployer = deleteAllTables(targetInstance, del);


        // Upload schema to destination instance
        String deploymentId = targetDeployer.deployZip(zipFile, new HashSet<Deployer.DeploymentOptions>());
        targetDeployer.checkDeploymentComplete(deploymentId, del);

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

//        MetadataConnection metadataConnection = targetInstance.getHelper().getMetadataConnection();
        Reconnector reconnector = new Reconnector(targetInstance.getHelper());

        Deployment undeploy = new Deployment(reconnector.getSfVersion());


        List<ListMetadataQuery> queryList = new ArrayList<ListMetadataQuery>();
        for (String m : metaDataToDelete) {
            ListMetadataQuery mq = new ListMetadataQuery();
            mq.setType(m);
            queryList.add(mq);
            // Salesforce only lets us call with 3 at a time. Thanks Salesforce!
            if (queryList.size() == 3) {
                addToUndeploymentList(reconnector, undeploy, queryList);
            }
        }


        List<Table> tables = targetInstance.getMetaDataFactory().getTables();
        for (Table table : tables) {
            if (table.getType().equals("TABLE")) {
                if (table.isCustom()) {
                    undeploy.dropMember("CustomObject", table.getName());
                } else {
                    for (Column column : table.getColumns()) {
                        if (column.isCustom()) {
                            undeploy.dropMember("CustomField", table.getName() + "." + column.getName());
                        }
                    }
                }
            }
        }

        Deployer targetDeployer = new Deployer(reconnector);
        targetDeployer.deploy(undeploy, del);

        return targetDeployer;
    }

    private void addToUndeploymentList(Reconnector metadataConnection,
                                       Deployment undeploy,
                                       List<ListMetadataQuery> queryList) throws Exception {

        ListMetadataQuery[] queries = new ListMetadataQuery[queryList.size()];
        queryList.toArray(queries);

        FileProperties[] props = metadataConnection.listMetadata(
                queries,
                metadataConnection.getSfVersion());

        for (FileProperties prop : props) {
            undeploy.addMember(prop.getType(), prop.getFullName(), null, null);
        }
    }

    // TODO: restoreRequests does not honour the .sql property  -- thats ok? (need source and dest filtering?)
    public void restoreRows(SfConnection destination,
                            Connection localDb,
                            List<MigrationCriteria> restoreRequests,
                            List<MigrationCriteria> existingDataCriteriaList) throws SQLException {

        Set<String> processedTables = new HashSet<String>();

        Exporter exporter = new Exporter();

        // Insert all data, except for references and unwritable fields

        // Capture the new ids and map them to the old ids
        Statement localDbStatement = localDb.createStatement();
        localDbStatement.execute("drop table keyMap if exists");
        localDbStatement.execute("create table keyMap(tableName varchar(50), oldId varchar(18), newId varchar(18))");

        final PreparedStatement getCorrectedIds = localDb.prepareStatement(
                "select newId from keyMap where oldId = ? and tableName = ?");

        final PreparedStatement insertKeymap = localDb.prepareStatement(
                "insert into keyMap(tableName, oldId, newId) values (?,?,?)");

        mapRecordTypes(insertKeymap, destination, localDb, processedTables);

        final String defaultUserId = getDefaultUser(destination);

        mapDataInDestination(insertKeymap, destination, localDb,
                "select Id, Name from User where isActive = true",
                "User",
                new SimpleKeyBuilder("Name"),
                processedTables);

        mapDataInDestination(insertKeymap, destination, localDb, "select Id, Name from UserRole",
                "UserRole",
                new SimpleKeyBuilder("Name"), processedTables);


        for (MigrationCriteria migrationCriteria : existingDataCriteriaList) {
            mapDataInDestination(insertKeymap, destination, localDb,
                    "select Id, " + migrationCriteria.keyBuilderColumns + " from " + migrationCriteria.tableName + "  " + migrationCriteria.sql,
                    migrationCriteria.tableName,
                    migrationCriteria.keyBuilder,
                    processedTables);
        }


        // Create fresh master-detail records
        final ResultSetFactory salesforceMetadata = destination.getMetaDataFactory();

//        final List<Column> requiredColumns = new ArrayList<Column>();

        ResultSetCallback callback = new ResultSetCallback() {
            public void onRow(ResultSet rs) {

            }

            public void afterBatchInsert(String tableName, List<String> sourceIds, PreparedStatement pinsert) throws SQLException {
                // Update local db with the keys from newly inserted rows
                ResultSet keys = pinsert.getGeneratedKeys();
                int row = 0;
                while (keys.next()) {
                    insertKeymap.setString(1, tableName.toLowerCase());
                    insertKeymap.setString(2, sourceIds.get(row++));
                    insertKeymap.setString(3, keys.getString("Id"));

                    insertKeymap.execute();
                }
            }

            public boolean shouldInsert(String tableName, ResultSet rs, int col, Set<String> processedTables) throws SQLException {
                boolean insertColumn = true;

                Table table = salesforceMetadata.getTable(tableName);
                String columnName = rs.getMetaData().getColumnName(col);

                Column column = table.getColumn(columnName);

                if (columnName.equalsIgnoreCase("OwnerId")) { // Maybe base on type = User?
                    insertColumn = true;
                }
                if (column.isCalculated() || !column.isUpdateable()) {
                    insertColumn = false;

                } else if (column.getType().equalsIgnoreCase("Reference") &&
                        processedTables.contains(column.getReferencedTable().toLowerCase())) {
                    insertColumn = true;

                } else if (column.getType().equalsIgnoreCase("Reference") && column.isNillable()) {
                    insertColumn = false;
                }

//                if (insertColumn && !column.isNillable()) {
//                    requiredColumns.add(column);
//                }

                return insertColumn;
            }

            public Object alterValue(String tableName, String columnName, Object value) throws SQLException {
                Table table = salesforceMetadata.getTable(tableName);
                Column column = table.getColumn(columnName);
                Object originalValue = value;

//                System.out.print("KJS processing " + columnName + " " + column.getType() + "=" + value);
                // TODO: This should be configurable! (example of mangling email addresses)
                if (column.getType().equalsIgnoreCase("Email") && value != null) {
                    value = value.toString() + ".example.com";

                // TODO: This should be configurable! (example of mangling phone numbers)
                } else if ((columnName.equalsIgnoreCase("Phone_Number__c") || column.getType().equalsIgnoreCase("Phone")) && value != null) {
                    Double randomPhone = Math.random() * 1000;
                    value = Long.toString(randomPhone.longValue());
                }
//                System.out.println(" now " + value);


                if (column.getRelationshipType() != null && !column.isNillable()) {
                    getCorrectedIds.setObject(1, value);
                    getCorrectedIds.setString(2, column.getRelationshipType().toLowerCase());
                    ResultSet rs = getCorrectedIds.executeQuery();
                    if (rs.next()) {
                        value = rs.getString(1);
                    } else {
                        // Handle inactive users -- a common problem
                        if (column.getReferencedTable().equalsIgnoreCase("User")) {
                            value = defaultUserId;

//                            TODO: Softcode somehow!
//                        } else if (!columnName.equalsIgnoreCase("Id") && (originalValue != null) && (originalValue instanceof String) && (originalValue.equals("a0z30000000onDeAAI"))) {
//                            value = "a0zS0000000EIGtIAO";

                        } else {
                            System.out.println("KJS WARNING -- failed to make a value for " + columnName + " " + value);
                            value = null;
                        }
                    }
                    rs.close();
                }
                return value;

            }
        };

        // Copy the base data to Salesforce, but not the relationships
        for (MigrationCriteria restoreRequest : restoreRequests) {
            if (!tableContainsMasterDetail(salesforceMetadata.getTable(restoreRequest.tableName))) {

                String fromTable = restoreRequest.tableName;
                String sql = "select * from " + fromTable; //  + " " + restoreRequest.sql;

                PreparedStatement sourceStmt = localDb.prepareStatement(sql);
                ResultSet rs = sourceStmt.executeQuery();

                System.out.println("Pulling data from localdb " + fromTable );
                exporter.copyResultSetToTable(
                        destination,
                        restoreRequest.tableName,
                        rs,
                        processedTables,
                        callback);

//                processedTables.add(restoreRequest.tableName.toUpperCase());
            }
        }


        // Record the names of tables with master/detail references
        Set<String> masterRecordTables = new HashSet<String>();

        for (MigrationCriteria restoreRequest : restoreRequests) {
            Table table = salesforceMetadata.getTable(restoreRequest.tableName);
            if (tableContainsMasterDetail(table)) {
                masterRecordTables.add(table.getName().toLowerCase());
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
                            (!processedTables.contains(col.getReferencedTable().toLowerCase()))) {
                        ok = false;
                        break;
                    }
                }

                if (ok) {
                    correctReferences(destination, localDbStatement, table, callback);
                    masterRecordTables.remove(tableName.toLowerCase());
                    processedTables.add(tableName.toLowerCase());
                    processingOccurred = true;
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

    private String getDefaultUser(SfConnection destination) throws SQLException {
        String defaultUserId = null;
        PreparedStatement psStatement = destination.prepareStatement(
                "select id from User where Name = 'Fronde Admin' limit 1");
//                "select id from User where isActive = true and UserType = 'Standard' limit 1");
        ResultSet rs = psStatement.executeQuery();
        if (rs.next()) {
            defaultUserId = rs.getString("id");
        }
        return defaultUserId;
    }



    // Record type is a special case. Setup the mapping here
    private void mapRecordTypes(PreparedStatement insertKeymap, SfConnection destination, Connection localDb, Set<String> processedTables) throws SQLException {
        String sql = "select Id, SobjectType, DeveloperName from RecordType where isActive = true";

        KeyBuilder kb = new KeyBuilder() {
            public String buildKey(ResultSet rs) throws SQLException {
                return rs.getString("SobjectType") + "." + rs.getString("DeveloperName");
            }
        };

        mapDataInDestination(insertKeymap, destination, localDb, sql, "RecordType", kb, processedTables);

    }

    /**
     * Some data is already in the destination, and we have to reuse it. Define such data with this method
     *
     * @param destination - the salesforce instance where the existing data lives
     * @param tableName - the name of the table that we can't change
     * @param kb - something that tells us how to identify the licence common to the source and destination environments
     * @param processedTables
     */
    private void mapDataInDestination(PreparedStatement insertKeymap, SfConnection destination,
                                      Connection localDb, String sql, String tableName, KeyBuilder kb, Set<String> processedTables) throws SQLException {
        Map<String, String> oldIds = new HashMap<String, String>();

        PreparedStatement stmt = localDb.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            oldIds.put(kb.buildKey(rs), rs.getString("Id"));
        }
        rs.close();
        stmt.close();

        stmt = destination.prepareStatement(sql);
        rs = stmt.executeQuery();
        while (rs.next()) {
            String key = kb.buildKey(rs);
            String oldId = oldIds.get(key);
            if (oldId != null) {
                insertKeymap.setString(1, tableName.toLowerCase());
                insertKeymap.setString(2, oldId);
                insertKeymap.setString(3, rs.getString("Id"));
                insertKeymap.execute();
            }
        }
        rs.close();
        stmt.close();

        processedTables.add(tableName.toLowerCase());
    }


    private void correctReferences(Connection destination, Statement stmt, Table table, ResultSetCallback callback) throws SQLException {
        boolean hasMasterDetail = tableContainsMasterDetail(table);

        System.out.println("KJS " + table.getName() + " has master detail=" + hasMasterDetail);

        boolean first = true;
        StringBuilder selectColumns = new StringBuilder();
        StringBuilder selectJoins = new StringBuilder();

        selectColumns.append("select ");
        selectJoins.append(" left join keyMap as new");
        selectJoins.append(table.getName());
        selectJoins.append(" on new");
        selectJoins.append(table.getName());
        selectJoins.append(".tableName = '");
        selectJoins.append(table.getName().toLowerCase());
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

        System.out.println("KJS correctReferences with " + updateRefs);
        int colCount = 0;
        for (Column column : table.getColumns()) {
            if ((!column.isUpdateable() && !column.getType().equalsIgnoreCase("masterrecord")) || column.isCalculated()
                    || column.getName().equalsIgnoreCase("OwnerId") // Maybe base on type = User?
                    ) {
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
                    joinTable = joinTable + colCount;
                    selectColumns.append(joinTable);
                    selectColumns.append(".newId as ");
                    selectColumns.append(column.getName());
                    selectColumns.append(",");

                    selectJoins.append(" left join keyMap as ");
                    selectJoins.append(joinTable);
                    selectJoins.append(" on ");
                    selectJoins.append(joinTable);
                    selectJoins.append(".tableName = '");
                    selectJoins.append(column.getReferencedTable().toLowerCase());
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

            System.out.println("KJS PULL FROM " + selectColumns.toString());

            ResultSet rs = stmt.executeQuery(selectColumns.toString());
//            ResultSetMetaData metaData = rs.getMetaData();

            while (rs.next()) {
                for (int i = 1; i <= colCount; i++) {
//                    System.out.println("KJS GOT ID " + rs.getString(i) + " " + (rs.getString(i) == null));
                    if ((i == colCount) && hasMasterDetail) {
                        sourceIds.add(rs.getString(i));
                    } else {
//                        Object value = callback.alterValue(table.getName(), metaData.getColumnName(i), rs.getString(i));
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


    /**
     * Migrates all data specified in MigrationCriteria from
     * the source salesforce instance to the destination saleforce instance.
     * <p/>
     * WARNING: All existing rows in the destination salesforce migration tables will be deleted,
     * so this is expected to be used to provide a big-bang migration of data.
     * (TODO: We could populate localdb from sourceSalesforce for those tables not in migration list,
     * which should allow for incremental migrations that still respect new ids)
     * <p/>
     *
     * @param h2Conn - is a local database connection used to hold data during migration.
     * @param migrationCriteriaList - is a list of tables that will be deleted from dest and migrated from source
     * @param existingDataCriteriaList - is a list of tables that NOT be deleted from dest and NOT migrated from source,
     *                                   but who have references that will be used to link migrated data from other tables
     */
    public void migrateData(SfConnection sourceSalesforce, SfConnection destSalesforce, Connection h2Conn,
                            List<MigrationCriteria> migrationCriteriaList,
                            List<MigrationCriteria> existingDataCriteriaList) throws Exception {

        Set<String> tableNames = correctTableNames(sourceSalesforce, migrationCriteriaList);

        Reconnector destinationConnector = new Reconnector(destSalesforce.getHelper());

        StringBuilder errors = new StringBuilder();
        DdlDeploymentListener del = new DdlDeploymentListener(errors, null);

        File srcSchemaDir = FileUtil.createTempDirectory("Triggers");

        File originalFile = downloadBackup(destSalesforce, tableNames, destinationConnector, del, srcSchemaDir);
        File restoreZip = createFileToRestore(srcSchemaDir);
        File unenabledFile = createUnenabledFile(srcSchemaDir);

        Deployer deployer = new Deployer(destinationConnector);
        HashSet<Deployer.DeploymentOptions> options = new HashSet<Deployer.DeploymentOptions>();
        options.add(Deployer.DeploymentOptions.IGNORE_WARNINGS);
        options.add(Deployer.DeploymentOptions.IGNORE_ERRORS);
        options.add(Deployer.DeploymentOptions.ALLOW_MISSING_FILES);
        String deploymentId = deployer.deployZip(unenabledFile, options);
        deployer.checkDeploymentComplete(deploymentId, del);

        if (errors.length() != 0) {
            throw new Exception("Disable of components failed " + errors.toString());
        }

        try {
            for (String table : tableNames) {
                try {
                    System.out.println("Deleting records from " + table);
                    destSalesforce.createStatement().execute("delete from " + table);
                } catch (SQLException e) {
                    System.out.println("KJS unable to delete from " + table + ": " + e.getMessage());
                }
            }

            Exporter exporter = new Exporter();
            exporter.createLocalSchema(sourceSalesforce, h2Conn);



            Set<String> processedTables = new HashSet<String>();



            // We always need these for mappings...
            migrationCriteriaList.add(new MigrationCriteria("RecordType"));
            migrationCriteriaList.add(new MigrationCriteria("User"));
            migrationCriteriaList.add(new MigrationCriteria("UserRole"));
//            migrationCriteriaList.add(new MigrationCriteria("Group"));  -- TODO: Handle horrible table name!
            exporter.downloadData(sourceSalesforce, h2Conn, migrationCriteriaList, processedTables);

            // Download data from source for use in id mapping only -- this is never loaded into destination
            exporter.downloadData(sourceSalesforce, h2Conn, existingDataCriteriaList, processedTables);


            // Create a simpler restore migration criteria, with no 'sql' criteria,
            // and duplicated tables removed.
            List<MigrationCriteria> restoreCriteria = new ArrayList<MigrationCriteria>();
            for (String tableName : tableNames) {
                System.out.println("Restoring rows from " + tableName);
                MigrationCriteria criteria = new MigrationCriteria(tableName);
                restoreCriteria.add(criteria);
            }

            restoreRows(destSalesforce, h2Conn, restoreCriteria, existingDataCriteriaList);

        } finally {
 //           System.out.println("Restoring schema " + restoreZip.getAbsolutePath());
            String deployId = deployer.deployZip(restoreZip, options);
            deployer.checkDeploymentComplete(deployId, del);
//            System.out.println("Restoring schema... done");
            if (errors.length() != 0) {
                throw new Exception("Restore of schema in " + originalFile.getAbsolutePath() +
                        " failed " + errors.toString());
            }

        }
    }

    private File createUnenabledFile(File sourceSchemaDir) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        unenableThings(dBuilder, new File(sourceSchemaDir, "workflows"), ".workflow", "active", "false");
        unenableThings(dBuilder, new File(sourceSchemaDir, "triggers"), ".xml", "status", "Inactive");
        unenableThings(dBuilder, new File(sourceSchemaDir, "objects"), ".object", "active", "false");


        File up = File.createTempFile("SFDC-UP-UNENABLED", ".ZIP");
        FolderZipper zipper = new FolderZipper();
        zipper.zipFolder(sourceSchemaDir, up.getAbsolutePath());
        return up;
    }

    private File createFileToRestore(File sourceSchemaDir) throws Exception {
        removePackagedFields(new File(sourceSchemaDir, "objects"));
        File restoreZip = File.createTempFile("SFDC-RESTORE", ".ZIP");
        FolderZipper zipper = new FolderZipper();
        zipper.zipFolder(sourceSchemaDir, restoreZip.getAbsolutePath());
        return restoreZip;
    }

    private File downloadBackup(SfConnection destSalesforce, Set<String> tableNames, Reconnector destinationConnector,
                                DeploymentEventListener del, File sourceSchemaDir) throws Exception {
        Downloader destinationBackup = new Downloader(destinationConnector, sourceSchemaDir,
                del, null);


        // TODO: Why don't we do anyting with triggersToEnable???
        List<String> triggersToEnable = new ArrayList<String>();
        PreparedStatement findTrigger = destSalesforce.prepareStatement(
                "select Name from ApexTrigger " +
                        "where Status = 'Active' " +
                        "and NamespacePrefix = '' " +
                        "and TableEnumOrId=?");

        for (String table : tableNames) {
            destinationBackup.addPackage("Workflow", table);
            destinationBackup.addPackage("CustomObject", table); // for validation rules

            findTrigger.setString(1, table);

            ResultSet rs = findTrigger.executeQuery();
            while (rs.next()) {
                destinationBackup.addPackage("ApexTrigger", rs.getString("Name"));
                triggersToEnable.add(rs.getString("Name"));
            }
        }

        return destinationBackup.download();
    }

    private Set<String> correctTableNames(SfConnection sourceSalesforce, List<MigrationCriteria> migrationCriteriaList) throws SQLException {
        Set<String> tableNames = new HashSet<String>();

        // Salesforce is fussy about getting the case right in package.xml
        for (int i = 0; i < migrationCriteriaList.size(); i++) {
            MigrationCriteria mc = migrationCriteriaList.get(i);
            Table t = sourceSalesforce.getMetaDataFactory().getTable(mc.tableName);
            mc.tableName = t.getName();
            tableNames.add(t.getName());
        }
        return tableNames;
    }

    private static void removePackagedFields(File objectsDir) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        File[] objectFiles = objectsDir.listFiles();
        for (File objectFile : objectFiles) {
            System.out.println("Removing packaged fields from " + objectFile.getName());
            Document doc = dBuilder.parse(objectFile);

            List<Node> fieldsToDelete = new ArrayList<Node>();

            NodeList fieldsNodes = doc.getElementsByTagName("fields");
            for (int i = 0; i < fieldsNodes.getLength(); i++) {
                Node field = fieldsNodes.item(i);
                NodeList children = field.getChildNodes();

                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (child.getNodeName().equals("fullName")) {

                        if (child.getTextContent().indexOf("__") != child.getTextContent().lastIndexOf("__")) {
                            fieldsToDelete.add(field);
                            break;
                        }
                    }

                }
            }

            for (Node field : fieldsToDelete) {
                field.getParentNode().removeChild(field);
            }
            doc.normalize();

//            StringWriter sw = new StringWriter();
            FileOutputStream sw = new FileOutputStream(objectFile);
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(sw);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, result);
            sw.close();

        }
    }

    private static void unenableThings(DocumentBuilder dBuilder,
                                       File child, String suffix,
                                       String tagname, String inactiveValue) throws Exception {
        File[] profileFiles = child.listFiles();
        if (profileFiles == null) {
            System.out.println("KJS no children found for " + child.getName());
        } else {
            for (File profileFile : profileFiles) {
                if (profileFile.getName().toLowerCase().endsWith(suffix.toLowerCase())) {
                    System.out.println("Unenabling things in " + profileFile.getName());
                    Document doc = dBuilder.parse(profileFile);

                    NodeList downOne = doc.getElementsByTagName(tagname);
                    for (int i = 0; i < downOne.getLength(); i++) {
                        Node n = downOne.item(i);
                        // Don't disable recordTypes
                        if (!n.getParentNode().getNodeName().equalsIgnoreCase("recordTypes")) {
                            n.setTextContent(inactiveValue);
                        }
                    }

                    FileOutputStream sw = new FileOutputStream(profileFile);
                    DOMSource source = new DOMSource(doc);
                    StreamResult result = new StreamResult(sw);

                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    transformer.transform(source, result);
                    sw.close();

                } else {
//                System.out.println("NOT Processing " + profileFile.getAbsolutePath());
                }

            }
        }
    }


}
