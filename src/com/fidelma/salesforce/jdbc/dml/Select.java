package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.SfResultSet;
import com.fidelma.salesforce.jdbc.SfStatement;
import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.parse.ParseColumn;
import com.fidelma.salesforce.parse.ParseSelect;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.ws.ConnectionException;

import java.sql.ResultSet;
import java.sql.SQLException;
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

            ParseSelect parseSelect = la.extractColumnsFromSoql();

            table = parseSelect.getDrivingTable();
            List<ParseColumn> columnsInSql = parseSelect.getColumns();

            sql = removeQuotedTableName(sql);
            sql = patchCountStar(sql, columnsInSql);

            Integer oldBatchSize = 2000;
            if (pc.getQueryOptions() != null) {
                oldBatchSize = pc.getQueryOptions().getBatchSize();
            }

            try {

                pc.setQueryOptions(statement.getFetchSize());
                QueryResult qr = pc.query(sql);

                return new SfResultSet(statement, pc, qr, columnsInSql);

            } finally {
                pc.setQueryOptions(oldBatchSize);
            }

        } catch (ConnectionException e) {
            throw new SQLException(e);
        } catch (Exception e) {
            throw new SQLException(e);
        }
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
        String upper = sql.toUpperCase();
        String check = "FROM \"" + table.toUpperCase() + "\"";
        int pos = upper.indexOf(check);
        if (pos != -1) {
            sql = sql.substring(0, pos) + " from " + table + sql.substring(pos + check.length());
        }
        return sql;
    }

}
