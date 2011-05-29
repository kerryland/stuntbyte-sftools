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
        int count=0;
        String table = al.getToken().getValue();

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
                Integer dataType = metaDataFactory.lookupJdbcType(tableData.getColumn(column).getType());
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

            qr = pc.query(readSoql.toString());

//            SObject[] input = qr.getRecords();

            SObjectChunker chunker = new SObjectChunker(MAX_UPDATES_PER_CALL, pc, qr);
            while (chunker.next()) {
                SObject[] sObjects = chunker.nextChunk();
                SObject[] update = new SObject[sObjects.length];

                for (int i = 0; i < sObjects.length; i++) {
                    SObject sObject = new SObject();
                    update[i] = sObject;
                    sObject.setType(table);
                    sObject.setId(sObjects[i].getId());

                    for (String key : values.keySet()) {
                        Integer dataType = metaDataFactory.lookupJdbcType(tableData.getColumn(key).getType());
                        Object value = TypeHelper.dataTypeConvert((String) values.get(key), dataType);
                        sObject.setField(key, value);
                    }

                    if (batchMode) {
                        batchSObjects.add(sObject);
                    }
                }

                if (!batchMode) {
                    saveSObjects(update);
                }
                count += sObjects.length;
            }

        } catch (ConnectionException e) {
            throw new SQLException(e);
        }
        return count;
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
