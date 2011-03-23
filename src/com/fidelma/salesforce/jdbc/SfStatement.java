package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalAnalyzer;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.misc.LoginHelper;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.*;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import org.omg.PortableInterceptor.INACTIVE;
import org.omg.PortableInterceptor.Interceptor;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class SfStatement implements java.sql.Statement {

    private SfConnection sfConnection;
    private PartnerConnection pc;

    public SfStatement(SfConnection sfConnection, LoginHelper helper) throws ConnectionException, SQLException {
        this.sfConnection = sfConnection;
        pc = helper.getPartnerConnection();


//        mc = helper.getMetadataConnection();
//        helper.getMetadataConnection();
    }


    public ResultSet executeQuery(String sql) throws SQLException {
        try {
            QueryResult qr = pc.query(sql);
            return new SfResultSet(qr);

        } catch (ConnectionException e) {
            throw new SQLException(e);
        }
    }

    // TODO: See TYPE_INFO_DATA in ResultSetFactory for all the ones we need to cover
    public static Object dataTypeConvert(String value, Integer dataType) {

        if (dataType == null) {
            return value;
        }
        if (dataType == Types.INTEGER) {
            return Integer.parseInt(value);
        }
        if (dataType == Types.DOUBLE) {
            return Double.parseDouble(value);
        }
        if (dataType == Types.BOOLEAN) {
            return Boolean.parseBoolean(value);
        }

        if (dataType == Types.DECIMAL) {
            return new BigDecimal(value);
        }

        //TODO: DATE, TIME, TIMESTAMP, ARRAY, OTHER, VARBINARY

        return value;

    }

    public int executeUpdate(String sql) throws SQLException {
        try {
            LexicalAnalyzer al = new LexicalAnalyzer(new ByteArrayInputStream(sql.getBytes()), System.out);
            LexicalToken token = al.getToken();

            int count = 0;
            if (token.getValue().equalsIgnoreCase("UPDATE")) {
                count = processUpdate(al);

            } else if (token.getValue().equalsIgnoreCase("INSERT")) {
                count = processInsert(al);
            } else if (token.getValue().equalsIgnoreCase("COMMIT")) {
            } else {
                throw new SQLException("Unsupported command " + token.getValue());
            }

            return count;
        } catch (RuntimeException e) {
            throw new SQLException(e);
        }
    }

    private int processInsert(LexicalAnalyzer al) throws SQLException {
        try {
            LexicalToken token;
            read(al, "INTO");
            String table = readIf(al, LexicalToken.Type.IDENTIFIER).getValue();

            token = al.getToken("(");
            token = al.getToken(LexicalToken.Type.IDENTIFIER);

            List<String> columns = new ArrayList<String>();
            while (token != null) {
                String column = token.getValue();
                columns.add(column);
                System.out.println("ADDED COLUMN " + column);

                // Comma or )
                token = al.getToken();
                if (token.getValue().equals(")")) {
                    break;
                } else if (token.getValue().equals(",")) {
                    token = al.getToken();
                    continue;
                } else {
                    throw new SQLException("Unexpected token " + token.getValue());
                }
            }

            read(al, "values");
            token = al.getToken("(");
            token = al.getToken();

            List<String> values = new ArrayList<String>();
            while (token != null) {
                String value = token.getValue();
                values.add(value);

                // Comma or )
                token = al.getToken(LexicalToken.Type.PUNCTUATION);
                if (token.getValue().equals(")")) {
                    break;
                } else if (token.getValue().equals(",")) {
                    token = al.getToken();
                    continue;
                } else {
                    throw new SQLException("Unexpected token " + token.getValue());
                }
            }

            if (columns.size() != values.size()) {
                throw new SQLException("Number of columns does not match number of values ");
            }

            SObject sObject = new SObject();
            sObject.setType(table);

            Table tableData = sfConnection.getMetaDataFactory().getTable(table);

            int i = 0;
            for (String key : columns) {
                String val = values.get(i++);
                Integer dataType = sfConnection.getMetaDataFactory().lookupJdbcType(tableData.getColumn(key).getType());
                Object value = dataTypeConvert(val, dataType);

                sObject.setField(key, value);
            }

            SaveResult[] sr = pc.create(new SObject[]{sObject}); // TODO: Handle errors
            for (SaveResult saveResult : sr) {
                System.out.println("INSERT OK=" + saveResult.isSuccess());
                if (!saveResult.isSuccess()) {
                    Error[] errors = saveResult.getErrors();
                    for (Error error : errors) {
                        System.out.println("ERROR: " + error.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            throw new SQLException(e);
        }

        return 1;
    }

    private int processUpdate(LexicalAnalyzer al) throws SQLException {
        LexicalToken token;
        int count;
        String table = readIf(al, LexicalToken.Type.IDENTIFIER).getValue();

        Map<String, Integer> columnToDatatype = new HashMap<String, Integer>();
        ResultSet columnsRs = sfConnection.getMetaData().getColumns(null, null, table, null);
        while (columnsRs.next()) {
            String column = columnsRs.getString("COLUMN_NAME").toUpperCase();
            int dataType = columnsRs.getInt("DATA_TYPE");
            columnToDatatype.put(column, dataType);
        }
        read(al, "SET");
        token = al.getToken();
        String whereClause = "";

        Map<String, Object> values = new HashMap<String, Object>();

        while (token != null) {
            String column = token.getValue();
            read(al, "=");
            LexicalToken value = al.getToken();

            if (!column.equalsIgnoreCase("Id")) {

                Integer dataType = columnToDatatype.get(column.toUpperCase());
                assert dataType != null;
                System.out.println("Data type is " + dataType + " for " + column);
                values.put(column.toUpperCase(), value.getValue());
            }

            token = al.getToken();  // comma, or WHERE or null (end of statement)
            if (token != null) {
                if (token.getValue().equalsIgnoreCase("WHERE")) {
                    while (token != null) {
                        if (token.getType().equals(LexicalToken.Type.STRING)) {
                            whereClause += "'";
                        }
                        System.out.println(token.getType() + " " + token.getValue());
                        whereClause += token.getValue();
                        if (token.getType().equals(LexicalToken.Type.STRING)) {
                            whereClause += "'";
                        }

                        whereClause += " ";
                        token = al.getToken();
                    }
                    break;
                } else if (token.getValue().equalsIgnoreCase(",")) {
                    token = al.getToken();
                } else {
                    throw new SQLException("Expected WHERE or COMMA");
                }
            }
        }

        QueryResult qr;
        try {
            StringBuilder readSoql = new StringBuilder();
            readSoql.append("select Id ");
            readSoql.append(" from ").append(table).append(" ").append(whereClause);

            qr = pc.query(readSoql.toString());

            SObject[] sObjects = qr.getRecords();
            SObject[] update = new SObject[sObjects.length];

            for (int i = 0; i < sObjects.length; i++) {
                SObject sObject = new SObject();
                update[i] = sObject;
                sObject.setType(table);
                sObject.setId(sObjects[i].getId());

                for (String key : values.keySet()) {

                    Integer datatype = columnToDatatype.get(key.toUpperCase());
                    Object value = dataTypeConvert((String) values.get(key), datatype);

                    sObject.setField(key, value);
                }
            }

            SaveResult[] sr = pc.update(update); // TODO: Handle errors
            for (SaveResult saveResult : sr) {
                System.out.println("UPDATE OK=" + saveResult.isSuccess());
                if (!saveResult.isSuccess()) {
                    Error[] errors = saveResult.getErrors();
                    for (Error error : errors) {
                        System.out.println("ERROR: " + error.getMessage());
                    }
                }
            }
            count = qr.getSize();

        } catch (ConnectionException e) {
            throw new SQLException(e);
        }
        return count;
    }

    private LexicalToken readIf(LexicalAnalyzer al, LexicalToken.Type type) throws SQLException {
        LexicalToken token = al.getToken();
        if (token == null) {
            throw new SQLException("SOQL Command ended unexpected");
        }
        if (!token.getType().equals(type)) {
            throw new SQLException("Expected " + type.name());
        }
        return token;
    }

    private void read(LexicalAnalyzer al, String expected) throws SQLException {
        LexicalToken token = al.getToken();
        if (token == null) {
            throw new SQLException("SOQL Command ended unexpected");
        }
        if (!token.getValue().equalsIgnoreCase(expected)) {
            throw new SQLException("Expected " + expected + " got " + token);
        }
    }


    public void close() throws SQLException {

    }

    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    public void setMaxFieldSize(int max) throws SQLException {

    }

    public int getMaxRows() throws SQLException {
        return 0;
    }

    public void setMaxRows(int max) throws SQLException {

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

    public void setFetchSize(int rows) throws SQLException {

    }

    public int getFetchSize() throws SQLException {
        return 200;
    }

    public int getResultSetConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY; // TODO : May impact updates later!
    }

    public int getResultSetType() throws SQLException {
        return 0;
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

    public Connection getConnection() throws SQLException {
        return sfConnection;
    }

    public boolean getMoreResults(int current) throws SQLException {
        return false;
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
