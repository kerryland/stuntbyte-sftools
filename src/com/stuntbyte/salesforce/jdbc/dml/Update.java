package com.stuntbyte.salesforce.jdbc.dml;

import com.stuntbyte.salesforce.jdbc.metaforce.Column;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.jdbc.metaforce.Table;
import com.stuntbyte.salesforce.jdbc.sqlforce.LexicalToken;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.stuntbyte.salesforce.parse.SimpleParser;
import com.stuntbyte.salesforce.misc.TypeHelper;
import com.sforce.soap.partner.*;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import org.mvel2.MVEL;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class Update {

    private static int MAX_UPDATES_PER_CALL = 100; // We hit "Too many fields describes" at 200.

    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private Reconnector reconnector;

    public Update(SimpleParser al, ResultSetFactory metaDataFactory, Reconnector reconnector) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.reconnector = reconnector;
    }


    public int execute(Boolean batchMode, List<SObject> batchSObjects) throws Exception {
        LexicalToken token;
        String tableName = al.getToken().getValue();

        al.read("SET");
        token = al.getToken();
        String whereClause = "";

        Map<String, ExpressionHolder> values = new HashMap<String, ExpressionHolder>();

        Table table = metaDataFactory.getTable(tableName);
        List<String> whereChunk = new ArrayList<String>();

        while (token != null) {
            String column = token.getValue();
            al.read("=");

            ExpressionHolder expressionHolder = new ExpressionHolder();
            token = assembleExpression(al, expressionHolder, table);

            if (!column.equalsIgnoreCase("Id")) {
                Integer dataType = ResultSetFactory.lookupJdbcType(table.getColumn(column).getType());
                assert dataType != null;
                values.put(column.toUpperCase(), expressionHolder);
            }

//            token = al.getToken();  // comma, or WHERE or null (end of statement)
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
                    throw new SQLException("Expected WHERE or COMMA, not " + token.getValue() );
                }
            }
        }

        int count = -1;

        boolean needMultipleUpdate = true;

        if (noReferenceToOtherColumns(values.values())) {
            String rowId = detectSingleRowUpdate(whereChunk);
            if (rowId != null) {
                count = updateSingleRow(batchMode, batchSObjects, tableName, values, table, rowId);
                needMultipleUpdate = false;
            }
        }
        if (needMultipleUpdate) {
            count = updateMultipleRows(batchMode, batchSObjects, tableName, whereClause, values, table);
        }

        return count;
    }

    private boolean noReferenceToOtherColumns(Collection<ExpressionHolder> values) {
        boolean result = true;
        for (ExpressionHolder value : values) {
            if (value.referencedColumns.size() > 0) {
                result = false;
                break;
            }
        }
        return result;
    }


    private class ExpressionHolder {
        String expression;
        List<Column> referencedColumns = new ArrayList<Column>();
        Serializable compiledExpression;
    }


    private LexicalToken assembleExpression(SimpleParser al, ExpressionHolder expressionHolder, Table table) throws Exception {
        LexicalToken value = al.getToken();

        StringBuilder expression = new StringBuilder();

        int bracketBalance = 0;

        do {
            if (value.getType().equals(LexicalToken.Type.STRING)) {
                expression.append("'").append(value.getValue()).append("'");

            } else if (value.getType().equals(LexicalToken.Type.IDENTIFIER)) {

                String identifier = value.getValue();
                expression.append(identifier.toLowerCase());

                try {
                    Column col = table.getColumn(identifier);
                    expressionHolder.referencedColumns.add(col);
                } catch (SQLException e) {
                    // Not a big deal -- it's just not a column
                }

            } else {
                expression.append(value.getValue());
            }

            if (value.getValue().equals("(")
                    || value.getValue().equals("[")
                    || value.getValue().equals("{")) {
                bracketBalance++;
            } else if (value.getValue().equals(")")
                    || value.getValue().equals("]")
                    || value.getValue().equals("}")) {
                bracketBalance--;
            }
            expression.append(" ");

            value = al.getToken();

        } while (value != null && (bracketBalance !=0 || (!value.getValue().equals(",") && !value.getValue().equalsIgnoreCase("where"))));

        expressionHolder.expression = expression.toString();
        expressionHolder.compiledExpression = MVEL.compileExpression(expressionHolder.expression);

        return value;
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
                                String tableName, Map<String, ExpressionHolder> values,
                                Table table, String id) throws SQLException, ParseException {

        SObject[] sObjects = new SObject[1];
        sObjects[0] = new SObject();
        sObjects[0].setType(tableName);
        sObjects[0].setId(id);

        storeData(batchMode, batchSObjects, tableName, values, table, sObjects, new HashSet<String>(0));
        return 1;
    }

    private int updateMultipleRows(Boolean batchMode, List<SObject> batchSObjects,
                                   String tableName, String whereClause, Map<String, ExpressionHolder> values,
                                   Table table) throws SQLException, ParseException {
        QueryResult qr;
        int count = 0;
        try {
            Set<String> referencedColumns = new HashSet<String>();
            referencedColumns.add("Id");
            for (ExpressionHolder expressionHolder : values.values()) {
                for (Column referencedColumn : expressionHolder.referencedColumns) {
                    referencedColumns.add(referencedColumn.getName());
                }
            }

            StringBuilder readSoql = new StringBuilder();
            readSoql.append("select ");

            for (String referencedColumn : referencedColumns) {
                readSoql.append(referencedColumn).append(",");
            }
            readSoql.deleteCharAt(readSoql.lastIndexOf(","));

            readSoql.append(" from ").append(tableName).append(" ").append(whereClause);

            qr = reconnector.query(readSoql.toString());

            SObjectChunker chunker = new SObjectChunker(MAX_UPDATES_PER_CALL, reconnector, qr);
            while (chunker.next()) {
                SObject[] selectedObjects = chunker.nextChunk();
                storeData(batchMode, batchSObjects, tableName, values, table, selectedObjects, referencedColumns);
                count += selectedObjects.length;
            }

        } catch (ConnectionException e) {
            throw new SQLException(e);
        }
        return count;
    }

    private void storeData(Boolean batchMode,
                           List<SObject> batchSObjects,
                           String tableName,
                           Map<String, ExpressionHolder> values,
                           Table table,
                           SObject[] selectedObjects,
                           Set<String> referencedColumns) throws SQLException, ParseException {

        SObject[] update = new SObject[selectedObjects.length];

        for (int i = 0; i < selectedObjects.length; i++) {

            Map vars = new HashMap();

            String id = selectedObjects[i].getId();

            for (String referencedColumn : referencedColumns) {
                vars.put(referencedColumn.toLowerCase(), selectedObjects[i].getField(referencedColumn));
            }

            SObject sObject = new SObject();
            sObject.setType(tableName);
            sObject.setId(id);
            List<String> fieldsToNull = new ArrayList<String>();

            for (String key : values.keySet()) {
                Integer dataType = ResultSetFactory.lookupJdbcType(table.getColumn(key).getType());

                ExpressionHolder expressionHolder = values.get(key);
                Object value = MVEL.executeExpression(expressionHolder.compiledExpression, vars);

                if (value == null) {
                    // Not that it seems to matter...
                    fieldsToNull.add(key);
                    sObject.setField(key, null);
                } else {
                    value = TypeHelper.dataTypeConvert(value.toString(), dataType);
                    sObject.setField(key, value);
                }
            }

            if (fieldsToNull.size() > 0) {
                String[] nullFields = new String[fieldsToNull.size()];
                fieldsToNull.toArray(nullFields);
                sObject.setFieldsToNull(nullFields);
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
            SaveResult[] sr = reconnector.update(update);
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
