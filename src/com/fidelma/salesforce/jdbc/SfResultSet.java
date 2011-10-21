package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.misc.Reconnector;
import com.fidelma.salesforce.parse.ParsedColumn;
import com.fidelma.salesforce.misc.TypeHelper;
import com.fidelma.salesforce.parse.ParsedSelect;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.bind.XmlObject;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 */

// http://wiki.developerforce.com/index.php/Introduction_to_the_Force.com_Web_Services_Connector
public class SfResultSet implements java.sql.ResultSet {
    private SObject[] records;
    private Reconnector reconnector;
    private QueryResult qr;
    private int maxRows;
    private List<String> columnsInResult;
    private Map<String, String> columnNameCaseMap = new HashMap<String, String>();

    private int ptr = -1;
    private int rowCount = 0;
    private int batchEnd = 0;
    private int batchSize = 200;
    private SfResultSetMetaData metaData;
    private boolean wasNull;
    private SfStatement statement;
    private boolean closed;

    private SimpleDateFormat timestampSdf = new SimpleDateFormat(TypeHelper.timestampFormat);
    private SimpleDateFormat dateSdf = new SimpleDateFormat(TypeHelper.dateFormat);

    public SfResultSet() {
        // Create an empty result set
        metaData = new SfResultSetMetaData();
        rowCount = 2;
        maxRows = 1;
    }


    public SfResultSet(SfStatement statement,
                       Reconnector reconnector,
                       QueryResult qr,
                       List<ParsedSelect> parsedSelects) throws SQLException {

        this.statement = statement;
        SfConnection conn = (SfConnection) statement.getConnection();
        ResultSetFactory rsf = (conn).getMetaDataFactory();
        boolean useLabels = Boolean.parseBoolean(conn.getClientInfo("useLabels"));
        try {
            if (reconnector.getQueryOptions() != null) {
                batchSize = reconnector.getQueryOptions().getBatchSize();
            }
        } catch (ConnectionException e) {
            throw new SQLException(e);
        }

        this.reconnector = reconnector;
        this.qr = qr;
        this.maxRows = statement.getMaxRows();

        records = qr.getRecords();

        columnsInResult = new ArrayList<String>();

        if (records.length > 0) {
            configureDateTimeObjects();

            generateResultFields(null, records[0], columnsInResult);

            String drivingTable = null;
            List<ParsedColumn> cols = new ArrayList<ParsedColumn>();
            for (ParsedSelect parsedSelect : parsedSelects) {
                cols.addAll(parsedSelect.getColumns());
                drivingTable = parsedSelect.getDrivingTable();
            }
            finaliseColumnList(columnsInResult, cols);

            metaData = new SfResultSetMetaData(drivingTable, rsf, records[0], columnsInResult, useLabels);
        } else {
            metaData = new SfResultSetMetaData();
        }

        batchEnd = records.length - 1;
    }

