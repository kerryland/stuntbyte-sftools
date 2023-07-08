/**
 * This code based on code at https://code.google.com/archive/p/force-metadata-jdbc-driver/ released under the New BSD Licence
 */

package com.stuntbyte.salesforce.jdbc.metaforce;


import com.stuntbyte.salesforce.jdbc.TableEvent;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * Holds a force.com org's objects (tables) and fields (columns)
 * and translates them into ResultSet objects that match the patterns
 * specified in the DatabaseMetaData Javadoc.
 */
public class ResultSetFactory {

    public static final int DATATYPES_SQL92 = 0;
    public static final int DATATYPES_SALESFORCE_UI = 1;
    public static final int DATATYPES_SALESFORCE_API = 2;

    public ResultSetFactory(int dataTypeMode) {
        this.dataTypeMode = dataTypeMode;
    }

    private int dataTypeMode;

    public static String schemaName = "SF";
    public static String DEPLOYABLE = "deployable";
    public static String catalogName = null;

    public static String getNiceName(String dataType) {
        TypeInfo typeInfo = lookupTypeInfo(dataType);
        if (typeInfo == null) {
            return "unknown '" + dataType + "'";
        }
        return typeInfo.getNiceName();
    }

    public static String getSql92Name(String dataType) {
        TypeInfo typeInfo = lookupTypeInfo(dataType);
        if (typeInfo == null) {
            return "unknown '" + dataType + "'";
        }
        return typeInfo.getSql92name();
    }



    private static class TypeInfo {
        public TypeInfo(
                String sql92name,
                String niceName,
                String typeName,
                int sqlDataType,
                int precision,
                int minScale,
                int maxScale,
                int radix) {
            this.sql92name = sql92name;
            this.niceName = niceName;
            this.typeName = typeName;
            this.sqlDataType = sqlDataType;
            this.precision = precision;
            this.minScale = minScale;
            this.maxScale = maxScale;
            this.radix = radix;
        }

        private String sql92name;
        private String niceName;
        String typeName;
        int sqlDataType;
        int precision;
        int minScale;
        int maxScale;
        int radix;

        public String getSql92name() {
            return sql92name;
        }

        public String getNiceName() {
            return niceName;
        }
    }

    /*

    https://developer.salesforce.com/docs/atlas.en-us.api.meta/api/primitive_data_types.htm
    https://developer.salesforce.com/docs/atlas.en-us.api.meta/api/field_types.htm

AutoNumber
Lookup
MasterDetail
Checkbox
Currency
Date
DateTime
Email
Number
Percent
Phone
Picklist
MultiselectPicklist
Text
TextArea
LongTextArea
Url
EncryptedText
Summary
Hierarchy
    */

    // LogTextArea

