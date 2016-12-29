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

import com.stuntbyte.salesforce.jdbc.sqlforce.LexicalToken;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.stuntbyte.salesforce.parse.SimpleParser;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

import java.sql.SQLException;
import java.util.List;

/**
 */
public class Delete {

    private static int MAX_DELETES_PER_CALL = 200;
    private SimpleParser al;
    private Reconnector reconnector;

    public Delete(SimpleParser al, Reconnector reconnector) {
        this.al = al;
        this.reconnector = reconnector;
    }

    public int execute(Boolean batchMode, List<SObject> batchSObjects) throws Exception {
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

            qr = reconnector.query(readSoql.toString());

            SObjectChunker chunker = new SObjectChunker(MAX_DELETES_PER_CALL, reconnector, qr);
            while (chunker.next()) {
                SObject[] sObjects = chunker.nextChunk();
                if (batchMode) {
                    for (SObject sObject : sObjects) {
                        batchSObjects.add(sObject);
                    }
                } else {
                    deleteSObjects(sObjects);
                }
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

    public void deleteSObjects(SObject[] chunk) throws SQLException {
        try {
            String[] delete = new String[chunk.length];
            for (int i = 0; i < chunk.length; i++) {
                SObject sObject = chunk[i];
                delete[i] = sObject.getId();
            }
            DeleteResult[] sr = reconnector.delete(delete);
            for (DeleteResult deleteResult : sr) {
                if (!deleteResult.isSuccess()) {
                    Error[] errors = deleteResult.getErrors();
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
