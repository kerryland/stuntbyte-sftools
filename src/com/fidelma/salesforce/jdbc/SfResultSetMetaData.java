package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.TypeHelper;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.bind.XmlObject;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 */
public class SfResultSetMetaData implements ResultSetMetaData {

    private ResultSetFactory rsf;

    private class ColumnInfo {
        String table;
        String column;
    }

    private List<ColumnInfo> cols = new ArrayList<ColumnInfo>();

    // For an empty result set
    public SfResultSetMetaData() {

    }

    public SfResultSetMetaData(
            ResultSetFactory rsf,
            SObject record, List<String> resultFields) throws SQLException {
        this.rsf = rsf;
        addChildren(null, record, resultFields, new HashSet<String>());
    }


    private void addChildren(String baseName, XmlObject parent, List<String> resultFields, Set<String> already) {
        Iterator cIt = parent.getChildren();

        String type = "Unknown";
        if (parent instanceof SObject) {
            SObject sObject = (SObject) parent;
            type = sObject.getType();
        }

//        System.out.println("PROCESSING " + parent.getName().getLocalPart() + " with base of " + baseName);

        if (baseName != null) {
            baseName += "." + parent.getName().getLocalPart();
        } else if (!"records".equals(parent.getName().getLocalPart())) {
            baseName = parent.getName().getLocalPart();
        }


        while (cIt.hasNext()) {
            XmlObject next = (XmlObject) cIt.next();

            if (next.hasChildren()) {
                addChildren(baseName, next, resultFields, already);
            } else {
                ColumnInfo ci = new ColumnInfo();
                ci.table = type;
                if (baseName == null) {
                    ci.column = next.getName().getLocalPart();
                } else {
                    ci.column = baseName + "." + next.getName().getLocalPart();
                }
                if (resultFields.contains(ci.column)) {
                    if (already.contains(ci.column)) {

                    } else {
                        cols.add(ci);
                        already.add(ci.column);
                    }
                } else {
                    // TODO: Throw an exception?
                    System.out.println("WHAT TO DO WITH base  " + baseName + ">> " + ci.column);
                }
            }
        }
    }


    public int getColumnCount() throws SQLException {
        return cols.size();
    }

    public boolean isAutoIncrement(int column) throws SQLException {
        Column col = getColumn(column);
        if (col == null) {
            return true;
        }
        return col.isAutoIncrement();
    }

    public boolean isCaseSensitive(int column) throws SQLException {
        Column col = getColumn(column);
        if (col == null) {
            return true;
        }
        return col.isCaseSensitive();
    }

    public boolean isSearchable(int column) throws SQLException {
        return true;
    }

    public boolean isCurrency(int column) throws SQLException {
        return false;
    }

    public int isNullable(int column) throws SQLException {
        Column col = getColumn(column);
        if (col == null) {
            return ResultSetMetaData.columnNullableUnknown;
        }
        if (col.isNillable()) {
            return ResultSetMetaData.columnNullable;
        }
        return ResultSetMetaData.columnNoNulls;
    }

    public boolean isSigned(int column) throws SQLException {
        return false;
    }

    public int getColumnDisplaySize(int column) throws SQLException {
        Column col = getColumn(column);
        if (col == null) {
            return 10;
        }
        return col.getLength();
    }

    public String getColumnLabel(int column) throws SQLException {
        Column col = getColumn(column);
        if (col != null) {
            return col.getLabel();
        }
        return getColumnName(column);
    }

    public String getColumnName(int column) throws SQLException {
        return cols.get(column - 1).column;
    }

    public String getSchemaName(int column) throws SQLException {
        return "";
    }

    public int getPrecision(int column) throws SQLException {
        return 0;
    }

    public int getScale(int column) throws SQLException {
        return 0;
    }

    public String getTableName(int column) throws SQLException {
        return cols.get(column - 1).table;
    }

    public String getCatalogName(int column) throws SQLException {
        return "";
    }

    public int getColumnType(int column) throws SQLException {
        Column col = getColumn(column);
        if (col == null) {
            return Types.VARCHAR; // TODO: Be smarter?
        }
        return ResultSetFactory.lookupJdbcType(col.getType());
    }

    public String getColumnTypeName(int column) throws SQLException {
        Column col = getColumn(column);
        if (col == null) {
            return "Unknown";
        }
        return col.getType();
    }

    private Column getColumn(int column) throws SQLException {
        String columnName = cols.get(column-1).column;
        Table table = rsf.getTable(cols.get(column-1).table);
        try {
            return table.getColumn(columnName);
        } catch (SQLException e) {
            return null;
        }
    }


    public boolean isReadOnly(int column) throws SQLException {
        Column col = getColumn(column);
        if (col == null) {
            return true;
        }
        return col.isCalculated();
    }

    public boolean isWritable(int column) throws SQLException {
        return !isReadOnly(column);
    }

    public boolean isDefinitelyWritable(int column) throws SQLException {
        return !isReadOnly(column);
    }

    public String getColumnClassName(int column) throws SQLException {
        return TypeHelper.dataTypeToClassName(getColumnType(column));
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
