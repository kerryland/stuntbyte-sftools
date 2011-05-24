package com.fidelma.salesforce.database.exporter;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.LoginHelper;
import com.sforce.soap.metadata.MetadataConnection;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * http://www.salesforce.com/us/developer/docs/api_asynch/index_Left.htm#StartTopic=Content/asynch_api_quickstart.htm
 */
public class Exporter {

    private LoginHelper loginHelper;

    public Exporter(LoginHelper loginHelper) {
        this.loginHelper = loginHelper;
    }


    public void createLocalSchema(SfConnection sfConnection, Connection localConnection) throws SQLException {

        ResultSetFactory rsf = sfConnection.getMetaDataFactory();
        List<Table> tables = rsf.getTables();

        Statement stmt = localConnection.createStatement();
        stmt.execute("drop all objects");

        for (Table table : tables) {
            if (table.getType().equals("TABLE")) {
                StringBuilder sb = new StringBuilder();

                String tableName = table.getName();
                if (!tableName.endsWith("__c")) {
                    tableName += "__s";   // Make sure we don't fail on the GROUP table...
                }
                sb.append("create table " + tableName);
                sb.append(" (");

//                Dialect dialect = new SalesforceDialect();
                Dialect dialect = new H2Dialect();

//                System.setProperty("h2.identifiersToUpper","false");


                List<Column> cols = table.getColumns();
                for (Column col : cols) {
                    sb.append(col.getName());

                    Integer jdbcType = ResultSetFactory.lookupJdbcType(col.getType());
                    String typeName = dialect.getTypeName(jdbcType, col.getLength(), col.getPrecision(), col.getScale());
                    sb.append(" ");
                    sb.append(typeName);
                    sb.append(",");
                }
                sb.replace(sb.length(), sb.length(), ")");

                stmt.execute(sb.toString());

            }
        }

        for (Table table : tables) {
            if (table.getType().equals("TABLE")) {
//            if (table.getName().equalsIgnoreCase("ACCOUNT")) {
                StringBuilder sb = new StringBuilder();

                String tableName = table.getName();
                if (!tableName.endsWith("__c")) {
                    tableName += "__s";   // Make sure we don't fail on the GROUP table...
                }

                PreparedStatement pstmt = sfConnection.prepareStatement("select * from " + table.getName());
                ResultSet rs = pstmt.executeQuery();

                StringBuilder columns = new StringBuilder();
                StringBuilder values = new StringBuilder();
                columns.append("insert into ").append(tableName).append(" (");
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    columns.append(rs.getMetaData().getColumnName(i)).append(",");
                    values.append("?,");
                }
                columns.replace(columns.length(), columns.length(), ") values (");
                columns.append(values);
                columns.replace(columns.length(), columns.length(), ")");

                PreparedStatement pinsert= localConnection.prepareStatement(columns.toString());

                while (rs.next()) {
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        pinsert.setObject(i, rs.getObject(i));
                    }
                    pinsert.executeUpdate();
                }
            }
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


    public void exportSalesforceViaBatch() throws Exception {
        LoginHelper.RubbishRestConnection conn = loginHelper.getBulkConnection();

        System.out.println("Go to " + conn.url);
        URL serverAddress = new URL(conn.url + "/job");
        HttpsURLConnection connection = (HttpsURLConnection) serverAddress.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setAllowUserInteraction(false);
        connection.setReadTimeout(10000);
        /*
        X-SFDC-Session: sessionId" -H "Content-Type: application/xml; charset=UTF-8" -d
@create-job.xml
         */
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
                "salesforce@fidelma.com",
                "u9SABqa2dQxG0Y3kqWiJQVEwnYtryr1Ja1");

        Exporter exporter = new Exporter(loginHelper);
//        exporter.exportSalesforce();

    }

}



