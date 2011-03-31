package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.SfResultSet;
import com.fidelma.salesforce.jdbc.SfStatement;
import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.misc.SimpleParser;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.ws.ConnectionException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            Set<String> columnsInSql = extractColumnsFromSoql(la);

//            System.out.println("SOQL: " + sql + " found " + columnsInSql.size());
//            for (String s : columnsInSql) {
//                System.out.println("COL IN SQL: " + s);
//            }

            boolean oldTypeCount = false;

            if ((columnsInSql.size() == 1) && (columnsInSql.contains("COUNT"))) {
                String upper = sql.toUpperCase();
                if (upper.contains("COUNT(*)")) {
                    sql = patchCountStar(sql);
                    oldTypeCount = true;
                } else if (upper.contains("COUNT()")) {
                    oldTypeCount = true;
                }
            }

            if ((columnsInSql.size() == 1) && (columnsInSql.contains("*"))) {
                SfConnection conn = (SfConnection) statement.getConnection();
                Table t = conn.getMetaDataFactory().getTable(table);
                List<Column> cols = t.getColumns();
                StringBuilder sb = new StringBuilder("Id");
                columnsInSql = new HashSet<String>();
                columnsInSql.add("Id".toUpperCase());
                for (Column col : cols) {
                    if ((!col.getName().equalsIgnoreCase("Id")) &&
                            (!col.getName().equalsIgnoreCase("Type"))) {
                        sb.append(",").append(col.getName());
                        columnsInSql.add(col.getName().toUpperCase());
                    }
                }
                sql = sql.replace("*", sb.toString());
            }

            Integer oldBatchSize = 2000;
            if (pc.getQueryOptions() != null) {
                oldBatchSize = pc.getQueryOptions().getBatchSize();
            }

            try {
                pc.setQueryOptions(statement.getFetchSize());
                QueryResult qr = pc.query(sql);
                return new SfResultSet(pc, qr, columnsInSql, statement.getMaxRows(), oldTypeCount);

            } finally {
                pc.setQueryOptions(oldBatchSize);
            }

        } catch (ConnectionException e) {
            throw new SQLException(e);
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private Set<String> extractColumnsFromSoql(SimpleParser la) throws Exception {
        Set<String> result = new HashSet<String>();

        la.getToken("SELECT");
        LexicalToken token = la.getToken();

        while ((token != null) && (!token.getValue().equalsIgnoreCase("FROM"))) {
            if (token.getValue().equals("(")) {
                token = swallowUntilMatchingBracket(la);
            } else {
                String x = token.getValue().trim();
                if (x.length() > 0) {
                    result.add(x.toUpperCase());
                }
                token = la.getToken();
            }
        }

        result = handleColumnAlias(result, la, token);

        return result;
    }

    private Set<String> handleColumnAlias(Set<String> result, SimpleParser la, LexicalToken token) throws Exception {
        if ((token != null) && (token.getValue().equalsIgnoreCase("from"))) {
            table = la.getValue();
            if (table != null) {
                String alias = la.getValue();
                if (alias != null) {
                    String prefix = alias.toUpperCase() + ".";
                    Set<String> freshResult = new HashSet<String>();
                    for (String columnName : result) {
                        if (columnName.startsWith(prefix)) {
                            String x = columnName.substring(prefix.length()).trim();
                            if (x.length() != 0) {
                                freshResult.add(x);
                            }

                        } else {
                            if (!columnName.equals("")) {
                                freshResult.add(columnName);
                            }
                        }
                    }
                    result = freshResult;
                }
            }
        }
        return result;
    }

    private LexicalToken swallowUntilMatchingBracket(SimpleParser la) throws Exception {
        LexicalToken token = la.getToken();

        while ((token != null) && (!token.getValue().equals(")"))) {
            if (token.getValue().equals("(")) {
                token = swallowUntilMatchingBracket(la);
            } else {
                token = la.getToken();
            }
        }
        if (token != null) {
            token = la.getToken();
        }
        return token;
    }

    // Convert the SQL-normal "count(*)" to Salesforce's "count()"
    private int detectCountStar(String sql, int start) {
        int countPos = sql.indexOf("count(*)", start);
        if (countPos == -1) {
            countPos = sql.indexOf("COUNT(*)", start);
        }
        return countPos;

    }

    private String patchCountStar(String sql) {
        StringBuilder fixed = new StringBuilder();

        int start = 0;
        int countPos = detectCountStar(sql, start);
        while (countPos != -1) {
            fixed.append(sql.substring(start, countPos));
            fixed.append("count()");
            start = countPos + 8;
            countPos = detectCountStar(sql, start);
        }
        fixed.append(sql.substring(start));
        return fixed.toString();
    }
}
