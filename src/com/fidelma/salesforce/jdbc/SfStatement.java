package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.ddl.CreateTable;
import com.fidelma.salesforce.jdbc.dml.Delete;
import com.fidelma.salesforce.jdbc.dml.Insert;
import com.fidelma.salesforce.jdbc.dml.Select;
import com.fidelma.salesforce.jdbc.dml.Update;
import com.fidelma.salesforce.jdbc.metaforce.ColumnMap;
import com.fidelma.salesforce.jdbc.metaforce.ForceResultSet;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.misc.LoginHelper;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.*;
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
import java.util.List;

/**
 */
public class SfStatement implements java.sql.Statement {

    private SfConnection sfConnection;
    private PartnerConnection pc;
    private MetadataConnection metadataConnection;
    private int updateCount = -1;
    private String generatedId;

    public SfStatement(SfConnection sfConnection, LoginHelper helper) throws ConnectionException, SQLException {
        this.sfConnection = sfConnection;
        pc = helper.getPartnerConnection();
        metadataConnection = helper.getMetadataConnection();
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        generatedId = null;
        sql = stripComments(sql);
        Select select = new Select(this, pc);
        return select.execute(sql);
    }

    private String stripComments(String sql) {
        LineNumberReader r = new LineNumberReader(new StringReader(sql));
        String line = null;
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

    public int executeUpdate(String sql) throws SQLException {
        try {
            sql = stripComments(sql);
            generatedId = null;
            SimpleParser al = new SimpleParser(sql);
            LexicalToken token = al.getToken();

            updateCount = 0;
            if (token.getValue().equalsIgnoreCase("UPDATE")) {
                Update update = new Update(al, sfConnection.getMetaDataFactory(), pc);
                updateCount = update.execute();

            } else if (token.getValue().equalsIgnoreCase("INSERT")) {
                Insert insert = new Insert(al, sfConnection.getMetaDataFactory(), pc);
                updateCount = insert.execute();
                generatedId = insert.getGeneratedId();

            } else if (token.getValue().equalsIgnoreCase("DELETE")) {
                Delete delete = new Delete(al, pc);
                updateCount = delete.execute();

            } else if (token.getValue().equalsIgnoreCase("CREATE")) {
                al.read("TABLE");

                CreateTable createTable = new CreateTable(al, pc, metadataConnection);
                createTable.execute();

            } else if (token.getValue().equalsIgnoreCase("COMMIT")) {
            } else if (token.getValue().equalsIgnoreCase("ROLLBACK")) {
            } else {
                throw new SQLException("Unsupported command " + token.getValue());
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
        ColumnMap<String, Object> row = new ColumnMap<String, Object>();
        row.put("Id", generatedId);
        maps.add(row);
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
        throw new SQLFeatureNotSupportedException();
    }

    public void clearBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public int[] executeBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException();
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

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