    private static TypeInfo TYPE_INFO_DATA[] = {
            new TypeInfo("varchar", "Id", "id", Types.VARCHAR, 0x7fffffff, 0, 0, 0),

            new TypeInfo("varchar", "MasterDetail", "masterrecord", Types.VARCHAR, 0x7fffffff, 0, 0, 0),
            new TypeInfo("varchar", "Lookup", "reference", Types.VARCHAR, 0x7fffffff, 0, 0, 0),
            new TypeInfo("varchar", "Text", "string", Types.VARCHAR, 0x7fffffff, 0, 0, 0),
            new TypeInfo("varchar", "EncryptedText", "encryptedstring", Types.VARCHAR, 0x7fffffff, 0, 0, 0),
            new TypeInfo("varchar", "Email", "email", Types.VARCHAR, 0x7fffffff, 0, 0, 0),
            new TypeInfo("varchar", "Address", "address", Types.STRUCT, 0, 0, 0, 0),
            new TypeInfo("varchar", "Phone", "phone", Types.VARCHAR, 0x7fffffff, 0, 0, 0),
            new TypeInfo("varchar", "Url", "url", Types.VARCHAR, 0x7fffffff, 0, 0, 0),
            new TypeInfo("varchar", "TextArea", "textarea", Types.LONGVARCHAR, 0x7fffffff, 0, 0, 0),
            new TypeInfo("varchar", "LongTextArea", "base64", Types.LONGVARCHAR, 0x7fffffff, 0, 0, 0),  // LongTextArea?
            new TypeInfo("boolean", "Checkbox", "boolean", Types.BOOLEAN, 1, 0, 0, 0),
            new TypeInfo("boolean", "Checkbox", "_boolean", Types.BOOLEAN, 1, 0, 0, 0),
            new TypeInfo("binary", "Byte", "byte", Types.VARBINARY, 10, 0, 0, 10),     // Byte ?
            new TypeInfo("binary", "Byte", "_byte", Types.VARBINARY, 10, 0, 0, 10),    // Byte?
            new TypeInfo("decimal", "Number", "decimal", Types.DECIMAL, 17, -324, 306, 10),
            new TypeInfo("integer", "Number", "int", Types.INTEGER, 10, 0, 0, 10),
            new TypeInfo("integer", "Number", "_int", Types.INTEGER, 10, 0, 0, 10),
            new TypeInfo("double", "Number", "double", Types.DOUBLE, 17, -324, 306, 10),
            new TypeInfo("double", "Number", "_double", Types.DOUBLE, 17, -324, 306, 10),
            new TypeInfo("double", "Percent", "percent", Types.DOUBLE, 17, -324, 306, 10),
            new TypeInfo("decimal", "Currency", "currency", Types.DOUBLE, 17, -324, 306, 10), // TODO: double for currency seems crazy!
            new TypeInfo("date", "Date", "date", Types.DATE, 10, 0, 0, 0),
            new TypeInfo("time", "Time", "time", Types.TIME, 10, 0, 0, 0),      // Time?
            new TypeInfo("timestamp", "DateTime", "datetime", Types.TIMESTAMP, 10, 0, 0, 0),

            new TypeInfo("varchar", "Picklist", "picklist", Types.VARCHAR, 0x7fffffff, 0, 0, 0),
            new TypeInfo("varchar", "MultiselectPicklist", "multipicklist", Types.VARCHAR, 0x7fffffff, 0, 0, 0),
            new TypeInfo("varchar", "MultiselectPicklist", "combobox", Types.VARCHAR, 0x7fffffff, 0, 0, 0),  // MultiselectPicklist?

            // TODO: How handle autonumber?
//            new TypeInfo("varchar", "AutoNumber", "string", Types.VARCHAR, 0x7fffffff, 0, 0, 0),
            new TypeInfo("varchar", "AutoNumber", "autonumber", Types.VARCHAR, 0x7fffffff, 0, 0, 0),

            // new TypeInfo("varchar", "picklist", Types.ARRAY, 0, 0, 0, 0),
            // new TypeInfo("varchar", "multipicklist", Types.ARRAY, 0, 0, 0, 0),
            // new TypeInfo("varchar", "combobox", Types.ARRAY, 0, 0, 0, 0),


            new TypeInfo("binary", "any", "anyType", Types.OTHER, 0, 0, 0, 0),

            new TypeInfo("binary", "Summary", "summary", Types.OTHER, 0, 0, 0, 0),      // Summary?
    };


    private Set<Table> tables = new TreeSet<>(new Comparator<Table>() {
        @Override
        public int compare(Table table, Table t1) {
            return (table.getSchema() + table.getName()).compareTo(t1.getSchema() + t1.getName());
        }
    });
    private int counter;

    public void addTable(Table table) {
        int ordinal = 0;
        for (Column column : table.getColumns()) {
            column.setOrdinal(++ordinal);
        }

        tables.add(table);
    }

    public void removeTable(String tableName) throws SQLException {
        Table table = getTable(ResultSetFactory.schemaName,  tableName);
        tables.remove(table);
    }

    public void removeColumn(String tableName, String columnName) throws SQLException {
        Table table = getTable(ResultSetFactory.schemaName, tableName);
        table.removeColumn(columnName);
    }


    public Table getTable(String tableName) throws SQLException {
        return getTable(ResultSetFactory.schemaName, tableName);

    }
    public Table getTable(String schema, String tableName) throws SQLException {

        final Table[] result = new Table[1];
        getTables(schema, tableName, new TableEvent() {
            public void onTable(Table table) throws SQLException {
                result[0] = table;
            }
        });
        if (result[0] == null) {
            throw new SQLException("Unknown table: " + tableName);
        }
        return result[0];
    }

