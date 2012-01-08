package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.TypeHelper;
import com.fidelma.salesforce.parse.ParsedColumn;
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
    private List<String> colAliases = new ArrayList<String>();

    // For an empty result set
    public SfResultSetMetaData() {

    }

    public SfResultSetMetaData(
            String drivingTable,
            ResultSetFactory rsf,
            SObject record,
            List<String> resultFields,
            List<ParsedColumn> resultDataTypes,
            boolean useLabels) throws SQLException {

        this.rsf = rsf;
        this.useLabels = useLabels;

        String baseType = record.getType();
        Boolean isAggregate = baseType.equals("AggregateResult");
        if (isAggregate) {
            baseType = drivingTable;
        }

        addChildren(baseType, isAggregate, resultFields, resultDataTypes);
    }


    private void addChildren(String type, Boolean aggregate, List<String> resultFields, List<ParsedColumn> resultDataTypes)
            throws SQLException {

        int ptr = 0;
        for (String resultField : resultFields) {

            ParsedColumn pc = resultDataTypes.get(ptr++);

            StringTokenizer tok = new StringTokenizer(resultField, ".", false);
            Column column = keepDrilling(tok, type, pc, null, aggregate);
            if (column == null) {
                throw new SQLException("Failed to find column " + resultField);
            }

            cols.add(column);
            colName.add(resultField);

//            System.out.println("KJS GOT " + pc.isAlias() + " " + pc.getAliasName() + " " + pc.getName() + " " + column.getName());
            
            if (pc.isAlias()) {
                colAliases.add(pc.getAliasName());
            } else {
                colAliases.add(column.getName());
            }
        }
    }

    private Column keepDrilling(StringTokenizer tok, String type, ParsedColumn pc, Column column, Boolean aggregate) throws SQLException {
        while (tok.hasMoreTokens()) {
            String col = tok.nextToken();
            String lookup = col;
            if (col.toLowerCase().endsWith("__r")) {
                lookup = col.substring(0, col.length() - 1) + "c";
            }
            Table t = rsf.getTable(type);
            try {
                column = null;

                if (lookup.equalsIgnoreCase("CreatedBy") || (lookup.equalsIgnoreCase("LastModifiedBy"))) {
                    column = new Column(lookup, "reference"); // TODO: Lookup?
                    column.setRelationshipType("User");
                } else {
                    column = t.getColumn(lookup);
                }

                if (column.getRelationshipType() != null) {
                    type = column.getRelationshipType();
                    return keepDrilling(tok, type, pc, column, aggregate);
                }

            } catch (SQLException e) {
                if (!aggregate && (column != null && !column.hasMultipleRelationships())) {
                    throw new SQLException("Attempted to invent column data for " + lookup + " : " + e.getMessage());
                }

                // Try to figure out the data type for an aggregate result.
                column = new Column(lookup);
                column.setLabel(lookup);
                column.setCalculated(true);
                column.setLength(10);

                if (aggregate) {
                    String aggregateDatatype = null;

                    if ((pc.getFunctionName().equalsIgnoreCase("count")) ||
                            (pc.getFunctionName().equalsIgnoreCase("COUNT_DISTINCT"))) {
                        aggregateDatatype = "int";
                    }

                    // This should handle most expressions, including Max and min
                    try {
                        // Because SF expressions are so woeful (at least in 2011)
                        // we don't have to be clever evaluating the expression contents
                        // because we know it's always a single column.
                        Column aggregateColumn = t.getColumn(pc.getExpressionContents());
                        aggregateDatatype = aggregateColumn.getType();

                        column.setLength(aggregateColumn.getLength());
                        column.setPrecision(aggregateColumn.getPrecision());
                        column.setScale(aggregateColumn.getScale());

                    } catch (SQLException e1) {
                    }

                    // If we couldn't figure out the data type, make a guess
                    if (aggregateDatatype == null) {
                        if ((pc.getFunctionName().equalsIgnoreCase("avg")) ||
                                (pc.getFunctionName().equalsIgnoreCase("sum"))) {
                            aggregateDatatype = "decimal";
                        } else {
                            aggregateDatatype = "string";
                        }
                    }

                    column.setType(aggregateDatatype);

                } else {
                    // Some columns (eg: Task.Who and Task.What) can be fk to more than one type of table.
                    // Getting the data type right seems like more work than it's worth right now. TODO!
                    column.setType("string");
                }

                return column;
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
//        System.out.println("Label=" + colAliases.get(column - 1));
        return colAliases.get(column - 1);
//        Column col = getColumn(column);
//        if ((col != null) && useLabels) {
//            return col.getLabel();
//        }
//        return getColumnName(column) +"X";
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
        return cols.get(column - 1);
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
