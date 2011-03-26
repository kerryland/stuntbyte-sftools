package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.dml.Delete;
import com.fidelma.salesforce.jdbc.dml.Insert;
import com.fidelma.salesforce.jdbc.dml.Select;
import com.fidelma.salesforce.jdbc.dml.Update;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.misc.LoginHelper;
import com.fidelma.salesforce.misc.SimpleParser;
import com.sforce.soap.partner.*;
import com.sforce.ws.ConnectionException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;

/**
 */
public class SfStatement implements java.sql.Statement {

    private SfConnection sfConnection;
    private PartnerConnection pc;

    public SfStatement(SfConnection sfConnection, LoginHelper helper) throws ConnectionException, SQLException {
        this.sfConnection = sfConnection;
        pc = helper.getPartnerConnection();
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        Select select = new Select(this, pc);
        return select.execute(sql);
    }

    public int executeUpdate(String sql) throws SQLException {
        try {
            SimpleParser al = new SimpleParser(sql);
            LexicalToken token = al.getToken();

            int count = 0;
            if (token.getValue().equalsIgnoreCase("UPDATE")) {
                Update update = new Update(al, sfConnection.getMetaDataFactory(), pc);
                count = update.execute();

            } else if (token.getValue().equalsIgnoreCase("INSERT")) {
                Insert insert = new Insert(al, sfConnection.getMetaDataFactory(), pc);
                count = insert.execute();

            } else if (token.getValue().equalsIgnoreCase("DELETE")) {
                Delete delete = new Delete(al, sfConnection.getMetaDataFactory(), pc);
                count = delete.execute();

            } else if (token.getValue().equalsIgnoreCase("COMMIT")) {
            } else {
                throw new SQLException("Unsupported command " + token.getValue());
            }

            return count;
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

    // TODO: Think about this one:
    public int getUpdateCount() throws SQLException {
        return -1;
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
        return ResultSet.CONCUR_READ_ONLY; // TODO : May impact updates later!
    }

    public int getResultSetType() throws SQLException {
        return 0;
    }

    public Connection getConnection() throws SQLException {
        return sfConnection;
    }

    // SQLFeatureNotSupportedException from here down...

    public void addBatch(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void clearBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public int[] executeBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }


    public boolean getMoreResults(int current) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
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
