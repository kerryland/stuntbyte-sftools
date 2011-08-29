package com.fidelma.salesforce.database.migration;

public class MigrationCriteria {
    public String tableName;
    public String sql = "";

    public MigrationCriteria(String tableName) {
        this.tableName = tableName;
    }

    public MigrationCriteria(String tableName, String sql) {
        this.tableName = tableName;
        this.sql = sql;
    }
}