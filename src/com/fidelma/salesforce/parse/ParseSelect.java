package com.fidelma.salesforce.parse;

import java.util.List;

/**
 */
public class ParseSelect {
    private String drivingTable;
    private List<ParseColumn> columns;

    public String getDrivingTable() {
        return drivingTable;
    }

    public void setDrivingTable(String drivingTable) {
        this.drivingTable = drivingTable;
    }

    public List<ParseColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<ParseColumn> columns) {
        for (ParseColumn column : columns) {
            column.setTable(drivingTable);
        }
        this.columns = columns;
    }
}
