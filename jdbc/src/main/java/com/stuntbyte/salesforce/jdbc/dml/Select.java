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
    private String tableName;
    private String schema;

    public Select(SfStatement statement, MetadataServiceImpl metadataService, Reconnector reconnector) {
        this.statement = statement;
        this.metadataService = metadataService;
        this.reconnector = reconnector;
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

            tableName = parsedSelect.getDrivingTable();
            schema = parsedSelect.getDrivingSchema();

            SfConnection conn = (SfConnection) statement.getConnection();
            ResultSet rs = conn.getMetaData().getTables(ResultSetFactory.catalogName, schema, tableName, null);
            if (!rs.next()) {
                throw new SQLException("Unknown table " + tableName + " in schema " + schema);
            }

            sql = patchWhereZeroEqualsOne(sql);
            sql = patchAsterisk(sql, parsedSelect.getColumns());

            if (ResultSetFactory.DEPLOYABLE.equalsIgnoreCase(schema)) {

                if (sql.contains("COUNT(ID)")) {
                    Show show = new Show(parsedSelect, metadataService);
                    return show.count();
                } else {
                    Show show = new Show(parsedSelect, metadataService);
                    return show.execute();
                }
            }

            Integer oldBatchSize = 2000;
            if (reconnector.getQueryOptions() != null) {
                oldBatchSize = reconnector.getQueryOptions().getBatchSize();
            }

            try {
                reconnector.setQueryOptions(statement.getFetchSize());
                QueryResult qr = reconnector.query(sql);

                return new SfResultSet(tableName, statement, reconnector, qr, parsedSelects);

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
            // Yuck
            sql = sql.replaceAll("COUNT \\( \\* \\)", "COUNT(ID)");
            sql = sql.replaceAll("count \\( \\* \\)", "COUNT(ID)");
            sql = sql.replaceAll("COUNT \\(\\)", "COUNT(ID)");
            sql = sql.replaceAll("count \\(\\)", "COUNT(ID)");
            sql = sql.replaceAll("count \\( \\)", "COUNT(ID)");
            sql = sql.replaceAll("COUNT \\( \\)", "COUNT(ID)");

            // sql = sql.replaceAll("\\. \\*", "count(ID)");
        }

        if ((columnsInSql.size() == 1) && (starDetected)) {
            SfConnection conn = (SfConnection) statement.getConnection();

            StringBuilder sb = new StringBuilder();
            columnsInSql.clear();

            ResultSet columnsRs = conn.getMetaData().getColumns("", schema, tableName, null);
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

    private String replace(String sql, String replace, String check) {
        String upper = sql.toUpperCase();
        check = check.toUpperCase();
        int pos = upper.indexOf(check);
        if (pos != -1) {
            sql = sql.substring(0, pos) + replace + sql.substring(pos + check.length());
        }
        return sql;
    }

    public String getTableName() {
        return tableName;
    }
}
