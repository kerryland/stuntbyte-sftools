package com.fidelma.salesforce.parse;

import java.util.List;

/**
 */
public class ParsedSelect {
    private String drivingTable;
    private String parsedSql="";
    private List<ParsedColumn> columns;

    public String getDrivingTable() {
        return drivingTable;
    }

    public void setDrivingTable(String drivingTable) {
        this.drivingTable = drivingTable;
    }

    public List<ParsedColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<ParsedColumn> columns) {
        for (ParsedColumn column : columns) {
            column.setTable(drivingTable);
        }
        this.columns = columns;
    }

    public void addToSql(String val) {
        System.out.print(val + " ");
        parsedSql += val + " ";
    }

    public String getParsedSql() {
        return parsedSql;
    }

    public void addRemainderToSql() {
        //To change body of created methods use File | Settings | File Templates.
    }

    public void setParsedSql(String parsedSql) {
        this.parsedSql = parsedSql;
    }
}
