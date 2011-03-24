package com.fidelma.salesforce.jdbc;

import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.bind.XmlObject;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.io.Reader;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */

// http://wiki.developerforce.com/index.php/Introduction_to_the_Force.com_Web_Services_Connector
public class SfResultSet implements java.sql.ResultSet {
    private SObject[] records;
    private SfConnection connection;
    private QueryResult qr;
    private List<String> resultFields;
    private int ptr = -1;
    private int batchEnd = 0;
    private String soqlObjectType;
    private List<String> columns = new ArrayList<String>();
    private SfResultSetMetaData metaData;

    public SfResultSet() {
        // Create an empty resultset
    }


    private void storeFields(SObject parent) {


    }

    public SfResultSet(QueryResult qr, Set<String> columnsInSql) throws SQLException {
        this.qr = qr;

        records = qr.getRecords();

        resultFields = new ArrayList<String>();
        if (records.length > 0) {
            generateResultFields(null, records[0], resultFields, columnsInSql);

        } else if (qr.getSize() != 0) {  // count()
            resultFields.add("count");
        }

        if (records.length > 0) {
            metaData = new SfResultSetMetaData(records[0], resultFields);

        } else if (qr.getSize() != 0) {  // count()
            SObject count = new SObject();
            count.setName(new QName("records"));
            count.addField("count", qr.getSize());
            records = new SObject[]{count};
            metaData = new SfResultSetMetaData(count, resultFields);
        } else {
            metaData = new SfResultSetMetaData();
        }

        batchEnd = records.length - 1;
    }


    public boolean next() throws SQLException {
        //System.out.println(ptr + " vs " + batchEnd);
        if (ptr++ < batchEnd) {
            return true;
        }

        return false;
        //return qr.isDone();
    }

    private void generateResultFields(String parentName, XmlObject parent, List<String> columnsInResult,
                                      Set<String> columnsInSql) throws SQLException {
        if (parent.hasChildren()) {
            Iterator<XmlObject> children = parent.getChildren();

            if (parentName != null) {
                parentName += "." + parent.getName().getLocalPart();
            } else {
                parentName = parent.getName().getLocalPart();
            }

            while (children.hasNext()) {
                XmlObject child = children.next();
                generateResultFields(parentName, child, columnsInResult, columnsInSql);
            }
        } else {

            String columnName;
            if (parentName != null) {
                columnName = parentName + "." + parent.getName().getLocalPart();
            } else {
                columnName = parent.getName().getLocalPart();
            }

            columnName = columnName.substring(8);
            if (!columnsInResult.contains(columnName)) {
                columnsInResult.add(columnName);
                System.out.println("COLUMNS IN RESULT: " + columnName);
            }
        }

        List<String> blurg = new ArrayList<String>();
        for (String col : columnsInResult) {
            if (columnsInSql.contains(col.toUpperCase())) {
                blurg.add(col);
                System.out.println("INCL " + col);
            }
        }
        columnsInResult.clear();
        columnsInResult.addAll(blurg);
    }


    private Object drillToChild(XmlObject parent, String columnLabel) throws SQLException {
        Object result = null;

        int dotPos = columnLabel.indexOf(".");
        if (dotPos != -1) {
            if (!parent.hasChildren()) {
                throw new SQLException(columnLabel + " does not have children");
            }
            String parentName = columnLabel.substring(0, dotPos);

            String childName = columnLabel.substring(dotPos + 1);
            XmlObject child = (XmlObject) parent.getField(parentName);
//            XmlObject child = (XmlObject) parent.getField(childName);
            System.out.println("Child " + childName + " of " + parentName + " vs " + parent.getName().getLocalPart() + " is " + child);
            if (child == null) {
                throw new SQLException("Unknown child field " + parentName + " of " + parent.getName().getLocalPart() + " cn=" + childName);
            }

            String childLabel = columnLabel.substring(dotPos + 1);
            result = drillToChild(child, childLabel);

        } else {
            result = parent.getField(columnLabel);
        }
        return result;
    }

