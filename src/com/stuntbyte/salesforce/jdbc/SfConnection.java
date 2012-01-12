package com.stuntbyte.salesforce.jdbc;

import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.misc.LoginHelper;

import java.sql.*;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 */
public class SfConnection implements java.sql.Connection {

    private boolean closed = true;

    private String server;
    private String username;

    private LoginHelper helper;
    private ResultSetFactory metaDataFactory;
    private Properties info;

    public ResultSetFactory getMetaDataFactory() {
        return metaDataFactory;
    }

    public SfConnection(String server, String username, String password, Properties info) {

        String key = null;
        if (info.containsKey("licence")) {
            key = info.getProperty("licence");
        }

        if (password.startsWith("licence(")) {
            int sfdcPos = password.indexOf("sfdc(");
            if (sfdcPos == -1) {
                throw new RuntimeException("Password starts with licence( but does not contain sfdc(");
            }
            key = password.substring(8, password.indexOf(")"));
            password = password.substring(sfdcPos + 5, password.lastIndexOf(")"));
        }


        this.server = server;
        this.username = username;
        this.info = info;
        helper = new LoginHelper(server, username, password, key);

        if (key == null) {
            throw new RuntimeException("No licence information found");
        }


        try {
            metaDataFactory = helper.createResultSetFactory(info);
            closed = false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public java.sql.Statement createStatement() throws SQLException {
        return new SfStatement(this, helper);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new SfPreparedStatement(this, helper, sql);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return null;
    }

    public String nativeSQL(String sql) throws SQLException {
        return sql;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {

    }

    public boolean getAutoCommit() throws SQLException {
        return false;
    }

    public void commit() throws SQLException {

    }

    public void rollback() throws SQLException {

    }

    public void close() throws SQLException {
        closed = true;

    }

    public boolean isClosed() throws SQLException {
        return closed;
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return new SfDatabaseMetaData(this, metaDataFactory);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {

    }

    public boolean isReadOnly() throws SQLException {
        return false;
    }

    public void setCatalog(String catalog) throws SQLException {

    }

    public String getCatalog() throws SQLException {
        return null;
    }

    public void setTransactionIsolation(int level) throws SQLException {

    }

    public int getTransactionIsolation() throws SQLException {
        return Connection.TRANSACTION_NONE;
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void clearWarnings() throws SQLException {

    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return createStatement();
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setHoldability(int holdability) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public int getHoldability() throws SQLException {
        return 0;
    }

    public Savepoint setSavepoint() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return createStatement();
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Clob createClob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Blob createBlob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public NClob createNClob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public SQLXML createSQLXML() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isValid(int timeout) throws SQLException {
        return true;
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        info.setProperty(name, value);
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        info = properties;
    }

    public String getClientInfo(String name) throws SQLException {
        return info.getProperty(name);
    }

    public Properties getClientInfo() throws SQLException {
        return info;
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setSchema(String s) throws SQLException {
    }

    public String getSchema() throws SQLException {
        return null;
    }

    public void abort(Executor executor) throws SQLException {
    }

    public void setNetworkTimeout(Executor executor, int i) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public int getNetworkTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public String getServer() {
        return server;
    }

    public String getUsername() {
        return username;
    }

    public LoginHelper getHelper() {
        return helper;
    }
}
