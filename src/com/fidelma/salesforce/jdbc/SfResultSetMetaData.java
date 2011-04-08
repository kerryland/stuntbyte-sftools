package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.TypeHelper;
import com.sforce.soap.partner.sobject.SObject;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 */
public class SfResultSetMetaData implements ResultSetMetaData {

    private ResultSetFactory rsf;
    private boolean useLabels;

    private List<Column> cols = new ArrayList<Column>();
    private List<String> colName = new ArrayList<String>();

    // For an empty result set
    public SfResultSetMetaData() {

    }

    public SfResultSetMetaData(
            ResultSetFactory rsf,
            SObject record,
            List<String> resultFields,
            boolean useLabels) throws SQLException {

        this.rsf = rsf;
        this.useLabels = useLabels;
        addChildren(record.getType(), resultFields);
    }


    private void addChildren(String type, List<String> resultFields) throws SQLException {
        for (String resultField : resultFields) {

            StringTokenizer tok = new StringTokenizer(resultField, ".", false);
            Column column = keepDrilling(tok, type, null);
            if (column == null) {
                System.out.println("IMPLICATIONS? Failed to find column " + resultField);
                column = new Column(resultField, "string");
            }

            cols.add(column);
            colName.add(resultField);
        }
    }

    private Column keepDrilling(StringTokenizer tok, String type, Column column) {
        while (tok.hasMoreTokens()) {
            String col = tok.nextToken();
            String lookup = col;
            if (col.toLowerCase().endsWith("__r")) {
                lookup = col.replaceFirst("__r$", "__c");
            }
            try {
                Table t = rsf.getTable(type);
                try {
                    column = t.getColumn(lookup);
                    if (column.getRelationshipType() != null) {
                        type = column.getRelationshipType();
                        return keepDrilling(tok, type, column);
                    }
                } catch (SQLException e) {
                    // make something up
                    System.out.println("Make up data about column " + lookup);
                    column = new Column(lookup, "string");
                    column.setLabel(lookup);
                    column.setLength(10);
                    column.setCalculated(true);

                    return column;
                }

            } catch (SQLException e) {
                System.out.println("Borked table?");
                e.printStackTrace();
            }
        }
        return column;
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
        return true;
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
//        System.out.println("Use labels=" + useLabels);
        if ((col != null) && useLabels) {
            return col.getLabel();
        }
        return getColumnName(column);
    }

    public String getColumnName(int column) throws SQLException {
        return colName.get(column - 1);
    }

    public String getSchemaName(int column) throws SQLException {
        return "";
    }

    public int getPrecision(int column) throws SQLException {
        return getColumn(column).getPrecision();
    }

    public int getScale(int column) throws SQLException {
        return getColumn(column).getScale();
    }

    public String getTableName(int column) throws SQLException {
        if (getColumn(column).getTable() == null) {
            return "";
        }
        return getColumn(column).getTable().getName();
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
        return cols.get(column-1);
        /*
        String columnName = cols.get(column - 1).column;

        if (cols.get(column - 1).table == null) {
            // TODO: Look up type somewhere else, somehow
            Column c = new Column(columnName, "string");
            c.setLabel(columnName);
            return c;
        }

        Table table = rsf.getTable(cols.get(column - 1).table);
        try {
            return table.getColumn(columnName);
        } catch (SQLException e) {
            return null;
        }
        */
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
