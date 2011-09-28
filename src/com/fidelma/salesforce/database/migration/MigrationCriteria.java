package com.fidelma.salesforce.database.migration;

public class MigrationCriteria {
    public String tableName;
    public String sql = "";
    public String keyBuilderColumns;
    public KeyBuilder keyBuilder;

    public MigrationCriteria(String tableName) {
        this.tableName = tableName;
    }

    public MigrationCriteria(String tableName, String sql) {
        this.tableName = tableName;
        this.sql = sql;
    }

    public MigrationCriteria(String tableName, String sql, String keyBuilderColumns, KeyBuilder keyBuilder) {
        this.tableName = tableName;
        this.sql = sql;
        this.keyBuilderColumns = keyBuilderColumns;
        this.keyBuilder = keyBuilder;
    }
}