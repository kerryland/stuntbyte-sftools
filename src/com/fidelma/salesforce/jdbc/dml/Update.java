package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.parse.SimpleParser;
import com.fidelma.salesforce.misc.TypeHelper;
import com.sforce.soap.partner.*;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class Update {

    private static int MAX_UPDATES_PER_CALL = 200;

    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private PartnerConnection pc;

    public Update(SimpleParser al, ResultSetFactory metaDataFactory, PartnerConnection pc) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.pc = pc;
    }

    public int execute(Boolean batchMode, List<SObject> batchSObjects) throws Exception {
        LexicalToken token;
        String tableName = al.getToken().getValue();

        al.read("SET");
        token = al.getToken();
        String whereClause = "";

        Map<String, Object> values = new HashMap<String, Object>();

        Table table = metaDataFactory.getTable(tableName);
        List<String> whereChunk = new ArrayList<String>();

        while (token != null) {
            String column = token.getValue();
            al.read("=");
            LexicalToken value = al.getToken();

            if (!column.equalsIgnoreCase("Id")) {
                Integer dataType = ResultSetFactory.lookupJdbcType(table.getColumn(column).getType());
                System.out.println("MAPPED TYPE " + column + " " + table.getColumn(column).getType() + " to "+ dataType);
                assert dataType != null;
                values.put(column.toUpperCase(), value.getValue());
            }

            token = al.getToken();  // comma, or WHERE or null (end of statement)
            if (token != null) {
                if (token.getValue().equalsIgnoreCase("WHERE")) {
                    while (token != null) {
                        if (token.getType().equals(LexicalToken.Type.STRING)) {
                            whereClause += "'";
                        }
                        whereClause += token.getValue();
                        whereChunk.add(token.getValue());
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

        int count;
        String rowId = detectSingleRowUpdate(whereChunk);
        if (rowId != null) {
            count = updateSingleRow(batchMode, batchSObjects, tableName, values, table, rowId);
        } else {
            count = updateMultipleRows(batchMode, batchSObjects, tableName, whereClause, values, table);
        }
        return count;
    }


    // Try to determine if the WHERE clause refers to a single row.
    // ie: WHERE ID = 'someid'
    // or WHERE ID in ('someid')
    private String detectSingleRowUpdate(List<String> whereChunk) {
        String id = null;
        if ((whereChunk.size() == 4) &&
                (whereChunk.get(0).equalsIgnoreCase("where")) &&
                (whereChunk.get(1).equalsIgnoreCase("Id")) &&
                (whereChunk.get(2).equals("="))) {
            id = whereChunk.get(3);
        } else if ((whereChunk.size() == 6) &&
                (whereChunk.get(0).equalsIgnoreCase("where")) &&
                (whereChunk.get(1).equalsIgnoreCase("Id")) &&
                (whereChunk.get(2).equalsIgnoreCase("in")) &&
                (whereChunk.get(3).equals("(")) &&
                (whereChunk.get(5).equals(")"))) {
            id = whereChunk.get(4);
        }
        return id;
    }


    private int updateSingleRow(Boolean batchMode, List<SObject> batchSObjects,
                                       String tableName,  Map<String, Object> values,
                                       Table table, String id) throws SQLException, ParseException {

        SObject[] sObjects = new SObject[1];
        sObjects[0] = new SObject();
        sObjects[0].setType(tableName);
        sObjects[0].setId(id);

        storeData(batchMode, batchSObjects, tableName, values, table, sObjects);
        return 1;
    }

    private int updateMultipleRows(Boolean batchMode, List<SObject> batchSObjects,
                                   String tableName, String whereClause, Map<String, Object> values,
                                   Table table) throws SQLException, ParseException {
        QueryResult qr;
        int count = 0;
        try {
            StringBuilder readSoql = new StringBuilder();
            readSoql.append("select Id ");
            readSoql.append(" from ").append(tableName).append(" ").append(whereClause);

            qr = pc.query(readSoql.toString());

            SObjectChunker chunker = new SObjectChunker(MAX_UPDATES_PER_CALL, pc, qr);
            while (chunker.next()) {
                SObject[] sObjects = chunker.nextChunk();
                storeData(batchMode, batchSObjects, tableName, values, table, sObjects);
                count += sObjects.length;
            }

        } catch (ConnectionException e) {
            throw new SQLException(e);
        }
        return count;
    }

    private void storeData(Boolean batchMode, List<SObject> batchSObjects, String tableName, Map<String, Object> values, Table table, SObject[] sObjects) throws SQLException, ParseException {
        SObject[] update = new SObject[sObjects.length];

        for (int i = 0; i < sObjects.length; i++) {
            String id = sObjects[i].getId();

            SObject sObject = new SObject();
            sObject.setType(tableName);
            sObject.setId(id);

            for (String key : values.keySet()) {
                Integer dataType = ResultSetFactory.lookupJdbcType(table.getColumn(key).getType());
                Object value = TypeHelper.dataTypeConvert((String) values.get(key), dataType);
                sObject.setField(key, value);
            }

            if (batchMode) {
                batchSObjects.add(sObject);
            }
            update[i] = sObject;
        }

        if (!batchMode) {
            saveSObjects(update);
        }
    }


    public void saveSObjects(SObject[] update) throws SQLException {
        try {
            SaveResult[] sr = pc.update(update);
            for (SaveResult saveResult : sr) {
                if (!saveResult.isSuccess()) {
                    Error[] errors = saveResult.getErrors();
                    for (Error error : errors) {
                        throw new SQLException(error.getMessage());
                    }
                }
            }
        } catch (ConnectionException e) {
            throw new SQLException(e);
        }


    }
}
