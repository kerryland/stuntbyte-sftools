package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

import java.sql.SQLException;

/**
 */
public class Delete {

    private static int MAX_DELETES_PER_CALL = 200;
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
        int count=0;
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

            qr = pc.query(readSoql.toString());

            SObjectChunker chunker = new SObjectChunker(MAX_DELETES_PER_CALL, pc, qr);
            while (chunker.next()) {
                SObject[] sObjects = chunker.nextChunk();
                deleteChunk(sObjects);
                count+=sObjects.length;
            }

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

    private void deleteChunk(SObject[] chunk) throws ConnectionException, SQLException {
        String[] delete = new String[chunk.length];
        for (int i = 0; i < chunk.length; i++) {
            SObject sObject = chunk[i];
            delete[i] = sObject.getId();
        }
        DeleteResult[] sr = pc.delete(delete);
        for (DeleteResult deleteResult : sr) {
            if (!deleteResult.isSuccess()) {
                Error[] errors = deleteResult.getErrors();
                for (Error error : errors) {
                    throw new SQLException(error.getMessage());
                }
            }
        }
    }


}
