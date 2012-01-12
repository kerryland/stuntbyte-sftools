package com.stuntbyte.salesforce.database.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 4/06/11
 * Time: 5:57 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ResultSetCallback {
    void onRow(ResultSet rs);

    void afterBatchInsert(String tableName, List<String> sourceIds, PreparedStatement pinsert) throws SQLException;

    boolean shouldInsert(String tableName, ResultSet rs, int col, Set<String> processedTables) throws SQLException;

    Object alterValue(String tableName, String columnName, Object value) throws SQLException;
}
