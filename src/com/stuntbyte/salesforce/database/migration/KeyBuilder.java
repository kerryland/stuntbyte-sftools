package com.stuntbyte.salesforce.database.migration;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 */
public interface KeyBuilder {
    String buildKey(ResultSet rs) throws SQLException;
}
