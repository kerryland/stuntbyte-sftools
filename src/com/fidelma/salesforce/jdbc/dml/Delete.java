package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.misc.SimpleParser;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.partner.fault.ApiQueryFault;
import com.sforce.soap.partner.fault.InvalidFieldFault;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class Delete {

    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private PartnerConnection pc;

    public Delete(SimpleParser al, ResultSetFactory metaDataFactory, PartnerConnection pc) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.pc = pc;
    }

    public int execute() throws Exception {
        al.read("FROM");

        LexicalToken token;
        int count;
        String table = al.getToken().getValue();
        token = al.getToken();

        if ((token != null) && (!token.getValue().equalsIgnoreCase("WHERE"))) {
            throw new SQLException("Expected WHERE, not " + token.getValue());
        }

        String whereClause = "";
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

        // TODO: Handle large deletes!
        QueryResult qr;
        String selectPrefix = "select id";
        try {
            StringBuilder readSoql = new StringBuilder(selectPrefix);
            readSoql.append(" from ").append(table).append(" ").append(whereClause);

            System.out.println("DELETE BASED ON " + readSoql.toString());
            qr = pc.query(readSoql.toString());

            SObject[] sObjects = qr.getRecords();

            List<String> chunk = new ArrayList<String>();

            for (int i = 0; i < sObjects.length; i++) {
                chunk.add(sObjects[i].getId());
                if (chunk.size() == 200) {
                    deleteChunk(chunk);
                }
            }
            deleteChunk(chunk);

            count = qr.getSize();

        } catch (ApiFault e) {
            // TODO: Build PartnerConnection delegate, and wrap this stuff
            String msg = e.getExceptionMessage();
            if (msg != null) {
                msg = msg.replace(selectPrefix, "   delete");
            }
            throw new SQLException(msg, e.getExceptionCode().toString());

        } catch (ConnectionException e) {

            throw new SQLException(e);
        }
        return count;
    }

    private void deleteChunk(List<String> chunk) throws ConnectionException {
        String[] delete = new String[chunk.size()];
        chunk.toArray(delete);
        DeleteResult[] sr = pc.delete(delete); // TODO: Handle errors
        for (DeleteResult deleteResult : sr) {
//            System.out.println("DELETE OK=" + deleteResult.isSuccess());
            if (!deleteResult.isSuccess()) {
                Error[] errors = deleteResult.getErrors();
                for (Error error : errors) {
                    System.out.println("DELETE ERROR: " + error.getMessage());
                }
            }
        }
        chunk.clear();
    }


}
