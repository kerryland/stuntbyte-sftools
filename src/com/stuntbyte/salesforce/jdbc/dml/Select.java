package com.stuntbyte.salesforce.jdbc.dml;

import com.stuntbyte.salesforce.core.metadata.MetadataServiceImpl;
import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.jdbc.SfResultSet;
import com.stuntbyte.salesforce.jdbc.SfStatement;
import com.stuntbyte.salesforce.jdbc.ddl.Show;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.stuntbyte.salesforce.parse.ParsedColumn;
import com.stuntbyte.salesforce.parse.ParsedSelect;
import com.stuntbyte.salesforce.parse.SimpleParser;
import com.sforce.soap.partner.QueryResult;
import com.sforce.ws.ConnectionException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;

/**
 */
public class Select {

    private SfStatement statement;
    private MetadataServiceImpl metadataService;
    private Reconnector reconnector;
    private String table;
    private String schema;

    public Select(SfStatement statement, MetadataServiceImpl metadataService, Reconnector reconnector) {
        this.statement = statement;
        this.metadataService = metadataService;
        this.reconnector = reconnector;
    }

    public ResultSet execute(String sql) throws SQLException {
        try {

            if ((!reconnector.getLicence().supportsJdbcFeature()) &&
                    (!reconnector.getLicence().supportsDeploymentFeature())) {
                throw new SQLException("Licence does not support JDBC");
            }


            SimpleParser la = new SimpleParser(sql);

            List<ParsedSelect> parsedSelects = la.extractColumnsFromSoql();
            if (parsedSelects.size() > 1) {
                throw new SQLFeatureNotSupportedException("Parent --> Child subqueries not supported via JDBC");
            }
            ParsedSelect parsedSelect = parsedSelects.get(parsedSelects.size() - 1);
            sql = parsedSelect.getParsedSql();

            table = parsedSelect.getDrivingTable();
            String[] tableSplit = table.split("\\.");
            if (tableSplit.length > 1) {
                schema = tableSplit[0];
                table = tableSplit[1];
            }

            SfConnection conn = (SfConnection) statement.getConnection();
            ResultSet rs = conn.getMetaData().getTables(null, schema, table, null);
            if (!rs.next()) {
                throw new SQLException("Unknown table " + table + " in schema " + schema);
            }

            sql = removeQuotedColumns(sql, parsedSelect);
            sql = removeQuotedTableName(sql);
            sql = patchWhereZeroEqualsOne(sql);
            sql = patchAsterisk(sql, parsedSelect.getColumns());
            // System.out.println("EXECUTE " + sql);

            if (ResultSetFactory.DEPLOYABLE.equalsIgnoreCase(schema)) {

                if (sql.contains("COUNT(ID)")) {
                    Show show = new Show(parsedSelect, metadataService);
                    return show.count();
                } else {
                    Show show = new Show(parsedSelect, metadataService);
                    return show.execute();
                }

            }

            if (reconnector.getLicence().supportsLimitedLicence()) {
                statement.setMaxRows(50);
            }

            Integer oldBatchSize = 2000;
            if (reconnector.getQueryOptions() != null) {
                oldBatchSize = reconnector.getQueryOptions().getBatchSize();
            }

            try {
                reconnector.setQueryOptions(statement.getFetchSize());
                QueryResult qr = reconnector.query(sql);

                return new SfResultSet(statement, reconnector, qr, parsedSelects);

            } finally {
                reconnector.setQueryOptions(oldBatchSize);
            }

        } catch (ConnectionException e) {
            throw new SQLException(e);

        } catch (SQLException e) {
            throw e;

        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private String patchWhereZeroEqualsOne(String sql) {
        return replace(sql, " ID = null", " 0 = 1");
    }

    // Fix count(*) and select *
    private String patchAsterisk(String sql, List<ParsedColumn> columnsInSql) throws SQLException {
        boolean countDetected = false;
        boolean starDetected = false;

        for (ParsedColumn parsedColumn : columnsInSql) {
            if (parsedColumn.isFunction() && parsedColumn.getFunctionName().equalsIgnoreCase("count")) {
                countDetected = true;
            } else if (parsedColumn.getName().equals("*")) {
                starDetected = true;
            }
        }

        if (countDetected) {
            sql = sql.replaceAll("COUNT \\( \\* \\)", "COUNT(ID)");
            sql = sql.replaceAll("count \\( \\* \\)", "count(ID)");
            sql = sql.replaceAll("COUNT \\(\\)", "COUNT(ID)");
            sql = sql.replaceAll("count \\(\\)", "count(ID)");
            sql = sql.replaceAll("count \\( \\)", "count(ID)");
            sql = sql.replaceAll("COUNT \\( \\)", "count(ID)");

            // sql = sql.replaceAll("\\. \\*", "count(ID)");
        }

        if ((columnsInSql.size() == 1) && (starDetected)) {
            SfConnection conn = (SfConnection) statement.getConnection();

            StringBuilder sb = new StringBuilder();
            columnsInSql.clear();

            ResultSet columnsRs = conn.getMetaData().getColumns("", schema, table, null);
            while (columnsRs.next()) {
                String col = columnsRs.getString("COLUMN_NAME");
                if (columnsInSql.size() > 0) {
                    sb.append(",");
                }
                sb.append(col);
                columnsInSql.add(new ParsedColumn(col.toUpperCase()));
            }
            columnsRs.close();
            sql = sql.replace("*", sb.toString());
        }
        return sql;
    }

    // DBVisualizer likes to put quotes around the table name
    // for no obvious reason. This undoes that, kinda crudely....
    private String removeQuotedTableName(String sql) {
        return replace(sql, "from " + table, "from \"" + table + "\"");
    }

    // SQL Workbench likes to put quotes around some column names, like "Type",
    // for no obvious reason. This undoes that, kinda crudely....
    private String removeQuotedColumns(String sql, ParsedSelect parsedSelect) {
        for (ParsedColumn parsedColumn : parsedSelect.getColumns()) {
            sql = replace(sql, parsedColumn.getName(), "\"" + parsedColumn.getName() + "\"");
        }
        return sql;
    }


    private String replace(String sql, String replace, String check) {
        String upper = sql.toUpperCase();
        check = check.toUpperCase();
        int pos = upper.indexOf(check);
        if (pos != -1) {
            sql = sql.substring(0, pos) + replace + sql.substring(pos + check.length());
        }
        return sql;
    }


}
