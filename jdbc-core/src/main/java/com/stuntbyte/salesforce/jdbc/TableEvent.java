package com.stuntbyte.salesforce.jdbc;

import com.stuntbyte.salesforce.jdbc.metaforce.Table;

import java.sql.SQLException;

/**
 */
public interface TableEvent {

    void onTable(Table table) throws SQLException;
}
