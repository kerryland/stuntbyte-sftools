/**
 * The MIT License
 * Copyright © 2011-2017 Kerry Sainsbury
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.stuntbyte.salesforce.jdbc;

import com.stuntbyte.salesforce.jdbc.metaforce.ColumnMap;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/**
 */
public class SfMetadataResultSetMetadata implements ResultSetMetaData {


    private List<ColumnMap<String, Object>> maps;

    public SfMetadataResultSetMetadata(List<ColumnMap<String, Object>> maps) {
        this.maps = maps;
    }

    public int getColumnCount() throws SQLException {
        if (maps.size() != 0) {
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
        return ResultSetFactory.lookupJdbcType(getColumnTypeName(column));
    }

    // TODO: Implement properly
    public String getColumnTypeName(int column) throws SQLException {
//        return "string";
        if (getColumnCount() == 0) {
            throw new SQLException("No data returned");
        }
        ColumnMap<String, Object> colMap = maps.get(0);

        Object x = colMap.getValueByIndex(column);

        if (x instanceof BigDecimal) {
            return "double";

        } else if (x instanceof Boolean) {
            return "boolean";

        } else if (x instanceof Date) {
            return "datetime";

        } else if (x instanceof Double) {
            return "double";

        } else if (x instanceof Float) {
            return "double";

        } else if (x instanceof Integer) {
            return "int";

        } else if (x instanceof Long) {
            return "int";

        } else if (x instanceof Short) {
            return "int";

        } else if (x instanceof String) {
            return "string";

        } else if (x instanceof Time) {
            return "time";

        } else if (x instanceof Timestamp) {
            return "datetime";

        }

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
        return ResultSetFactory.catalogName;
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
        if (getColumnCount() == 0) {
            throw new SQLException("No data returned");
        }
        ColumnMap<String, Object> colMap = maps.get(0);

        Object x =  colMap.getValueByIndex(column);

        if (x == null) {
            return String.class.getName();
        }
        
        return x.getClass().getName();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
