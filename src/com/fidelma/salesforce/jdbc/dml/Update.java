package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.misc.SimpleParser;
import com.sforce.soap.partner.*;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 26/03/11
 * Time: 7:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class Update {

    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private PartnerConnection pc;

    public Update(SimpleParser al, ResultSetFactory metaDataFactory, PartnerConnection pc) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.pc = pc;
    }

    public int execute() throws Exception {
        LexicalToken token;
        int count;
        String table = al.getToken().getValue();

//        Map<String, Integer> columnToDatatype = new HashMap<String, Integer>();
//        ResultSet columnsRs = sfConnection.getMetaData().getColumns(null, null, table, null);
//        while (columnsRs.next()) {
//            String column = columnsRs.getString("COLUMN_NAME").toUpperCase();
//            int dataType = columnsRs.getInt("DATA_TYPE");
//            columnToDatatype.put(column, dataType);
//        }
        al.read("SET");
        token = al.getToken();
        String whereClause = "";

        Map<String, Object> values = new HashMap<String, Object>();

        Table tableData = metaDataFactory.getTable(table);

        while (token != null) {
            String column = token.getValue();
            al.read("=");
            LexicalToken value = al.getToken();

            if (!column.equalsIgnoreCase("Id")) {
                metaDataFactory.getTable(table).getColumn(column);
//                Integer dataType = columnToDatatype.get(column.toUpperCase());
                Integer dataType = metaDataFactory.lookupJdbcType(tableData.getColumn(column).getType());
                assert dataType != null;
                System.out.println("Data type is " + dataType + " for " + column);
                values.put(column.toUpperCase(), value.getValue());
            }

            token = al.getToken();  // comma, or WHERE or null (end of statement)
            if (token != null) {
                if (token.getValue().equalsIgnoreCase("WHERE")) {
                    while (token != null) {
                        if (token.getType().equals(LexicalToken.Type.STRING)) {
                            whereClause += "'";
                        }
                        System.out.println(token.getType() + " " + token.getValue());
                        whereClause += token.getValue();
                        if (token.getType().equals(LexicalToken.Type.STRING)) {
                            whereClause += "'";
                        }

                        whereClause += " ";
                        token = al.getToken();
                    }
                    break;
                } else if (token.getValue().equalsIgnoreCase(",")) {
                    token = al.getToken();
                } else {
                    throw new SQLException("Expected WHERE or COMMA");
                }
            }
        }

        QueryResult qr;
        try {
            StringBuilder readSoql = new StringBuilder();
            readSoql.append("select Id ");
            readSoql.append(" from ").append(table).append(" ").append(whereClause);

            qr = this.pc.query(readSoql.toString());

            SObject[] sObjects = qr.getRecords();
            SObject[] update = new SObject[sObjects.length];

            for (int i = 0; i < sObjects.length; i++) {
                SObject sObject = new SObject();
                update[i] = sObject;
                sObject.setType(table);
                sObject.setId(sObjects[i].getId());

                for (String key : values.keySet()) {

                    metaDataFactory.getTable(table).getColumn(key);
//                Integer dataType = columnToDatatype.get(column.toUpperCase());
                    Integer dataType = metaDataFactory.lookupJdbcType(tableData.getColumn(key).getType());

//                    Integer datatype = columnToDatatype.get(key.toUpperCase());


                    Object value = metaDataFactory.dataTypeConvert((String) values.get(key), dataType);

                    sObject.setField(key, value);
                }
            }

            SaveResult[] sr = this.pc.update(update); // TODO: Handle errors
            for (SaveResult saveResult : sr) {
                System.out.println("UPDATE OK=" + saveResult.isSuccess());
                if (!saveResult.isSuccess()) {
                    com.sforce.soap.partner.Error[] errors = saveResult.getErrors();
                    for (Error error : errors) {
                        System.out.println("ERROR: " + error.getMessage());
                    }
                }
            }
            count = qr.getSize();

        } catch (ConnectionException e) {
            throw new SQLException(e);
        }
        return count;
    }

}
