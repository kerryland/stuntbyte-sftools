package com.fidelma.salesforce.jdbc.metaforce;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Table {

    private String name;
    private String comments;
    private List<Column> columns;
    private Map<String, Column> columnMap = new HashMap<String, Column>();

    public Table(String name, String comments, List<Column> columns) {
        this.name = name;
        this.comments = comments;
        this.columns = columns;
        for (Column c : columns) {
            c.setTable(this);
            columnMap.put(c.getName().toUpperCase(), c);
        }
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
            String msg = "Unable to find '" + columnName + "' in " + name;
            if (columnMap.get((columnName + "__c").toUpperCase()) != null) {
                msg += ". Do you mean '" + columnName + "'__c";
            }
            throw new SQLException(msg);
        }
        return result;
    }
}