    private boolean include(String search, String compare) {
        if (compare == null) {
            return true;
        }
        String compareUpper = compare.toUpperCase();
        boolean include = false;

        if (search == null || search.equals("")) {
            include = true;
        } else if (search.equals("%")) {
            include = true;
        } else if (search.startsWith("%") && compareUpper.endsWith(search.substring(1))) {
            include = true;
        } else if (search.endsWith("%") && compareUpper.startsWith(search.substring(0, search.indexOf("%") - 1))) {
            include = true;
        } else if (compareUpper.equalsIgnoreCase(search)) {
            include = true;
        }

        return include;
    }

    public void getTables(String schemaPattern, String tablePattern, TableEvent tableEvent) throws SQLException {
        for (Table table : tables) {
            if (include(schemaPattern, table.getSchema()) && (include(tablePattern, table.getName()))) {
                tableEvent.onTable(table);
            }
        }
    }


    public Collection<Table> getTables() {
        return tables;
    }

    /**
     * Provide table (object) detail.
     */
    public ResultSet createTableResultSet(String schemaPattern, String tableNamePattern, String[] types) {
        if (tableNamePattern != null) {
            tableNamePattern = tableNamePattern.toUpperCase();
        }

        Set<String> typeSet = new HashSet<String>();
        if (types != null) {
            for (int i = 0; i < types.length; i++) {
                typeSet.add(types[i].toUpperCase());
            }
        }

        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        for (Table table : tables) {
            if ((types == null) || (typeSet.contains(table.getType().toUpperCase()))) {
                if (include(schemaPattern, table.getSchema()) && (include(tableNamePattern, table.getName()))) {
                    ColumnMap<String, Object> map = new ColumnMap<String, Object>();
                    map.put("TABLE_CAT", catalogName);
                    map.put("TABLE_SCHEM", table.getSchema());
                    map.put("TABLE_NAME", table.getName());
                    map.put("TABLE_TYPE", table.getType());
                    map.put("REMARKS", table.getComments());
                    map.put("TYPE_CAT", null);
                    map.put("TYPE_SCHEM", null);
                    map.put("TYPE_NAME", null);
                    map.put("SELF_REFERENCING_COL_NAME", null);
                    map.put("REF_GENERATION", null);
                    maps.add(map);
                }
            }
        }
        return new ForceResultSet(maps);
    }

    public ResultSet getTypeInfo() {
        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        for (TypeInfo typeInfo : TYPE_INFO_DATA) {
            ColumnMap<String, Object> map = new ColumnMap<String, Object>();
            map.put("TYPE_NAME", getType(typeInfo.typeName));
            map.put("DATA_TYPE", typeInfo.sqlDataType);
            map.put("PRECISION", typeInfo.precision);
            map.put("LITERAL_PREFIX", null);
            map.put("LITERAL_SUFFIX", null);
            map.put("CREATE_PARAMS", null);
            map.put("NULLABLE", 1);
            map.put("CASE_SENSITIVE", 0);
            map.put("SEARCHABLE", 3);
            map.put("UNSIGNED_ATTRIBUTE", false);
            map.put("FIXED_PREC_SCALE", false);
            map.put("AUTO_INCREMENT", false);
            map.put("LOCAL_TYPE_NAME", getType(typeInfo.typeName));
            map.put("MINIMUM_SCALE", typeInfo.minScale);
            map.put("MAXIMUM_SCALE", typeInfo.maxScale);
            map.put("SQL_DATA_TYPE", typeInfo.sqlDataType);
            map.put("SQL_DATETIME_SUB", null);
            map.put("NUM_PREC_RADIX", typeInfo.radix);
            map.put("TYPE_SUB", 1);

            maps.add(map);
        }
        return new ForceResultSet(maps);
    }

    public ResultSet getCatalogs() {
        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        if (catalogName != null) {
            ColumnMap<String, Object> row = new ColumnMap<String, Object>();
            row.put("TABLE_CAT", catalogName);
            maps.add(row);
        }
        return new ForceResultSet(maps);
    }

    public ResultSet getSchemas() {
        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        if (schemaName != null || catalogName != null) {
            ColumnMap<String, Object> row;
            row = new ColumnMap<String, Object>();
            row.put("TABLE_SCHEM", schemaName);
            row.put("TABLE_CATALOG", catalogName);
            maps.add(row);

            row = new ColumnMap<String, Object>();
            row.put("TABLE_SCHEM", DEPLOYABLE);
            row.put("TABLE_CATALOG", catalogName);
            maps.add(row);
        }
        return new ForceResultSet(maps);
    }

