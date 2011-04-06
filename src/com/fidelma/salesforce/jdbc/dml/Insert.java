package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalAnalyzer;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.misc.SimpleParser;
import com.fidelma.salesforce.misc.TypeHelper;
import com.sforce.soap.partner.*;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.sobject.SObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class Insert {

    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private PartnerConnection pc;

    public Insert(SimpleParser al,
                  ResultSetFactory metaDataFactory,
                  PartnerConnection pc) {

        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.pc = pc;
    }

    public int execute() throws SQLException {
        try {
            LexicalToken token;
            al.read("INTO");
            String table = al.getToken().getValue();

            token = al.getToken("(");
            token = al.getToken();

            List<String> columns = new ArrayList<String>();
            while (token != null) {
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
                    throw new SQLException("Unexpected token " + token.getValue());
                }
            }

            al.read("values");
            token = al.getToken("(");
            token = al.getToken();

            List<String> values = new ArrayList<String>();
            while (token != null) {
                String value = token.getValue();
                values.add(value);

                // Comma or )
                token = al.getToken();
                if (token.getValue().equals(")")) {
                    break;
                } else if (token.getValue().equals(",")) {
                    token = al.getToken();
                    continue;
                } else {
                    throw new SQLException("Unexpected token " + token.getValue());
                }
            }

            if (columns.size() != values.size()) {
                throw new SQLException("Number of columns does not match number of values ");
            }

            SObject sObject = new SObject();
            sObject.setType(table);

            Table tableData = metaDataFactory.getTable(table);

            int i = 0;
            for (String key : columns) {
                String val = values.get(i++);
                Integer dataType = metaDataFactory.lookupJdbcType(tableData.getColumn(key).getType());
                Object value = TypeHelper.dataTypeConvert(val, dataType);

                sObject.setField(key, value);
            }

            SaveResult[] sr = pc.create(new SObject[]{sObject});
            for (SaveResult saveResult : sr) {
                if (!saveResult.isSuccess()) {
                    com.sforce.soap.partner.Error[] errors = saveResult.getErrors();
                    StringBuilder sb = new StringBuilder();
                    for (Error error : errors) {
                        sb.append(error.getMessage()).append(". ");
                    }
                    throw new SQLException(sb.toString());
                }
            }
        } catch (SQLException e) {
            throw e;

        } catch (Exception e) {
            throw new SQLException(e);
        }

        return 1;
    }

}