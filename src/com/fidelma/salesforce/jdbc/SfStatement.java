package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.dml.Insert;
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
import java.util.HashSet;
import java.util.Set;

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
            Set<String> columnsInSql = extractColumnsFromSoql(sql);
            System.out.println("SOQL: " + sql + " found " + columnsInSql.size());
            for (String s : columnsInSql) {
                System.out.println("COL IN SQL: " +s);
            }

            String upper = sql.toUpperCase();
            if (upper.contains("COUNT(*)")) {
                sql = patchCountStar(sql);
            }

            QueryResult qr = pc.query(sql);
            return new SfResultSet(qr, columnsInSql);

        } catch (ConnectionException e) {
            throw new SQLException(e);
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private Set<String> extractColumnsFromSoql(String sql) throws Exception {
        Set<String> result = new HashSet<String>();

        SimpleParser la = new SimpleParser(sql);
        la.getToken("SELECT");
        LexicalToken token = la.getToken();
        System.out.println("SELECT TOKEN=" + token);

        while ((token != null) && (!token.getValue().equalsIgnoreCase("from") )) {
            result.add(token.getValue().toUpperCase());
            token = la.getToken();
        }
        return result;
    }

    // Convert the SQL-normal "count(*)" to Salesforce's "count()"
    private int detectCountStar(String sql, int start) {
        int countPos = sql.indexOf("count(*)", start);
        if (countPos == -1) {
            countPos = sql.indexOf("COUNT(*)", start);
        }
        return countPos;

    }

    private String patchCountStar(String sql) {
        StringBuilder fixed = new StringBuilder();

        int start = 0;
        int countPos = detectCountStar(sql, start);
        while (countPos != -1) {
            System.out.println("FROM " + start + " TO " + countPos);

            fixed.append(sql.substring(start, countPos));
            fixed.append("count()");
            start = countPos + 8;
            countPos = detectCountStar(sql, start);
        }
        fixed.append(sql.substring(start));
        System.out.println(fixed.toString());
        return fixed.toString();
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
            } else if (token.getValue().equalsIgnoreCase("COMMIT")) {
            } else {
                throw new SQLException("Unsupported command " + token.getValue());
            }

            return count;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }




//    private void read(LexicalAnalyzer al, String expected) throws SQLException {
//        LexicalToken token = al.getToken();
//        if (token == null) {
//            throw new SQLException("SOQL Command ended unexpected");
//        }
//        if (!token.getValue().equalsIgnoreCase(expected)) {
//            throw new SQLException("Expected " + expected + " got " + token);
//        }
//    }


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
