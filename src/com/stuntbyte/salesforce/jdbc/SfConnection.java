package com.stuntbyte.salesforce.jdbc;

import com.stuntbyte.salesforce.core.metadata.MetadataService;
import com.stuntbyte.salesforce.core.metadata.MetadataServiceImpl;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.misc.LicenceResult;
import com.stuntbyte.salesforce.misc.LoginHelper;
import com.stuntbyte.salesforce.misc.Reconnector;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.*;
import java.sql.Statement;
import java.util.*;

/**
 */
public class SfConnection implements java.sql.Connection {

    private boolean closed = true;

    private String server;
    private String username;

    private LoginHelper helper;
    private ResultSetFactory metaDataFactory;
    private MetadataService metadataService;
    private Properties info;

    public ResultSetFactory getMetaDataFactory() {
        return metaDataFactory;
    }

    public SfConnection(String server, String username, String password, Properties info) throws SQLException {

        String licenceKey = null;
        if (info.containsKey("licence")) {
            licenceKey = info.getProperty("licence");
        }

        if (password.startsWith("licence(")) {
            int sfdcPos = password.indexOf("sfdc(");
            if (sfdcPos == -1) {
                throw new RuntimeException("Password starts with licence( but does not contain sfdc(");
            }
            licenceKey = password.substring(8, password.indexOf(")"));
            password = password.substring(sfdcPos + 5, password.lastIndexOf(")"));
        }

        server = pushUrlParametersIntoInfo(server, info);

        this.server = server;
        this.username = username;
        this.info = info;
        helper = new LoginHelper(server, username, password, licenceKey);

        if (licenceKey == null) {
            throw new SQLException("No licence information found");
        }

        try {
            metaDataFactory = helper.createResultSetFactory(info);
            metadataService = new MetadataServiceImpl(new Reconnector(helper));
            closed = false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String pushUrlParametersIntoInfo(String server, Properties info) throws SQLException {
        try {
            URL serverUrl = new URL(server);
            server = server.replace(serverUrl.getFile(), "");

            Map<String, String> params = extractParameters(serverUrl);

            for (String param : params.keySet()) {
                info.setProperty(param, params.get(param));
            }

        } catch (Exception e) {
            throw new SQLException(e);
        }
        return server;
    }

    private Map<String, String> extractParameters(URL serverUrl) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<String, String>();
        String query = serverUrl.getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                String paramName = URLDecoder.decode(pair[0], "UTF-8");
                String value = "";
                if (pair.length > 1) {
                    value = URLDecoder.decode(pair[1], "UTF-8");
                }
                params.put(paramName, value);
            }
        }
        return params;
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
        return new SfDatabaseMetaData(this, metaDataFactory, metadataService);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {

    }

    public boolean isReadOnly() throws SQLException {
        return false;
    }

    public void setCatalog(String catalog) throws SQLException {

    }

    public String getCatalog() throws SQLException {
        return ResultSetFactory.catalogName;
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

    /*
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
    */

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