    /**
     * Provide column (field) detail.
     */
    public ResultSet getColumns(String schemaPattern, String tableName, final String columnPattern) throws SQLException {
        final List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();

        getTables(schemaPattern, tableName, new TableEvent() {
            public void onTable(Table table) throws SQLException {
                for (Column column : table.getColumns()) {
                    if (include(columnPattern, column.getName())) {
                        ColumnMap<String, Object> map = new ColumnMap<String, Object>();
                        TypeInfo typeInfo = lookupTypeInfo(column.getType());
                        map.put("TABLE_CAT", catalogName);
                        map.put("TABLE_SCHEM", table.getSchema());
                        map.put("TABLE_NAME", table.getName());
                        map.put("COLUMN_NAME", column.getName());
                        map.put("DATA_TYPE", typeInfo != null ? typeInfo.sqlDataType : Types.OTHER);
                        map.put("TYPE_NAME", column.getType());
                        map.put("COLUMN_SIZE", column.getLength());
                        map.put("BUFFER_LENGTH", 0);
                        map.put("DECIMAL_DIGITS", column.getPrecision());
                        map.put("NUM_PREC_RADIX", typeInfo != null ? typeInfo.radix : 10);
                        map.put("NULLABLE", column.isNillable() ? DatabaseMetaData.columnNullable : DatabaseMetaData.columnNoNulls);
                        map.put("REMARKS", column.getComments());
                        map.put("COLUMN_DEF", column.getDefault());
                        map.put("SQL_DATA_TYPE", null);
                        map.put("SQL_DATETIME_SUB", null);
                        map.put("CHAR_OCTET_LENGTH", 0);
                        map.put("ORDINAL_POSITION", column.getOrdinal());
                        map.put("IS_NULLABLE", column.isNillable() ? "YES" : "NO");
                        map.put("SCOPE_CATLOG", null);
                        map.put("SCOPE_SCHEMA", null);
                        map.put("SCOPE_TABLE", null);
                        map.put("SOURCE_DATA_TYPE", column.getType());
                        map.put("IS_AUTOINCREMENT", column.isAutoIncrement() ? "YES" : "NO");

                        // The Auto column is obtained by SchemaSpy via ResultSetMetaData so awkward to support

                        maps.add(map);
                    }
                }
            }
        });

        return new ForceResultSet(maps);

    }


    static TypeInfo lookupTypeInfo(String forceTypeName) {
        for (TypeInfo entry : TYPE_INFO_DATA) {
            if ((forceTypeName.equalsIgnoreCase(entry.typeName) ||
                    (forceTypeName.equalsIgnoreCase(entry.niceName))) ||
                    (forceTypeName.equalsIgnoreCase(entry.sql92name))) {
                return entry;
            }
        }
        return null;
//        throw new SQLException("Unable to identify type for '" + forceTypeName + "'");
    }

    public static Integer lookupJdbcType(String forceTypeName) throws SQLException {
        TypeInfo typeInfo = lookupTypeInfo(forceTypeName);
        if (typeInfo == null) {
            return Types.OTHER;
        }
        return typeInfo.sqlDataType;
    }


    /**
     * Provide table (object) relationship information.
     */
    public ResultSet getImportedKeys(String schemaName, String tableName) throws SQLException {

        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        Table table = getTable(schemaName, tableName);

        for (Column column : table.getColumns()) {
            if (column.getReferencedTable() != null && column.getReferencedColumn() != null) {
                ColumnMap<String, Object> map = new ColumnMap<String, Object>();
                map.put("PKTABLE_CAT", catalogName);
                map.put("PKTABLE_SCHEM", table.getSchema());
                map.put("PKTABLE_NAME", column.getReferencedTable());
                map.put("PKCOLUMN_NAME", column.getReferencedColumn());
                map.put("FKTABLE_CAT", catalogName);
                map.put("FKTABLE_SCHEM", table.getSchema());
                map.put("FKTABLE_NAME", tableName);
                map.put("FKCOLUMN_NAME", column.getName());
                map.put("KEY_SEQ", 1);
                map.put("UPDATE_RULE", 0);
                map.put("DELETE_RULE", 0);
                map.put("FK_NAME", "FK" + tableName + counter);
                map.put("PK_NAME", "PK" + tableName + counter);
                map.put("DEFERRABILITY", 0);
                counter++;
                maps.add(map);
            }
        }
        return new ForceResultSet(maps);
    }