    public String getString(String columnName) throws SQLException {
        return (String) getObject(columnName);
    }

    public String getString(int columnIndex) throws SQLException {
        return (String) getObject(columnIndex);
    }

    public Object getObject(int columnIndex) throws SQLException {
        return getObject(findLabelFromIndex(columnIndex));
    }

    public Object getObject(String columnLabel) throws SQLException {

        System.out.println("col label=" + columnLabel);
        SObject obj = records[ptr];

        return drillToChild(obj, columnLabel);

//        Integer type = Types.VARCHAR;
//        if (columnLabel.toLowerCase().equals("count")) {
//            type = Types.INTEGER;
//        } else {
//            connection.getMetaDataFactory().getTable()
//        }
//
//
//       Object ro = SfStatement.dataTypeConvert(result, type);
//
//        System.out.println("RO=" + ro.getClass().getName());
//       return ro
//       ;
//       */
//        Integer dataType = sfConnection.getMetaDataFactory().lookupJdbcType(tableData.getColumn(key).getType());
//                Object value = dataTypeConvert(val, dataType);
        //        metaData.getColumnType()
//        SfStatement.dataTypeConvert(result,  )
//        return result;

    }

    private String findLabelFromIndex(int columnIndex) throws SQLException {
        if (columnIndex > resultFields.size()) {
            throw new SQLException("Unable to identify column " + columnIndex + " there are only " + resultFields.size());
        }
        return resultFields.get(columnIndex - 1);
    }


    public void close() throws SQLException {
    }


    public int findColumn(String columnLabel) throws SQLException {
        int i = 0;
        int col = -1;

        for (String fieldName : resultFields) {
            System.out.println("RF has " + fieldName + "( check for " + columnLabel );
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
            return Long.valueOf((String) o);
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
            return Integer.valueOf((String) o);
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

    private SimpleDateFormat timestampSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

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


    // TODO ------------------------------------------------------------------------
    public Date getDate(int columnIndex) throws SQLException {
        return null;
    }

    public Date getDate(String columnLabel) throws SQLException {
        return null;
    }


    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return null;
    }

    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return null;
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
        return false;
    }


    // HERE DOWN DON'T NEED IMPLEMENTING -- but should throw some exceptions ----------------------

