package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.misc.LoginHelper;
import com.fidelma.salesforce.parse.SimpleParser;
import com.fidelma.salesforce.misc.TypeHelper;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class SfPreparedStatement extends SfStatement implements PreparedStatement {
    private List<String> tokenizedSoql = new ArrayList<String>();
    private Map<Integer, Integer> paramMap = new HashMap<Integer, Integer>();
    private boolean oldTypeCount;


    public SfPreparedStatement(SfConnection sfConnection, LoginHelper helper, String sql) throws
            Exception {
        super(sfConnection, helper);

        if (sql.toUpperCase().contains("COUNT()")) {
            oldTypeCount = true;
        }
        SimpleParser al = new SimpleParser(sql);
        LexicalToken token = al.getToken();

        int paramCount = 0;
        int paramPointer = 0;
        while (token != null) {
            if (token.getType().equals(LexicalToken.Type.STRING)) {
                tokenizedSoql.add("'" + token.getValue() + "'");
            } else {
                tokenizedSoql.add(token.getValue());
            }
            if (token.getValue().equals("?")) {
                paramMap.put(++paramCount, paramPointer);
            }

            paramPointer++;
            token = al.getToken();
        }
    }


    public ResultSet executeQuery() throws SQLException {
        checkParametersSet();
        return super.executeQuery(assembleSoql());
    }

    public int executeUpdate() throws SQLException {
        checkParametersSet();
        return super.executeUpdate(assembleSoql());
    }

    private void checkParametersSet() throws SQLException {
        Set<Integer> keys = paramMap.keySet();
        for (Integer parameterIndex : keys) {
            Integer soqlIndex = paramMap.get(parameterIndex);
            String param = tokenizedSoql.get(soqlIndex);
            if (param.equals("?")) {
                throw new SQLException("Parameter " + parameterIndex + " not set");
            }
        }
    }

    private String assembleSoql() {
        StringBuilder soql = new StringBuilder();
        int ptr = 0;

        while (ptr < tokenizedSoql.size()) {
            String s = tokenizedSoql.get(ptr);
            if (oldTypeCount && s.equalsIgnoreCase("COUNT")) {
                soql.append("count() ");
                ptr = ptr + 2; // skip the tokenised brackets
            } else {
                soql.append(s).append(" ");
            }
            ptr++;
        }

        return soql.toString();
    }


    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        setParameter(parameterIndex, "''");
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        setParameter(parameterIndex, Boolean.toString(x));
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {

    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        setParameter(parameterIndex, Short.toString(x));
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        setParameter(parameterIndex, Integer.toString(x));
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        setParameter(parameterIndex, Long.toString(x));
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        setParameter(parameterIndex, Float.toString(x));
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        setParameter(parameterIndex, Double.toString(x));
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        setParameter(parameterIndex, x.toPlainString());
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        if (x != null) {
            setParameter(parameterIndex, "'" + x.replace("'", "\\'") + "'");
        } else {
            setParameter(parameterIndex, null);
        }

    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        // TODO!

    }


    private SimpleDateFormat timestampSdf = new SimpleDateFormat(TypeHelper.timestampFormat);
    private SimpleDateFormat dateSdf = new SimpleDateFormat(TypeHelper.dateFormat);


    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        Date d = new Date(x.getTime());
        setParameter(parameterIndex, timestampSdf.format(d));
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        Date d = new Date(x.getTime());
        setParameter(parameterIndex, dateSdf.format(d));
    }


    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        // TODO
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        // TODO: TEST ALL THESE!
        if (x == null) {
           setParameter(parameterIndex, null);

        } else if (x instanceof BigDecimal) {
            setBigDecimal(parameterIndex, (BigDecimal) x);

        } else if (x instanceof Boolean) {
            setBoolean(parameterIndex, (Boolean) x);

        } else if (x instanceof Date) {
            setDate(parameterIndex, (Date) x);

        } else if (x instanceof Double) {
            setDouble(parameterIndex, (Double) x);

        } else if (x instanceof Float) {
            setFloat(parameterIndex, (Float) x);

        } else if (x instanceof Integer) {
            setInt(parameterIndex, (Integer) x);

        } else if (x instanceof Long) {
            setLong(parameterIndex, (Long) x);

        } else if (x instanceof Short) {
            setShort(parameterIndex, (Short) x);

        } else if (x instanceof String) {
            setString(parameterIndex, (String) x);

        } else if (x instanceof Time) {
            setTime(parameterIndex, (Time) x);

        } else if (x instanceof Timestamp) {
            setTimestamp(parameterIndex, (Timestamp) x);

        }
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {

    }

    private void setParameter(int parameterIndex, String x) throws SQLException {
        Integer soqlIndex = paramMap.get(parameterIndex);
        if (soqlIndex == null) {
            throw new SQLException("Parameter " + parameterIndex + " does not exist");
        }
        tokenizedSoql.set(soqlIndex, x);
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {

    }


    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    public void clearParameters() throws SQLException {

    }


    public boolean execute() throws SQLException {
        if (isBatchMode()) {
            executeBatch();
            return true;
        }
        if (this.tokenizedSoql.size() > 0) {
            if (tokenizedSoql.get(0).toUpperCase().startsWith("SELECT")) {
                executeQuery();
            } else {
                executeUpdate();
            }
            return true;
        }
        return false;
    }

    public void addBatch() throws SQLException {
        // TODO: Handle batched SELECTs?
        System.out.println("KJS --> " + assembleSoql());
        executeUpdate(assembleSoql(), true);
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {

    }

    public void setRef(int parameterIndex, Ref x) throws SQLException {

    }

    public void setBlob(int parameterIndex, Blob x) throws SQLException {

    }

    public void setClob(int parameterIndex, Clob x) throws SQLException {

    }

    public void setArray(int parameterIndex, Array x) throws SQLException {

    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return null; // Cannot be implemented
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {

    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {

    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {

    }

    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {

    }

    public void setURL(int parameterIndex, URL x) throws SQLException {

    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new SQLFeatureNotSupportedException();
//        return null;
    }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {

    }

    public void setNString(int parameterIndex, String value) throws SQLException {

    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {

    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {

    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {

    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {

    }


    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {

    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {

    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {

    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {

    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {

    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {

    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {

    }

}
