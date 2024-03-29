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
package com.stuntbyte.salesforce.database.migration;

import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.jdbc.TableEvent;
import com.stuntbyte.salesforce.jdbc.metaforce.Column;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.jdbc.metaforce.Table;
import com.sforce.soap.metadata.MetadataConnection;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

import java.sql.*;
import java.util.*;


/**
 * Export data from Salesforce and push it into a local JDBC database
 * <p/>
 * http://www.salesforce.com/us/developer/docs/api_asynch/index_Left.htm#StartTopic=Content/asynch_api_quickstart.htm
 */
public class Exporter {

    public static String localname(Connection connection, String name) {
        if (connection instanceof SfConnection) {
            return name;
        }
        return "__m_" + name;
    }

    /**
     * Create a H2 schema based on the provided Salesforce instance
     */
    public List<Table> createLocalSchema(SfConnection sfConnection, final Connection localConnection, final String tablePattern) throws SQLException {
        final ResultSetFactory rsf = sfConnection.getMetaDataFactory();

        final Statement stmt = localConnection.createStatement();
        stmt.execute("drop all objects");

        final List<Table> tables = new ArrayList<Table>();

        rsf.getTables(ResultSetFactory.schemaName, tablePattern, new TableEvent() {
            public void onTable(Table table) throws SQLException {
                if ((table.getType().equals("TABLE") &&
                        !table.getName().endsWith("__History") &&  // Can't deal with 'anytype'
                        !table.getName().equalsIgnoreCase("ContentDocumentLink"))               // Implementation restriction: ContentDocumentLink requires a filter by a single Id on ContentDocumentId
                        || table.getName().equalsIgnoreCase("RecordType")
                        || table.getName().equalsIgnoreCase("User")
                        || table.getName().equalsIgnoreCase("UserRole")
                        )
                {
                    StringBuilder sb = new StringBuilder();

                    String tableName = table.getName();
                    sb.append("create table " +  localname(localConnection, tableName));
                    sb.append(" (");

//                Dialect dialect = new SalesforceDialect();
                    Dialect dialect = new H2Dialect();

//                System.setProperty("h2.identifiersToUpper","false");

                    Collection<Column> cols = table.getColumns();
                    for (Column col : cols) {
                        Integer jdbcType = ResultSetFactory.lookupJdbcType(col.getType());
                        if (jdbcType != Types.OTHER) {
                            sb.append(col.getName());


//                            System.out.println("Check " + col.getTable().getName() + "." + col.getName() + " " + col.getName() + " " + jdbcType + " " + col.getType());
                            String typeName = dialect.getTypeName(jdbcType, col.getLength(), col.getPrecision(), col.getScale());
                            sb.append(" ");
                            sb.append(typeName);
                            sb.append(",");
                        } else {
                            System.out.println("ARGH -- Don't know what to do with " + table.getName() + "." + col.getName() + " datatype " + col.getType());
                        }
                    }
                    sb.replace(sb.length(), sb.length(), ")");

                    stmt.execute(sb.toString());

                    tables.add(table);
                }

            }
        });


        return tables;
    }


    /**
     * Copy data from the sfConnection to the localConnection for the tables listed in migrationCriteriaList
     */
    public void downloadData(SfConnection sfConnection, Connection localConnection,
                             List<MigrationCriteria> migrationCriteriaList,
                             Set<String> processedTables) throws SQLException {

        for (MigrationCriteria criteria : migrationCriteriaList) {
            String tableName = criteria.tableName;
            if (!tableName.equalsIgnoreCase("Attachment")) {
                String sql = "select * from " + criteria.tableName + " " + criteria.sql;
                System.out.println(sql);
                PreparedStatement pstmt = sfConnection.prepareStatement(
                        sql);

                try {
                    ResultSet rs = pstmt.executeQuery();

                    copyResultSetToTable(localConnection, tableName, rs, processedTables, null);
                } catch (java.sql.SQLException e) {
                    System.out.println("Salesforce doesn't like to query " + criteria.tableName + ". " + e.getMessage());
                }
            }
        }
    }

