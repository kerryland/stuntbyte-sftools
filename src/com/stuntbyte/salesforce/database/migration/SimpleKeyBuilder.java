package com.stuntbyte.salesforce.database.migration;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 */
public class SimpleKeyBuilder implements KeyBuilder {
    private String keyName;

    public SimpleKeyBuilder(String keyName) {
        this.keyName = keyName;
    }

    public String buildKey(ResultSet rs) throws SQLException {
        return rs.getString(keyName);
    }
}

