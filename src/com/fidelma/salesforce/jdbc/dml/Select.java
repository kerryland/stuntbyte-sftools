package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.SfResultSet;
import com.fidelma.salesforce.jdbc.SfStatement;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.misc.SimpleParser;
import com.sforce.soap.partner.CallOptions_element;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryOptions_element;
import com.sforce.soap.partner.QueryResult;
import com.sforce.ws.ConnectionException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 */
public class Select {

    private SfStatement statement;
    private PartnerConnection pc;

    public Select(SfStatement statement, PartnerConnection pc) {
        this.statement = statement;
        this.pc = pc;
    }

    public ResultSet execute(String sql) throws SQLException {
        try {
            Set<String> columnsInSql = extractColumnsFromSoql(sql);
            System.out.println("SOQL: " + sql + " found " + columnsInSql.size());
            for (String s : columnsInSql) {
                System.out.println("COL IN SQL: " + s);
            }

            String upper = sql.toUpperCase();
            if (upper.contains("COUNT(*)")) {
                sql = patchCountStar(sql);
            }

            Integer oldBatchSize = 2000;
            if (pc.getQueryOptions() != null) {
                oldBatchSize = pc.getQueryOptions().getBatchSize();
            }

            try {
                pc.setQueryOptions(statement.getFetchSize());
                QueryResult qr = pc.query(sql);
                return new SfResultSet(pc, qr, columnsInSql, statement.getMaxRows());

            } finally {
                pc.setQueryOptions(oldBatchSize);
            }

        } catch (ConnectionException e) {
            throw new SQLException(e);
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private Set<String> extractColumnsFromSoql(String sql) throws Exception {
        Set<String> result = new HashSet<String>();

        // TODO: This is too primitive. COUNT() is one column!
        SimpleParser la = new SimpleParser(sql);
        la.getToken("SELECT");
        LexicalToken token = la.getToken();

        // TODO: Handle
        // COUNT()
        // count_distinct
        // min
        // max
        // sum
        // toLabel()
        // convertCurrency()
        System.out.println("SELECT TOKEN=" + token);

        while ((token != null) && (!token.getValue().equalsIgnoreCase("from"))) {
            result.add(token.getValue().toUpperCase());
            token = la.getToken();
        }
        return result;
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
            System.out.println("FROM " + start + " TO " + countPos);

            fixed.append(sql.substring(start, countPos));
            fixed.append("count()");
            start = countPos + 8;
            countPos = detectCountStar(sql, start);
        }
        fixed.append(sql.substring(start));
        System.out.println(fixed.toString());
        return fixed.toString();
    }
}
