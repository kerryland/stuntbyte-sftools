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
package com.stuntbyte.salesforce.jdbc;

import com.stuntbyte.salesforce.core.metadata.MetadataService;
import com.stuntbyte.salesforce.core.metadata.MetadataServiceImpl;
import com.stuntbyte.salesforce.jdbc.metaforce.Column;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.jdbc.metaforce.Table;
import com.stuntbyte.salesforce.misc.LoginHelper;
import com.stuntbyte.salesforce.misc.Reconnector;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.*;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.Executor;

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
    private boolean readOnly;

    public ResultSetFactory getMetaDataFactory() {
        return metaDataFactory;
    }

    public SfConnection(String server, String username, String password, Properties info) throws SQLException {

        server = pushUrlParametersIntoInfo(server, info);

        this.server = server;
        this.username = username;
        this.info = info;
        helper = new LoginHelper(server, username, password);  // TODO: Put version into "info"

        try {
            helper.authenticate();

            metaDataFactory = helper.createResultSetFactory(info, true);
            metadataService = new MetadataServiceImpl(new Reconnector(helper));

            if (info == null || "true".equals(info.getProperty("deployable"))) {
                populateWithDeployableTables(metaDataFactory, metadataService);
            }
            closed = false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void populateWithDeployableTables(ResultSetFactory metaDataFactory, MetadataService metadataService) {
        for (String deployable : metadataService.getMetadataTypes()) {
            Table table = new Table(deployable, "", "TABLE");
            table.setSchema(ResultSetFactory.DEPLOYABLE);
            table.addColumn(new Column("Identifier", metaDataFactory.getType("String")));
            table.addColumn(new Column("Name", metaDataFactory.getType("String")));
            table.addColumn(new Column("LastChangedBy", metaDataFactory.getType("String")));

            metaDataFactory.addTable(table);
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
        throw new SQLFeatureNotSupportedException();
    }

    public String nativeSQL(String sql) throws SQLException {
        return sql;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {

    }

    public boolean getAutoCommit() throws SQLException {
        return true;
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
        this.readOnly = readOnly;
    }

    public boolean isReadOnly() throws SQLException {
        return readOnly;
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
        return null; // Null means no warning
    }

    public void clearWarnings() throws SQLException {

    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        checkProperties(resultSetType, resultSetConcurrency);
        return createStatement();
    }

    private void checkProperties(int resultSetType, int resultSetConcurrency) throws SQLFeatureNotSupportedException {
        if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) {
            throw new SQLFeatureNotSupportedException("Only support TYPE_FORWARD_ONLY");
        }

        // TODO: Support updatable
        if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
            throw new SQLFeatureNotSupportedException("Only support CONCUR_READ_ONLY");
        }
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        checkProperties(resultSetType, resultSetConcurrency);
        return prepareStatement(sql);

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
        checkProperties(resultSetType, resultSetConcurrency);
        return createStatement();
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        checkProperties(resultSetType, resultSetConcurrency);
        return prepareStatement(sql);
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

    public void setSchema(String schema) throws SQLException {
    }

    public String getSchema() throws SQLException {
        return null;
    }

    public void abort(Executor executor) throws SQLException {
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    }

    public int getNetworkTimeout() throws SQLException {
        return 0;
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
