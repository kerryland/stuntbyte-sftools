package com.fidelma.salesforce.database.exporter;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.hibernate.SalesforceDialect;
import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.LoginHelper;
import com.sforce.async.AsyncApiException;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.OperationEnum;
import com.sforce.async.RestConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.bind.TypeInfo;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Properties;
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
                    System.out.println(table.getName() + " " + col.getName() + " JDBCTYPE " + jdbcType);

                    String typeName = dialect.getTypeName(jdbcType, col.getLength(), col.getPrecision(), col.getScale());
                    sb.append(" ");
                    sb.append(typeName);
                    sb.append(",");
                }
                sb.replace(sb.length(), sb.length(), ")");

                stmt.execute("drop table if exists " + tableName);
                stmt.execute(sb.toString());

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



