package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.ddl.AlterTable;
import com.fidelma.salesforce.jdbc.ddl.CreateTable;
import com.fidelma.salesforce.jdbc.ddl.DeployCommand;
import com.fidelma.salesforce.jdbc.ddl.DropTable;
import com.fidelma.salesforce.jdbc.ddl.Grant;
import com.fidelma.salesforce.jdbc.dml.Delete;
import com.fidelma.salesforce.jdbc.dml.Insert;
import com.fidelma.salesforce.jdbc.dml.Select;
import com.fidelma.salesforce.jdbc.dml.Update;
import com.fidelma.salesforce.jdbc.metaforce.ColumnMap;
import com.fidelma.salesforce.jdbc.metaforce.ForceResultSet;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.misc.LoginHelper;
import com.fidelma.salesforce.misc.Reconnector;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class SfStatement implements java.sql.Statement {

    private SfConnection sfConnection;
    //    private PartnerConnection pc;
    private Reconnector reconnector;
    private int updateCount = -1;
    private String generatedId;
    private List<String> generatedIds = new ArrayList<String>();
    private List<Object> batchDDL = new ArrayList<Object>();

    public SfStatement(SfConnection sfConnection, LoginHelper helper) {
        this.sfConnection = sfConnection;
        reconnector = new Reconnector(helper);
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        generatedIds.clear();
        sql = stripComments(sql);
        if (sql.toUpperCase().startsWith("SELECT")) {
            Select select = new Select(this, reconnector);
            return select.execute(sql);
        } else {
            throw new SQLFeatureNotSupportedException("Don't understand that SQL command");
        }
    }

    private String stripComments(String sql) {
        LineNumberReader r = new LineNumberReader(new StringReader(sql));
        String line;
        StringBuilder result = new StringBuilder();
        try {
            line = r.readLine();
            while (line != null) {
                if (!line.startsWith("--")) {
                    result.append(line);
                    result.append("\n");
                }
                line = r.readLine();
            }
        } catch (IOException e) {
            // meh -- will. not. happen
        }
        return result.toString();
    }

    enum DmlType {INSERT, UPDATE, DROP_TABLE, CREATE_TABLE, DELETE}

    private DmlType batchDmlType = null;
    private List<SObject> batchSObjects = new ArrayList<SObject>();
    private List<Integer> batchResults = new ArrayList<Integer>();

    public int executeUpdate(String sql) throws SQLException {
        return executeUpdate(sql, false);
    }

    protected boolean isBatchMode() {
        return batchDmlType != null;
    }

    private void checkBatchMode(Boolean batchMode, DmlType checkType) throws SQLException {
        if (batchMode) {
            if ((batchDmlType != null) && (batchDmlType != checkType)) {
                throw new SQLException("Batchmode only supports one DML type per statement");
            }
            batchDmlType = checkType;
        }
    }

    public int executeUpdate(String sql, Boolean batchMode) throws SQLException {
        try {
            sql = stripComments(sql);
            // System.out.println(sql);
            generatedIds.clear();
            SimpleParser al = new SimpleParser(sql);
            LexicalToken token = al.getToken();

            updateCount = 0;
            if (token.getValue().equalsIgnoreCase("UPDATE")) {
                checkBatchMode(batchMode, DmlType.UPDATE);
                Update update = new Update(al, sfConnection.getMetaDataFactory(), reconnector);
                updateCount = update.execute(batchMode, batchSObjects);

            } else if (token.getValue().equalsIgnoreCase("INSERT")) {
                checkBatchMode(batchMode, DmlType.INSERT);
                Insert insert = new Insert(al, sfConnection.getMetaDataFactory(), reconnector);
                updateCount = insert.execute(batchMode, batchSObjects);
                generatedIds.add(insert.getGeneratedId());

            } else if (token.getValue().equalsIgnoreCase("DELETE")) {
                checkBatchMode(batchMode, DmlType.DELETE);
                Delete delete = new Delete(al, reconnector);
                updateCount = delete.execute(batchMode, batchSObjects);

//            } else if (batchMode) {
//                throw new SQLException(token.getValue() + " command not supported in batch mode");

            } else if (token.getValue().equalsIgnoreCase("CREATE")) {
                al.read("TABLE");
                CreateTable createTable = new CreateTable(al, sfConnection.getMetaDataFactory(), reconnector);

                if (batchMode) {
                    checkBatchMode(batchMode, DmlType.CREATE_TABLE);
                    Table table = createTable.parse();                  //  What if 2nd CREATE TABLE refers to first...
                    batchDDL.add(table);
                } else {
                    createTable.executeCreate();
                }

            } else if (token.getValue().equalsIgnoreCase("ALTER")) {
                al.read("TABLE");

                AlterTable alterTable = new AlterTable(al, sfConnection.getMetaDataFactory(), reconnector);
                alterTable.execute();
                // TODO: Support batch? (Can you alter two columns on the same table?)

            } else if (token.getValue().equalsIgnoreCase("DROP")) {
                al.read("TABLE");

                DropTable dropTable = new DropTable(al, sfConnection.getMetaDataFactory(), reconnector);
                if (batchMode) {
                    checkBatchMode(batchMode, DmlType.DROP_TABLE);
                    String tableName = dropTable.parse();
                    if (tableName != null) {
                        batchDDL.add(tableName);
                    }
                } else {
                    dropTable.execute();
                }

            } else if (token.getValue().equalsIgnoreCase("GRANT")) {
                new Grant(al, sfConnection.getMetaDataFactory(), reconnector).execute(true);

            } else if (token.getValue().equalsIgnoreCase("REVOKE")) {
                new Grant(al, sfConnection.getMetaDataFactory(), reconnector).execute(false);

            } else if (token.getValue().equalsIgnoreCase("COMMIT")) {
            } else if (token.getValue().equalsIgnoreCase("ROLLBACK")) {
            } else if ((token.getValue().equalsIgnoreCase("DEPLOYMENT")) ||
                    (token.getValue().equalsIgnoreCase("DEP"))) {
                new DeployCommand(al, reconnector).execute();

            } else {
                throw new SQLException("Unsupported command " + token.getValue());
            }

            if (batchMode) {
                batchResults.add(updateCount);
            }

            return updateCount;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private int maxRows = 0;

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }


    public void close() throws SQLException {

    }

    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    public void setMaxFieldSize(int max) throws SQLException {

    }


    public void setEscapeProcessing(boolean enable) throws SQLException {

    }

    public int getQueryTimeout() throws SQLException {
        return 0;
    }

    public void setQueryTimeout(int seconds) throws SQLException {

    }

    public void cancel() throws SQLException {

    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void clearWarnings() throws SQLException {

    }

    public void setCursorName(String name) throws SQLException {

    }

    private ResultSet executeResultSet;

    public boolean execute(String sql) throws SQLException {
        if (sql.toUpperCase().startsWith("SELECT")) {
            executeResultSet = executeQuery(sql);
        } else {
            executeUpdate(sql);
        }
        return true;
    }

    public ResultSet getResultSet() throws SQLException {
        return executeResultSet;
    }

    public int getUpdateCount() throws SQLException {
        return updateCount;
    }

    public boolean getMoreResults() throws SQLException {
        return false;
    }

    public void setFetchDirection(int direction) throws SQLException {

    }

    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }

    private int fetchSize = 200;

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public int getResultSetConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    public int getResultSetType() throws SQLException {
        return 0;
    }

    public Connection getConnection() throws SQLException {
        return sfConnection;
    }

    public boolean getMoreResults(int current) throws SQLException {
        return false;
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        for (String generatedId : generatedIds) {
            ColumnMap<String, Object> row = new ColumnMap<String, Object>();
            row.put("Id", generatedId);
            maps.add(row);
        }
        return new ForceResultSet(maps);
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return executeUpdate(sql);
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return execute(sql);
    }

    // SQLFeatureNotSupportedException from here down...
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }


    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }


    public void addBatch(String sql) throws SQLException {
        executeUpdate(sql, true);
    }

    public void clearBatch() throws SQLException {
        batchDDL.clear();
        batchSObjects.clear();
    }

    public int[] executeBatch() throws SQLException {
        generatedIds.clear();
        try {
            if (batchDmlType == DmlType.INSERT) {
                Insert insert = new Insert(null, sfConnection.getMetaDataFactory(), reconnector);

                SObjectChunker chunker = new SObjectChunker(200, batchSObjects);
                while (chunker.next()) {
                    SObject[] arr = chunker.nextChunk();

                    insert.saveSObjects(arr);
                    for (SObject sObject : arr) {
                        generatedIds.add(sObject.getId());
                    }
                }

            } else if (batchDmlType == DmlType.UPDATE) {
                Update update = new Update(null, sfConnection.getMetaDataFactory(), reconnector);

                SObjectChunker chunker = new SObjectChunker(200, batchSObjects);
                while (chunker.next()) {
                    SObject[] arr = chunker.nextChunk();
                    update.saveSObjects(arr);
                }

            } else if (batchDmlType == DmlType.DELETE) {
                Delete delete = new Delete(null, reconnector);
                SObjectChunker chunker = new SObjectChunker(200, batchSObjects);
                while (chunker.next()) {
                    SObject[] arr = chunker.nextChunk();
                    delete.deleteSObjects(arr);
                }

            } else if (batchDmlType == DmlType.DROP_TABLE) {
                DropTable dropTable = new DropTable(null, sfConnection.getMetaDataFactory(), reconnector);
                List<String> tablesToDrop = new ArrayList<String>();
                for (Object tableName : batchDDL) {
                    tablesToDrop.add((String) tableName);
                }
                dropTable.dropTables(tablesToDrop);

            } else if (batchDmlType == DmlType.CREATE_TABLE) {
                CreateTable createTable = new CreateTable(null, sfConnection.getMetaDataFactory(), reconnector);

                List<Table> tables = new ArrayList<Table>();
                for (Object table : batchDDL) {
                    tables.add((Table) table);
                }
                createTable.createTables(tables);

            } else if (batchDmlType == null) {
                // We never called 'addBatch'. That's an OK thing to do

            } else {
                throw new SQLException("Unknown batch type '" + batchDmlType + "' -- cannot executeBatch");
            }
            clearBatch();
            batchDmlType = null;

        } catch (ConnectionException e) {
            throw new SQLException(e);
        }

        int[] br = new int[batchResults.size()];
        int i = 0;
        for (Integer batchResult : batchResults) {
            br[i++] = batchResult;
        }
        return br;
    }


    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    public boolean isClosed() throws SQLException {
        return false;
    }

    public void setPoolable(boolean poolable) throws SQLException {

    }

    public boolean isPoolable() throws SQLException {
        return false;
    }

    public void closeOnCompletion() throws SQLException {
    }

    public boolean isCloseOnCompletion() throws SQLException {
        return true;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }


    private class SObjectChunker {
        private int chunkSize;
        private int ptr;
        private SObject[] input;

        public SObjectChunker(int chunkSize, List<SObject> sObjects) {
            this.chunkSize = chunkSize;
            input = new SObject[sObjects.size()];
            sObjects.toArray(input);

            ptr = 0;
        }

        public SObject[] nextChunk() {
            SObject[] chunk;
            if (input.length - ptr <= chunkSize) {
                chunk = Arrays.copyOfRange(input, ptr, input.length);
                ptr = Integer.MAX_VALUE;
            } else {
                int end = Math.min(input.length, ptr + chunkSize);
                chunk = Arrays.copyOfRange(input, ptr, end);
                ptr += chunkSize;
            }
            return chunk;
        }

        public boolean next() throws ConnectionException {
            if (ptr <= input.length) {
                return true;
            }
            return false;
        }

    }
}