    private void configureDateTimeObjects() {
        timestampSdf = new SimpleDateFormat(TypeHelper.timestampFormat);
        timestampSdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        dateSdf = new SimpleDateFormat(TypeHelper.dateFormat);
        dateSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    public boolean next() throws SQLException {
        try {
            if ((maxRows > 0) && (rowCount >= maxRows)) {
                return false;
            }

            if (ptr < batchEnd) {
                rowCount++;
                return true;
            }

            if (!qr.isDone()) {
                qr = reconnector.queryMore(qr.getQueryLocator());
                records = qr.getRecords();
                batchEnd = records.length - 1;
                ptr = -1;
                return true;
            }
            return false;

        } catch (ConnectionException e) {
            throw new SQLException(e);

        } finally {
            ptr++;
        }
    }

    private void generateResultFields(String parentName, XmlObject parent, List<String> columnsInResult) throws SQLException {
        if (parent.hasChildren()) {
            Iterator<XmlObject> children = parent.getChildren();

            parentName = calculateParentName(parentName, parent);

            int childPos = 0;
            while (children.hasNext()) {
                XmlObject child = children.next();

                // Always skip the first two columns. They are hard-coded to be "Type" and "Id".
                if (++childPos > 2) {
                    generateResultFields(parentName, child, columnsInResult);
                }
            }
        } else {

            String columnName;
            if (parentName != null) {
                columnName = parentName + "." + parent.getName().getLocalPart();
            } else {
                columnName = parent.getName().getLocalPart();
            }

            if (!columnsInResult.contains(columnName)) {
                columnsInResult.add(columnName);
            }
        }
    }

    private String calculateParentName(String parentName, XmlObject parent) {
        if (parentName != null) {
            parentName += "." + parent.getName().getLocalPart();
        } else {
            parentName = parent.getName().getLocalPart();
        }

        if (parentName != null) {
            if (parentName.equals("records")) {
                parentName = null;
            } else if (parentName.endsWith(".records")) {
                parentName = parentName.substring(0, parentName.length() - ".records".length());
            }
        }
        return parentName;
    }


    private void finaliseColumnList(List<String> columnsInResult, List<ParsedColumn> columnsInSql) {
        List<String> newColumnsInResult = new ArrayList<String>(columnsInSql.size());

        Set<Integer> done = new HashSet<Integer>();
        for (int i = 0; i < columnsInSql.size(); i++) {
            newColumnsInResult.add(null);
            done.add(i);
        }


        // Salesforce doesn't always return columns in the same order that we requested
        // them, so we have to trawl through the results and try to figure out how
        // to match them up.

        // First pass -- match exact column names, and match "function" SQL to expression results
        for (int i = 0; i < columnsInSql.size(); i++) {
            ParsedColumn inSQl = columnsInSql.get(i);

            for (int j = 0; j < columnsInResult.size(); j++) {
                String inResult = columnsInResult.get(j);
                if ((inSQl != null) && (inResult != null)) {
                    if ((inSQl.getName().equalsIgnoreCase(inResult)) ||
                            (inSQl.isFunction() && inResult.toUpperCase().startsWith("EXPR"))) {

                        if (columnsInSql.get(i).getAliasName() != null) {
                            columnNameCaseMap.put(columnsInSql.get(i).getAliasName().toUpperCase(), columnsInResult.get(j));
                        } else {
                            columnNameCaseMap.put(columnsInSql.get(i).getName().toUpperCase(), columnsInResult.get(j));
                        }

                        newColumnsInResult.set(i, columnsInResult.get(j));
                        columnsInResult.set(j, null);
                        columnsInSql.set(i, null);
                        done.remove(i);
                        break;
                    }
                }
            }
        }

        // Second pass -- match any left over columns that end with what we're expecting
        // (sometimes Salesforce doesn't bother to give us the relationship table names
        // used in our query)
        if (!done.isEmpty()) {
            for (int i = 0; i < columnsInSql.size(); i++) {
                if (newColumnsInResult.get(i) == null) {
                    for (int j = 0; j < columnsInResult.size(); j++) {
                        if ((columnsInResult.get(j) != null) &&
                                (columnsInSql.get(i).getName().toUpperCase().endsWith(
                                        "." + columnsInResult.get(j).toUpperCase()))) {

                            if (columnsInSql.get(i).getAliasName() != null) {
                                columnNameCaseMap.put(columnsInSql.get(i).getAliasName().toUpperCase(), columnsInResult.get(j));
                            } else {
                                columnNameCaseMap.put(columnsInSql.get(i).getName().toUpperCase(), columnsInResult.get(j));
                            }

                            newColumnsInResult.set(i, columnsInSql.get(i).getName());
                            columnsInResult.set(j, null);
                            columnsInSql.set(i, null);
                            done.remove(i);
                            break;
                        }
                    }
                }
            }
        }

        // Third pass -- invent columns that Salesforce hasn't even bothered to
        // return at all. It does this sometimes for null values.
        if (!done.isEmpty()) {
            for (int i = 0; i < columnsInSql.size(); i++) {
                if (newColumnsInResult.get(i) == null) {
                    newColumnsInResult.set(i, columnsInSql.get(i).getName());

                    if (columnsInSql.get(i).getAliasName() != null) {
                        columnNameCaseMap.put(columnsInSql.get(i).getAliasName().toUpperCase(), columnsInSql.get(i).getName());
                    } else {
                        columnNameCaseMap.put(columnsInSql.get(i).getName().toUpperCase(), columnsInSql.get(i).getName());
                    }

                    done.remove(i);
                }
            }
        }

        assert done.isEmpty();

        columnsInResult.clear();
        columnsInResult.addAll(newColumnsInResult);

    }

    private class FullName {
        private StringBuilder name = new StringBuilder();

        void append(String chunk) {
            name = name.append(chunk);
        }

        @Override
        public String toString() {
            return name.toString();
        }
    }

    private Object drillToChild(XmlObject parent, String columnLabel,
                                Map<String, String> columnNameCaseMap,
                                String fullColumnName,
                                FullName correctedFullColumnName) throws SQLException {
        Object result;

        int dotPos = columnLabel.indexOf(".");
        if (dotPos != -1) {
            if (!parent.hasChildren()) {
                throw new SQLException(columnLabel + " does not have children");
            }
            String parentName = columnLabel.substring(0, dotPos);

            XmlObject child = (XmlObject) parent.getField(parentName);
            if (child == null) {
                // Maybe we couldn't find the column because we've got the
                // case wrong -- try to find it the slow way...
                Iterator it = parent.getChildren();
                while (it.hasNext()) {
                    XmlObject next = (XmlObject) it.next();

                    if (next.getName().getLocalPart().equalsIgnoreCase(parentName)) {
                        correctedFullColumnName.append(next.getName().getLocalPart());
                        child = (XmlObject) parent.getField(next.getName().getLocalPart());
                    }
                }
                if (child == null) {
                    return null;
                }
            } else {
                correctedFullColumnName.append(parentName);
            }

            correctedFullColumnName.append(".");

            String childLabel = columnLabel.substring(dotPos + 1);
            result = drillToChild(child, childLabel, columnNameCaseMap, fullColumnName, correctedFullColumnName);

        } else {
            result = parent.getField(columnLabel);
            if (result != null) {
                correctedFullColumnName.append(columnLabel);
            } else {
                // We couldn't find the column directly, but maybe it's
                // just a problem with the case of the column name.
                Iterator it = parent.getChildren();
                while (it.hasNext()) {
                    XmlObject next = (XmlObject) it.next();

                    if (next.getName().getLocalPart().equalsIgnoreCase(columnLabel)) {
                        correctedFullColumnName.append(next.getName().getLocalPart());

                        if (correctedFullColumnName.toString().equalsIgnoreCase(fullColumnName)) {
                            // Store the corrected column name so we don't have to do this
                            // iteration again.
                            columnNameCaseMap.put(fullColumnName.toUpperCase(), correctedFullColumnName.toString());
                        }
                        result = parent.getField(next.getName().getLocalPart());
                    }
                }
            }
        }
        return result;
    }

    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return getCharacterStreamFromString(getString(columnIndex));
    }

    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return getCharacterStreamFromString(getString(columnLabel));

    }

    private Reader getCharacterStreamFromString(String value) {
        if (value == null) {
            value = "";
        }
        return new StringReader(value);
    }


    public String getString(String columnName) throws SQLException {
        return (String) getObject(columnName);
    }

    public String getString(int columnIndex) throws SQLException {
        Object obj = null;
        try {
            obj = getObject(columnIndex);
            return (String) obj;
        } catch (ClassCastException e) {
            throw new SQLException("Tried to cast " + obj + " " + columnsInResult + " index: " + columnIndex + " to String");

        }
    }

    // What about using "expr0" or "expr1"?
    public Object getObject(int columnIndex) throws SQLException {
        return getObject(findLabelFromIndex(columnIndex));
    }

    public Object getObject(String columnLabel) throws SQLException {
        Object result;

        if (ptr == -1) {
            throw new SQLException("No data available -- next not called");
        }

        String realColumnName = columnNameCaseMap.get(columnLabel.toUpperCase());
//        System.out.println("Mapped " + columnLabel + " to " + realColumnName);

        if (realColumnName == null) {
            wasNull = true;
            result = null;  // ??
//            System.out.println("Don't know about column " + columnLabel);
//            throw new SQLException("Don't know about column " + columnLabel);
        } else {
            SObject obj = records[ptr];
//            System.out.println("DRILLING for " + columnLabel + " with REALCOLUMNANME " + realColumnName);
            result = drillToChild(obj, realColumnName, columnNameCaseMap, realColumnName, new FullName());
        }
        wasNull = (result == null);
        return result;
    }

    private String findLabelFromIndex(int columnIndex) throws SQLException {
        if (columnIndex > columnsInResult.size()) {
            throw new SQLException("Unable to identify column " + columnIndex + " there are only " + columnsInResult.size());
        }
        return columnsInResult.get(columnIndex - 1);
    }


    public void close() throws SQLException {
        closed = true;
    }

    public boolean isClosed() throws SQLException {
        return closed;
    }


    public int findColumn(String columnLabel) throws SQLException {
        int i = 0;
        int col = -1;

        for (String fieldName : columnsInResult) {
            i++;
            if (fieldName.equalsIgnoreCase(columnLabel)) {
                col = i;
                break;
            }
        }
        if (col == -1) {
            throw new SQLException("Unable to find column '" + columnLabel + "'");
        }
        return col;
    }


    public long getLong(String columnName) throws SQLException {
        return getLong(getObject(columnName));
    }

    public long getLong(int columnIndex) throws SQLException {
        return getLong(getObject(columnIndex));
    }

    private long getLong(Object o) throws SQLException {
        if (o == null) {
            return 0;
        } else if (o instanceof Number) {
            return ((Number) o).longValue();
        } else if (o instanceof String) {
            BigDecimal bd = new BigDecimal((String) o);
            return bd.toBigInteger().longValue();
        } else {
            throw new SQLException("No type conversion to long available for " + o);
        }
    }

    public int getInt(String columnName) throws SQLException {
        return getInt(getObject(columnName));
    }

    public int getInt(int columnIndex) throws SQLException {
        return getInt(getObject(columnIndex));
    }

    private int getInt(Object o) throws SQLException {
        if (o == null) {
            return 0;
        } else if (o instanceof Number) {
            return ((Number) o).intValue();
        } else if (o instanceof String) {
            BigDecimal bd = new BigDecimal((String) o);
            return bd.intValue();
        } else {
            throw new SQLException("No type conversion to int available for " + o);
        }
    }

    public short getShort(String columnName) throws SQLException {
        return getShort(getObject(columnName));
    }

    public short getShort(int columnIndex) throws SQLException {
        return getShort(getObject(columnIndex));
    }

    private short getShort(Object o) throws SQLException {
        if (o == null) {
            return 0;
        } else if (o instanceof Number) {
            return ((Number) o).shortValue();
        } else if (o instanceof String) {
            return Short.valueOf((String) o);

        } else {
            throw new SQLException("No type conversion to short available for " + o);
        }
    }

    public double getDouble(String columnLabel) throws SQLException {
        return getDouble(getObject(columnLabel));
    }

    public double getDouble(int columnIndex) throws SQLException {
        return getDouble(getObject(columnIndex));
    }

    private double getDouble(Object o) throws SQLException {
        if (o == null) {
            return 0;
        } else if (o instanceof Number) {
            return ((Number) o).doubleValue();
        } else if (o instanceof String) {
            return Double.valueOf((String) o);
        } else {
            throw new SQLException("No type conversion to int available for " + o);
        }
    }


    public float getFloat(String columnLabel) throws SQLException {
        return getFloat(getObject(columnLabel));
    }

    public float getFloat(int columnIndex) throws SQLException {
        return getFloat(getObject(columnIndex));
    }

    private float getFloat(Object o) throws SQLException {
        if (o == null) {
            return 0;
        } else if (o instanceof Number) {
            return ((Number) o).floatValue();
        } else if (o instanceof String) {
            return Float.valueOf((String) o);
        } else {
            throw new SQLException("No type conversion to int available for " + o);
        }
    }


    public boolean getBoolean(String columnLabel) throws SQLException {
        return getBoolean(getObject(columnLabel));
    }

    public boolean getBoolean(int columnIndex) throws SQLException {
        return getBoolean(getObject(columnIndex));
    }

    private boolean getBoolean(Object o) throws SQLException {
        if (o == null) {
            return false;
        } else if (o instanceof Boolean) {
            return (Boolean) o;
        } else if (o instanceof String) {
            return Boolean.valueOf((String) o);
        } else if (o instanceof Number) {
            return ((Number) o).intValue() == 1;
        } else {
            throw new SQLException("No type conversion to int available for " + o);
        }
    }


    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return getBigDecimal(getObject(columnLabel));
    }

    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        return getBigDecimal(getObject(columnIndex));
    }

    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return getBigDecimal(columnIndex).setScale(scale);
    }

    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return getBigDecimal(columnLabel).setScale(scale);
    }

    private BigDecimal getBigDecimal(Object o) throws SQLException {
        if (o == null) {
            return null;
        } else if (o instanceof BigDecimal) {
            return ((BigDecimal) o);
        } else if (o instanceof String) {
            return new BigDecimal((String) o);
        } else {
            throw new SQLException("No type conversion to int available for " + o);
        }
    }

    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        return getTimestamp(getObject(columnIndex));
    }

    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return getTimestamp(getObject(columnLabel));
    }

    // TODO: I guess -- what is cal for?
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return getTimestamp(columnIndex);
    }

    // TODO: I guess -- what is cal for?
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return getTimestamp(columnLabel);
    }


    private Timestamp getTimestamp(Object o) throws SQLException {
        if (o == null) {
            return null;
        } else if (o instanceof Timestamp) {
            return ((Timestamp) o);
        } else if (o instanceof String) {
            try {
                java.util.Date d = timestampSdf.parse((String) o);
                return new Timestamp(d.getTime());
            } catch (ParseException e) {
                throw new SQLException("No type conversion to Timestamp available for " + o);
            }
        } else {
            throw new SQLException("No type conversion to Timestamp available for " + o);
        }
    }


    public ResultSetMetaData getMetaData() throws SQLException {
        return metaData;
    }

    public Date getDate(int columnIndex) throws SQLException {
        return getDate(getObject(columnIndex));
    }

    public Date getDate(String columnLabel) throws SQLException {
        return getDate(getObject(columnLabel));
    }

    // TODO: Handle cal
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return getDate(getObject(columnIndex));
    }

    // TODO: Handle cal
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return getDate(getObject(columnLabel));
    }

    private Date getDate(Object o) throws SQLException {
        if (o == null) {
            return null;
        } else if (o instanceof java.sql.Date) {
            return (Date) o;
        } else if (o instanceof java.util.Date) {
            java.util.Date d = (java.util.Date) o;
            return new java.sql.Date(d.getTime());
        } else if (o instanceof String) {
            try {
                java.util.Date d = dateSdf.parse((String) o);
                return new Date(d.getTime());
            } catch (ParseException e) {
                throw new SQLException("No type conversion to Date available for " + o);
            }
        } else {
            throw new SQLException("No type conversion to Date available for " + o + " " + o.getClass().getName());
        }
    }


    // This is rubbish
    public Time getTime(String columnName) throws SQLException {
        return (Time) getObject(columnName);
    }

    // This is rubbish
    public Time getTime(int columnIndex) throws SQLException {
        return (Time) getObject(columnIndex);
    }


    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return null;
    }

    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return null;
    }


    public boolean wasNull() throws SQLException {
        return wasNull;
    }


    // HERE DOWN DON'T NEED IMPLEMENTING -- but should throw some exceptions ----------------------

    public Statement getStatement() throws SQLException {
        return statement;
    }


    public byte getByte(int columnIndex) throws SQLException {
        return 0;
    }

    public byte[] getBytes(int columnIndex) throws SQLException {
        return new byte[0];
    }


    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return null;
    }

    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return null;
    }

    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return null;
    }


    public byte getByte(String columnLabel) throws SQLException {
        return 0;
    }


    public byte[] getBytes(String columnLabel) throws SQLException {
        return new byte[0];
    }

    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return null;
    }

    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        return null;
    }

    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return null;
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void clearWarnings() throws SQLException {

    }

    public String getCursorName() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }


    public boolean isBeforeFirst() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isAfterLast() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isFirst() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isLast() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void beforeFirst() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void afterLast() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean first() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean last() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public int getRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean absolute(int row) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean relative(int rows) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean previous() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setFetchDirection(int direction) throws SQLException {

    }

    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }

    public void setFetchSize(int rows) throws SQLException {

    }

    public int getFetchSize() throws SQLException {
        return batchSize;
    }

    public int getType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    public int getConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    public boolean rowUpdated() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean rowInserted() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean rowDeleted() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNull(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateShort(int columnIndex, short x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateInt(int columnIndex, int x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateLong(int columnIndex, long x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateString(int columnIndex, String x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateDate(int columnIndex, Date x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateTime(int columnIndex, Time x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNull(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateByte(String columnLabel, byte x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateShort(String columnLabel, short x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateInt(String columnLabel, int x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateLong(String columnLabel, long x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateFloat(String columnLabel, float x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateDouble(String columnLabel, double x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }


    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }


    public void updateString(String columnLabel, String x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateDate(String columnLabel, Date x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateTime(String columnLabel, Time x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateObject(String columnLabel, Object x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void insertRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void deleteRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void refreshRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void cancelRowUpdates() throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void moveToInsertRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void moveToCurrentRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }


    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public Ref getRef(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Blob getBlob(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Clob getClob(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Array getArray(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Ref getRef(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Blob getBlob(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Clob getClob(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Array getArray(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }


    public URL getURL(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public URL getURL(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateRef(String columnLabel, Ref x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateClob(String columnLabel, Clob x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateArray(int columnIndex, Array x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateArray(String columnLabel, Array x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public RowId getRowId(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public RowId getRowId(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public int getHoldability() throws SQLException {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    public void updateNString(int columnIndex, String nString) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNString(String columnLabel, String nString) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public NClob getNClob(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public NClob getNClob(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public String getNString(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public String getNString(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
