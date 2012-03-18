package com.stuntbyte.salesforce.jdbc.metaforce;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Table {

    private String name;
    private String comments;
    private String type;
    private List<Column> columns;
    private Map<String, Column> columnMap = new HashMap<String, Column>();

    private String schema;

    public Table(String name, String comments, String type) {
        this.name = name;
        this.comments = comments;
        this.type = type;
        this.columns = new ArrayList<Column>();
    }

    public void addColumn(Column column) {
        column.setTable(this);
        columns.add(column);
        columnMap.put(column.getName().toUpperCase(), column);
    }

    public String getName() {
        return name;
    }

    public String getComments() {
        return comments;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public Column getColumn(String columnName) throws SQLException {
        Column result = columnMap.get(columnName.toUpperCase());
        if (result == null) {
            String msg = "Unable to find '" + columnName + "' in '" + name;
            if (columnMap.get((columnName + "__c").toUpperCase()) != null) {
                msg += "'. Do you mean '" + columnName + "'__c";
            } else {
                msg += "'";
            }
            throw new SQLException(msg);
        }
        return result;
    }

    public void removeColumn(String columnName) {
        columnMap.remove(columnName.toUpperCase());
    }

    /**
     * @return "TABLE" : "SYSTEM TABLE"
     *
     */
    public String getType() {
        return type;
    }

    public Boolean isCustom() {
        return name.toUpperCase().endsWith("__C");
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setName(String tableName) {
        this.name = tableName;
    }
}
