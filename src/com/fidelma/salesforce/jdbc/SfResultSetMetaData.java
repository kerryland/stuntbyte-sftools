package com.fidelma.salesforce.jdbc;

import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.bind.XmlObject;

import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class SfResultSetMetaData implements ResultSetMetaData {

    private class ColumnInfo {
        String type;
        String name;
    }

    private List<ColumnInfo> cols = new ArrayList<ColumnInfo>();

    // For an empty result set
    public SfResultSetMetaData() {

    }

    public SfResultSetMetaData(SObject record, List<String> resultFields) throws SQLException {
        addChildren(null, record, resultFields, new HashSet<String>());
    }


    private void addChildren(String baseName, XmlObject parent, List<String> resultFields, Set<String> already) {
        Iterator cIt = parent.getChildren();

        String type = "Unknown";
        if (parent instanceof SObject) {
            SObject sObject = (SObject) parent;
            type = sObject.getType();
        }

//        System.out.println("PROCESSING " + parent.getName().getLocalPart() + " with base of " + baseName);

        if (baseName != null) {
            baseName += "." + parent.getName().getLocalPart();
        } else if (!"records".equals(parent.getName().getLocalPart())) {
            baseName = parent.getName().getLocalPart();
        }


        while (cIt.hasNext()) {
            XmlObject next = (XmlObject) cIt.next();

            if (next.hasChildren()) {
                addChildren(baseName, next, resultFields, already);
            } else {
                ColumnInfo ci = new ColumnInfo();
                ci.type = type;
                if (baseName == null) {
                    ci.name = next.getName().getLocalPart();
                } else {
                    ci.name = baseName + "." + next.getName().getLocalPart();
                }
                if (resultFields.contains(ci.name)) {
                    if (already.contains(ci.name)) {

                    } else {
                        cols.add(ci);
                        already.add(ci.name);
                    }
                } else {
                    // TODO: Throw an exception?
                    System.out.println("WHAT TO DO WITH base  " + baseName + ">> " + ci.name);
                }
            }
        }
    }


    public int getColumnCount() throws SQLException {
        return cols.size();
    }

    public boolean isAutoIncrement(int column) throws SQLException {
        return false;
    }

    public boolean isCaseSensitive(int column) throws SQLException {
        return true;
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
        return 10; // TODO:
    }

    public String getColumnLabel(int column) throws SQLException {
        return cols.get(column - 1).name;
    }

    public String getColumnName(int column) throws SQLException {
        return cols.get(column - 1).name;
    }

    public String getSchemaName(int column) throws SQLException {
        return null;
    }

    public int getPrecision(int column) throws SQLException {
        return 0;
    }

    public int getScale(int column) throws SQLException {
        return 0;
    }

    public String getTableName(int column) throws SQLException {
        return cols.get(column - 1).type;
    }

    public String getCatalogName(int column) throws SQLException {
        return null;
    }

    public int getColumnType(int column) throws SQLException {
        return 0;
    }

    public String getColumnTypeName(int column) throws SQLException {
        return null;
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