    public ResultSet getExportedKeys(String tableName) throws SQLException {
        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();

        for (Table table : tables) {
            for (Column column : table.getColumns()) {
                if (tableName.equalsIgnoreCase(column.getReferencedTable())) {
                    ColumnMap<String, Object> map = new ColumnMap<String, Object>();
                    map.put("PKTABLE_CAT", catalogName);
                    map.put("PKTABLE_SCHEM", table.getSchema());
                    map.put("PKTABLE_NAME", column.getReferencedTable());
                    map.put("PKCOLUMN_NAME", column.getReferencedColumn());

                    map.put("FKTABLE_CAT", catalogName);
                    map.put("FKTABLE_SCHEM", table.getSchema());
                    map.put("FKTABLE_NAME", table.getName());
                    map.put("FKCOLUMN_NAME", column.getName());

                    map.put("KEY_SEQ", counter);
                    map.put("UPDATE_RULE", DatabaseMetaData.importedKeyRestrict);
                    map.put("DELETE_RULE", DatabaseMetaData.importedKeySetNull);
                    map.put("FK_NAME", "FK" + table.getName() + counter);
                    map.put("PK_NAME", "PK" + table.getName() + counter);
                    map.put("DEFERRABILITY", 0);
                    counter++;
                    maps.add(map);
                }
            }
        }
        return new ForceResultSet(maps);
    }

    /**
     */
    public ResultSet getPrimaryKeys(String tableName) throws SQLException {

        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        for (Table table : tables) {
            if (table.getName().equalsIgnoreCase(tableName)) {
                for (Column column : table.getColumns()) {
                    if (column.getName().equalsIgnoreCase("Id")) {
                        ColumnMap<String, Object> map = new ColumnMap<String, Object>();
                        map.put("TABLE_CAT", catalogName);
                        map.put("TABLE_SCHEM", table.getSchema());
                        map.put("TABLE_NAME", table.getName());
                        map.put("COLUMN_NAME", "" + column.getName());
                        map.put("KEY_SEQ", 1);
                        map.put("PK_NAME", "PK" + table.getName() + counter);
                        maps.add(map);
                    }
                }
            }
        }
        return new ForceResultSet(maps);
    }

    /**
     * Avoid the tables (objects) appearing in the "tables without indexes" anomalies list.
     */
    public ResultSet getIndexInfo(String tableName) throws SQLException {

        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        for (Table table : tables) {
            if (table.getName().equalsIgnoreCase(tableName)) {
                for (Column column : table.getColumns()) {
                    if (column.getName().equalsIgnoreCase("Id")) {
                        ColumnMap<String, Object> map = new ColumnMap<String, Object>();
                        map.put("TABLE_CAT", catalogName);
                        map.put("TABLE_SCHEM", table.getSchema());
                        map.put("TABLE_NAME", table.getName());
                        map.put("NON_UNIQUE", true);
                        map.put("INDEX_QUALIFIER", null);
                        map.put("INDEX_NAME", "IX" + counter++);
                        map.put("TYPE", DatabaseMetaData.tableIndexOther);
                        map.put("ORDINAL_POSITION", column.getOrdinal());
                        map.put("COLUMN_NAME", "Id");
                        map.put("ASC_OR_DESC", "A");
                        map.put("CARDINALITY", 1);
                        map.put("PAGES", 1);
                        map.put("FILTER_CONDITION", null);

                        maps.add(map);
                    }
                }
            }
        }
        return new ForceResultSet(maps);
    }


    public String getType(String s) {
        if (dataTypeMode == DATATYPES_SQL92) {
            s = ResultSetFactory.getSql92Name(s);
        } else if (dataTypeMode == DATATYPES_SALESFORCE_UI) {
            s = ResultSetFactory.getNiceName(s);
//        } else {
            // Things look nicer when we map "double" to decimal for api
//            s =  s.equalsIgnoreCase("double") ? "decimal" : s;
        }

        return s;
    }

}