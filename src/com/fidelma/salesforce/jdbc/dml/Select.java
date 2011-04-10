package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.SfResultSet;
import com.fidelma.salesforce.jdbc.SfStatement;
import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.parse.ParseColumn;
import com.fidelma.salesforce.parse.ParseSelect;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.partner.PartnerConnection;
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
    private PartnerConnection pc;
    private String table;

    public Select(SfStatement statement, PartnerConnection pc) {
        this.statement = statement;
        this.pc = pc;
    }

    public ResultSet execute(String sql) throws SQLException {
        try {
            SimpleParser la = new SimpleParser(sql);

            List<ParseSelect> parseSelects = la.extractColumnsFromSoql();
            if (parseSelects.size() > 1) {
                throw new SQLFeatureNotSupportedException("Parent --> Child subqueries not supported via JDBC");
            }
            ParseSelect parseSelect = parseSelects.get(parseSelects.size() - 1);

            table = parseSelect.getDrivingTable();

            sql = removeQuotedColumns(sql, parseSelect);
            sql = removeQuotedTableName(sql);
            sql = patchWhereZeroEqualsOne(sql);
            sql = patchCountStar(sql, parseSelect.getColumns());

            Integer oldBatchSize = 2000;
            if (pc.getQueryOptions() != null) {
                oldBatchSize = pc.getQueryOptions().getBatchSize();
            }

            try {
                pc.setQueryOptions(statement.getFetchSize());
                QueryResult qr = pc.query(sql);

                return new SfResultSet(statement, pc, qr, parseSelects);

            } finally {
                pc.setQueryOptions(oldBatchSize);
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

    private String patchCountStar(String sql, List<ParseColumn> columnsInSql) throws SQLException {
        boolean countDetected = false;
        boolean starDetected = false;

        for (ParseColumn parseColumn : columnsInSql) {
            if (parseColumn.isFunction() && parseColumn.getFunctionName().equalsIgnoreCase("count")) {
                countDetected = true;
            } else if (parseColumn.getName().equals("*")) {
                starDetected = true;
            }
        }

        if (countDetected) {
            sql = sql.replaceAll("COUNT\\(\\*\\)", "COUNT(ID)");
            sql = sql.replaceAll("count\\(\\*\\)", "count(ID)");
            sql = sql.replaceAll("COUNT\\(\\)", "COUNT(ID)");
            sql = sql.replaceAll("count\\(\\)", "count(ID)");
        }

        if ((columnsInSql.size() == 1) && (starDetected)) {
            SfConnection conn = (SfConnection) statement.getConnection();
            Table t = conn.getMetaDataFactory().getTable(table);
            List<Column> cols = t.getColumns();
            StringBuilder sb = new StringBuilder();
            columnsInSql.clear();
            for (Column col : cols) {
                if (columnsInSql.size() > 0) {
                    sb.append(",");
                }
                sb.append(col.getName());
                columnsInSql.add(new ParseColumn(col.getName().toUpperCase()));
            }
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
    private String removeQuotedColumns(String sql, ParseSelect parseSelect) {
        String upper = sql.toUpperCase();
        for (ParseColumn parseColumn : parseSelect.getColumns()) {
            sql = replace(sql, parseColumn.getName(), "\"" + parseColumn.getName() + "\"");
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
