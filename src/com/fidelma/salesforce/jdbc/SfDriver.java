package com.fidelma.salesforce.jdbc;

import java.sql.*;
import java.util.Properties;

/**
 */
public class SfDriver implements java.sql.Driver {

    static {
        try {
            DriverManager.registerDriver(new SfDriver());
        } catch (SQLException e) {
            e.printStackTrace();  // TODO properly!
        }
    }

    public java.sql.Connection connect(String url, Properties info) throws SQLException {
        String server;
        if (url.startsWith("jdbc:sfdc:")) {
            server = url.substring("jdbc:sfdc:".length());
            SfConnection con = new SfConnection(
                    server,
                    (String) info.get("user"),
                    (String) info.get("password"),
                    info);

            return con;
        }
        return null;
    }

    public boolean acceptsURL(String url) throws SQLException {
        return false;
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    public int getMajorVersion() {
        return 2;
    }

    public int getMinorVersion() {
        return 0;
    }

    public boolean jdbcCompliant() {
        return false;
    }
}