    public Statement getStatement() throws SQLException {
        return null;
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
        return null;
    }


    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return null;
    }

    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return null;
    }


    public boolean isBeforeFirst() throws SQLException {
        return false;
    }

    public boolean isAfterLast() throws SQLException {
        return false;
    }

    public boolean isFirst() throws SQLException {
        return false;
    }

    public boolean isLast() throws SQLException {
        return false;
    }

    public void beforeFirst() throws SQLException {

    }

    public void afterLast() throws SQLException {

    }

    public boolean first() throws SQLException {
        return false;
    }

    public boolean last() throws SQLException {
        return false;
    }

    public int getRow() throws SQLException {
        return 0;
    }

    public boolean absolute(int row) throws SQLException {
        return false;
    }

    public boolean relative(int rows) throws SQLException {
        return false;
    }

    public boolean previous() throws SQLException {
        return false;
    }

    public void setFetchDirection(int direction) throws SQLException {

    }

    public int getFetchDirection() throws SQLException {
        return 0;
    }

    public void setFetchSize(int rows) throws SQLException {

    }

    public int getFetchSize() throws SQLException {
        return 0;
    }

    public int getType() throws SQLException {
        return 0;
    }

    public int getConcurrency() throws SQLException {
        return 0;
    }

    public boolean rowUpdated() throws SQLException {
        return false;
    }

    public boolean rowInserted() throws SQLException {
        return false;
    }

    public boolean rowDeleted() throws SQLException {
        return false;
    }

    public void updateNull(int columnIndex) throws SQLException {

    }

    public void updateBoolean(int columnIndex, boolean x) throws SQLException {

    }

    public void updateByte(int columnIndex, byte x) throws SQLException {

    }

    public void updateShort(int columnIndex, short x) throws SQLException {

    }

    public void updateInt(int columnIndex, int x) throws SQLException {

    }

    public void updateLong(int columnIndex, long x) throws SQLException {

    }

    public void updateFloat(int columnIndex, float x) throws SQLException {

    }

    public void updateDouble(int columnIndex, double x) throws SQLException {

    }

    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {

    }

    public void updateString(int columnIndex, String x) throws SQLException {

    }

    public void updateBytes(int columnIndex, byte[] x) throws SQLException {

    }

    public void updateDate(int columnIndex, Date x) throws SQLException {

    }

    public void updateTime(int columnIndex, Time x) throws SQLException {

    }

    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {

    }

    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {

    }

    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {

    }

    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {

    }

    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {

    }

    public void updateObject(int columnIndex, Object x) throws SQLException {

    }

    public void updateNull(String columnLabel) throws SQLException {

    }

    public void updateBoolean(String columnLabel, boolean x) throws SQLException {

    }

    public void updateByte(String columnLabel, byte x) throws SQLException {

    }

    public void updateShort(String columnLabel, short x) throws SQLException {

    }

    public void updateInt(String columnLabel, int x) throws SQLException {

    }

    public void updateLong(String columnLabel, long x) throws SQLException {

    }

    public void updateFloat(String columnLabel, float x) throws SQLException {

    }

    public void updateDouble(String columnLabel, double x) throws SQLException {

    }

    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {

    }

    public void updateString(String columnLabel, String x) throws SQLException {

    }

    public void updateBytes(String columnLabel, byte[] x) throws SQLException {

    }

    public void updateDate(String columnLabel, Date x) throws SQLException {

    }

    public void updateTime(String columnLabel, Time x) throws SQLException {

    }

    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {

    }

    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {

    }

    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {

    }

    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {

    }

    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {

    }

    public void updateObject(String columnLabel, Object x) throws SQLException {

    }

    public void insertRow() throws SQLException {

    }

    public void updateRow() throws SQLException {

    }

    public void deleteRow() throws SQLException {

    }

    public void refreshRow() throws SQLException {

    }

    public void cancelRowUpdates() throws SQLException {

    }

    public void moveToInsertRow() throws SQLException {

    }

    public void moveToCurrentRow() throws SQLException {

    }


    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    public Ref getRef(int columnIndex) throws SQLException {
        return null;
    }

    public Blob getBlob(int columnIndex) throws SQLException {
        return null;
    }

    public Clob getClob(int columnIndex) throws SQLException {
        return null;
    }

    public Array getArray(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Ref getRef(String columnLabel) throws SQLException {
        return null;
    }

    public Blob getBlob(String columnLabel) throws SQLException {
        return null;
    }

    public Clob getClob(String columnLabel) throws SQLException {
        return null;
    }

    public Array getArray(String columnLabel) throws SQLException {
        return null;
    }


    public URL getURL(int columnIndex) throws SQLException {
        return null;
    }

    public URL getURL(String columnLabel) throws SQLException {
        return null;
    }

    public void updateRef(int columnIndex, Ref x) throws SQLException {

    }

    public void updateRef(String columnLabel, Ref x) throws SQLException {

    }

    public void updateBlob(int columnIndex, Blob x) throws SQLException {

    }

    public void updateBlob(String columnLabel, Blob x) throws SQLException {

    }

    public void updateClob(int columnIndex, Clob x) throws SQLException {

    }

    public void updateClob(String columnLabel, Clob x) throws SQLException {

    }

    public void updateArray(int columnIndex, Array x) throws SQLException {

    }

    public void updateArray(String columnLabel, Array x) throws SQLException {

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

    public boolean isClosed() throws SQLException {
        return true;
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
