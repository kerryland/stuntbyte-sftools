package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.SfResultSet;
import com.fidelma.salesforce.jdbc.SfStatement;
import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.parse.ParsedColumn;
import com.fidelma.salesforce.parse.ParsedSelect;
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

            List<ParsedSelect> parsedSelects = la.extractColumnsFromSoql();
            if (parsedSelects.size() > 1) {
                throw new SQLFeatureNotSupportedException("Parent --> Child subqueries not supported via JDBC");
            }
            ParsedSelect parsedSelect = parsedSelects.get(parsedSelects.size() - 1);
            sql = parsedSelect.getParsedSql();

            table = parsedSelect.getDrivingTable();

            sql = removeQuotedColumns(sql, parsedSelect);
            sql = removeQuotedTableName(sql);
            sql = patchWhereZeroEqualsOne(sql);
            sql = patchCountStar(sql, parsedSelect.getColumns());
            System.out.println("EXECUTE " + sql);

            Integer oldBatchSize = 2000;
            if (pc.getQueryOptions() != null) {
                oldBatchSize = pc.getQueryOptions().getBatchSize();
            }

            try {
                pc.setQueryOptions(statement.getFetchSize());
                QueryResult qr = pc.query(sql);

                return new SfResultSet(statement, pc, qr, parsedSelects);

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

    private String patchCountStar(String sql, List<ParsedColumn> columnsInSql) throws SQLException {
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
            Table t = conn.getMetaDataFactory().getTable(table);
            List<Column> cols = t.getColumns();
            StringBuilder sb = new StringBuilder();
            columnsInSql.clear();
            for (Column col : cols) {
                if (columnsInSql.size() > 0) {
                    sb.append(",");
                }
                sb.append(col.getName());
                columnsInSql.add(new ParsedColumn(col.getName().toUpperCase()));
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
    private String removeQuotedColumns(String sql, ParsedSelect parsedSelect) {
        String upper = sql.toUpperCase();
        for (ParsedColumn parsedColumn : parsedSelect.getColumns()) {
            sql = replace(sql, parsedColumn.getName(), "\"" + parsedColumn.getName() + "\"");
        }
        return sql;
    }

    private String removeAsColumns(String sql, ParsedSelect parsedSelect) {
        String upper = sql.toUpperCase();
        String freshSql = sql;
        for (ParsedColumn parsedColumn : parsedSelect.getColumns()) {
            System.out.println("Parsed column " + parsedColumn.getName() + " has alias " + parsedColumn.getAliasName());
            if (parsedColumn.getAliasName() != null) {
                String replaceMe = " AS " + parsedColumn.getAliasName().toUpperCase();
                int asPos = upper.indexOf(replaceMe);
                assert asPos != -1;
                freshSql = freshSql.substring(0, asPos) + sql.substring(asPos + replaceMe.length());
            }
        }
        System.out.println("Parsed to " + freshSql);
        return freshSql;
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
