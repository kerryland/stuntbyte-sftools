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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class Insert {

    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private Reconnector reconnector;
    private String generatedId;

    public Insert(SimpleParser al,
                  ResultSetFactory metaDataFactory,
                  Reconnector reconnector) {

        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.reconnector = reconnector;
    }

    public int execute(Boolean batchMode, List<SObject> batchSObjects) throws SQLException {
        try {
            SObject sObject = convertSqlToSobject();

            if (batchMode) {
                batchSObjects.add(sObject);
            } else {
                saveSObjects(new SObject[]{sObject});
            }
        } catch (SQLException e) {
            throw e;

        } catch (Exception e) {
            throw new SQLException(e);
        }
        return 1;
    }


    public int saveSObjects(SObject[] sObjects) throws SQLException {
        try {
            SaveResult[] sr = reconnector.create(sObjects);
            int row = 0;
            for (SaveResult saveResult : sr) {
                if (!saveResult.isSuccess()) {
                    com.sforce.soap.partner.Error[] errors = saveResult.getErrors();
                    StringBuilder sb = new StringBuilder();
                    for (Error error : errors) {
                        sb.append(error.getMessage()).append(". ");
                    }
                    throw new SQLException(sb.toString());
                } else {
                    generatedId = saveResult.getId();
                    sObjects[row++].setId(generatedId);
                }
            }
            return sr.length;
        } catch (ConnectionException e) {
            throw new SQLException(e);
        }

    }

    private SObject convertSqlToSobject() throws Exception {
        LexicalToken token;
        al.read("INTO");
        String table = al.getToken().getValue();

        token = al.getToken("(");
        token = al.getToken();

        List<String> columns = new ArrayList<String>();
        while (token != null && !token.getValue().equals(")")) {
            String column = token.getValue();
            columns.add(column);

            // Comma or )
            token = al.getToken();
            if (token.getValue().equals(")")) {
                break;
            } else if (token.getValue().equals(",")) {
                token = al.getToken();
                continue;
            } else {
                throw new SQLException("Unexpected token " + token.getValue() + " in " +
                        al.getCommandString());
            }
        }

        al.read("values");
        al.read("(");
        token = al.getToken();

        List<String> values = new ArrayList<String>();
        while (token != null && !token.getValue().equals(")")) {
            String value = token.getValue();
            values.add(value);

            // Comma or )
            token = al.getToken();
            if (token == null) {
                throw new SQLException("Unexpected end of command: " + al.getCommandString());
            } else if (token.getValue().equals(")")) {
                break;
            } else if (token.getValue().equals(",")) {
                token = al.getToken();
            } else {
                throw new SQLException("Unexpected token " + token.getValue() + " in " + al.getCommandString());
            }
        }

        if (columns.size() != values.size()) {
            throw new SQLException("Number of columns does not match number of values: " + al.getCommandString());
        }

        SObject sObject = new SObject();
        sObject.setType(table);

        Table tableData = metaDataFactory.getTable(ResultSetFactory.schemaName, table);

        int i = 0;
        List<String> fieldsToNull = new ArrayList<String>();
        for (String key : columns) {
            String val = values.get(i++);
            Integer dataType = ResultSetFactory.lookupJdbcType(tableData.getColumn(key).getType());

            Object value;
            if (val.equalsIgnoreCase("null")) {
                value = null;
                fieldsToNull.add(key);
            } else {
                value = TypeHelper.dataTypeConvert(val, dataType);
            }

            sObject.setField(key, value);
        }

        if (fieldsToNull.size() > 0) {
            String[] nullFields = new String[fieldsToNull.size()];
            fieldsToNull.toArray(nullFields);
            sObject.setFieldsToNull(nullFields);
        }

        return sObject;
    }

    public String getGeneratedId() {
        return generatedId;
    }
}