    public void copyResultSetToTable(Connection destination,
                                     String tableName,
                                     ResultSet rs,
                                     Set<String> processedTables,
                                     ResultSetCallback resultSetCallback) throws SQLException {

        System.out.println("Saving records to " + tableName);
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        columns.append("insert into ").append(localname(destination, tableName)).append(" (");
        boolean first = true;

        Map<Integer, Integer> sourceToDestColumnMap = new HashMap<Integer, Integer>();

        int destCol = 0;
        for (int col = 1; col <= rs.getMetaData().getColumnCount(); col++) {
            if (resultSetCallback == null || resultSetCallback.shouldInsert(tableName, rs, col, processedTables)) {
                if (!first) {
                    columns.append(",");
                    values.append(",");
                }
                first = false;
                columns.append(rs.getMetaData().getColumnName(col));
                values.append("?");

                sourceToDestColumnMap.put(col, ++destCol);
            }
        }
        columns.append(") values (");
        columns.append(values);
        columns.append(")");

//        System.out.println("KJS3 " + columns.toString());

        PreparedStatement pinsert = destination.prepareStatement(columns.toString());

        List<String> sourceIds = new ArrayList<String>();
        while (rs.next()) {
            for (Integer sourceCol : sourceToDestColumnMap.keySet()) {
                destCol = sourceToDestColumnMap.get(sourceCol);
//                System.out.println("KJS set col " + destCol + " to " + rs.getObject(sourceCol));

                Object value = rs.getObject(sourceCol);
                if (resultSetCallback != null) {
                    value = resultSetCallback.alterValue(tableName, rs.getMetaData().getColumnName(sourceCol), value);
                }
                pinsert.setObject(destCol, value);
            }

            pinsert.addBatch();
            sourceIds.add(rs.getString("Id"));

            if (resultSetCallback != null) {
                resultSetCallback.onRow(rs);
            }

        }
        try {
            pinsert.executeBatch();
            processedTables.add(tableName.toLowerCase());
        } catch (SQLException e) {
            System.out.println("Failed to copy data from " + tableName + " " + e.getMessage());
        }

        if (resultSetCallback != null) {
            resultSetCallback.afterBatchInsert(tableName, sourceIds, pinsert);
        }
    }

    public void cloneSalesforce(MetadataConnection sourceMetaData, boolean cloneData) {
        downloadMetaData(sourceMetaData);
//        if (cloneData) {
//            downloadData(source);
//        }

//        deleteMetaData(destination);
//
//        if (cloneData) {
//            deleteData(destination);
//        }
//
//        uploadMetaData(destination);
//        uploadData(destination);
    }

    private void downloadMetaData(MetadataConnection sourceMetaDataConnection) {


    }


    /*
    http://www.salesforce.com/us/developer/docs/api_asynch/index_Left.htm#StartTopic=Content/asynch_api_reference_jobinfo.htm


    public void exportSalesforceViaBatch() throws Exception {
        LoginHelper.RubbishRestConnection conn = loginHelper.getBulkConnection();

        System.out.println("Go to " + conn.url);
        URL serverAddress = new URL(conn.url + "/job");
        HttpsURLConnection connection = (HttpsURLConnection) serverAddress.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setAllowUserInteraction(false);
        connection.setReadTimeout(10000);
//        X-SFDC-Session: sessionId" -H "Content-Type: application/xml; charset=UTF-8" -d
@create-job.xml
        connection.addRequestProperty("Content-Type", "application/xml; charset=UTF-8");
        connection.addRequestProperty("X-SFDC-Session", conn.sessionId);

        OutputStream out = connection.getOutputStream();


        String msg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<jobInfo\n" +
                "xmlns=\"http://www.force.com/2009/06/asyncapi/dataload\">\n" +
                "<operation>query</operation>\n" +
                "<object>Account</object>\n" +
                "<concurrencyMode>Parallel</concurrencyMode>\n" +
                "<contentType>CSV</contentType>\n" +
                "</jobInfo>";

        out.write(msg.getBytes());
        // Read the response;

        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line + '\n');
            }

            // System.out.println(sb.toString());
        } catch (IOException e) {

            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line + '\n');
            }
            System.out.println("BANG WITH " + sb.toString());
        }

//        connection.connect();


    }

    public static void main(String[] args) throws Exception {

        LoginHelper loginHelper = new LoginHelper("https://login.salesforce.com",
                "kerry@fidelma.com",
                "g2Py8oPzevwDahQQrFnO4GfKHDr9eDhL");
//                "salesforce@fidelma.com",
//                "u9SABqa2dQxG0Y3kqWiJQVEwnYtryr1Ja1");

        Exporter exporter = new Exporter(loginHelper);
        exporter.exportSalesforceViaBatch();

    }
    */
}



