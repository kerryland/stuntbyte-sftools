package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.metaforce.ColumnMap;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 */
public class SfMetadataResultSetMetadata implements ResultSetMetaData {


    private List<ColumnMap<String, Object>> maps;

    public SfMetadataResultSetMetadata(List<ColumnMap<String, Object>> maps) {
        this.maps = maps;
    }

    public int getColumnCount() throws SQLException {
        if (maps.size() !=0) {
            return maps.get(0).size();
        }
        return 0;
    }

    // Here down prob should implement
    public String getColumnName(int column) throws SQLException {
        if (getColumnCount() == 0) {
            throw new SQLException("No data returned");
        }
        ColumnMap<String, Object> colMap = maps.get(0);
        return (String) colMap.getColumnNameByIndex(column);
    }

    public String getTableName(int column) throws SQLException {
        return "";
    }


    public String getColumnLabel(int column) throws SQLException {
        return getColumnName(column);
    }

    // TODO: Implement properly
    public int getColumnType(int column) throws SQLException {
        return Types.VARCHAR;
    }
    // TODO: Implement properly
    public String getColumnTypeName(int column) throws SQLException {
        return "string";
    }

    //--------- Here down not implemented


    public boolean isAutoIncrement(int column) throws SQLException {
        return false;
    }

    public boolean isCaseSensitive(int column) throws SQLException {
        return false;
    }

    public boolean isSearchable(int column) throws SQLException {
        return false;
    }

    public boolean isCurrency(int column) throws SQLException {
        return false;
    }

    public int isNullable(int column) throws SQLException {
        return 0;
    }

    public boolean isSigned(int column) throws SQLException {
        return false;
    }

    public int getColumnDisplaySize(int column) throws SQLException {
        return 0;
    }



    public String getSchemaName(int column) throws SQLException {
        return "";
    }

    public int getPrecision(int column) throws SQLException {
        return 0;
    }

    public int getScale(int column) throws SQLException {
        return 0;
    }


    public String getCatalogName(int column) throws SQLException {
        return "";
    }

    public boolean isReadOnly(int column) throws SQLException {
        return false;
    }

    public boolean isWritable(int column) throws SQLException {
        return false;
    }

    public boolean isDefinitelyWritable(int column) throws SQLException {
        return false;
    }

    public String getColumnClassName(int column) throws SQLException {
        return null;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
