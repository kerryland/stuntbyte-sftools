package com.stuntbyte.salesforce.jdbc.metaforce;

import com.stuntbyte.salesforce.jdbc.SfMetadataResultSetMetadata;
import com.stuntbyte.salesforce.jdbc.SfResultSet;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

public class ForceResultSet extends SfResultSet implements ResultSet {

    private int index = -1;
    private List<ColumnMap<String, Object>> maps;

    public ForceResultSet(List<ColumnMap<String, Object>> maps) {
        this.maps = maps;
    }

    public Object getObject(String columnName) throws SQLException {
        if (isBeforeFirst()) {
            throw new SQLException("ResultSet.next() not called");
        }
        return maps.get(index).get(columnName.toUpperCase());
    }

    public Object getObject(int columnIndex) throws SQLException {
        if (isBeforeFirst()) {
            throw new SQLException("ResultSet.next() not called");
        }
        return maps.get(index).getValueByIndex(columnIndex);
    }

    public boolean first() throws SQLException {
        if (maps.size() > 0) {
            index = 0;
            return true;
        } else {
            return false;
        }
    }

    public boolean last() throws SQLException {
        if (maps.size() > 0) {
            index = maps.size() - 1;
            return true;
        } else {
            return false;
        }
    }

    public boolean next() throws SQLException {
        if (maps.size() > 0) {
            index++;
            return index < maps.size();
        } else {
            return false;
        }
    }

    public boolean isAfterLast() throws SQLException {
        return maps.size() > 0 && index == maps.size();
    }

    public boolean isBeforeFirst() throws SQLException {
        return maps.size() > 0 && index == -1;
    }

    public boolean isFirst() throws SQLException {
        return maps.size() > 0 && index == 0;
    }

    public boolean isLast() throws SQLException {
        return maps.size() > 0 && index == maps.size() - 1;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return new SfMetadataResultSetMetadata(maps);
    }
}
