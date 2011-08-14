package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.misc.Reconnector;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

import java.util.Arrays;

public class SObjectChunker {
    private int chunkSize;
    private Reconnector reconnector;
    private QueryResult qr;
    private int ptr;
    private SObject[] input;

    public SObjectChunker(int chunkSize, Reconnector reconnector, QueryResult qr) {
        this.chunkSize = chunkSize;
        this.reconnector = reconnector;
        this.qr = qr;
        input = qr.getRecords();
        ptr = 0;
    }

    public SObject[] nextChunk() {
        SObject[] chunk;
        if (input.length - ptr <= chunkSize) {
            chunk = Arrays.copyOfRange(input, ptr, input.length);
            ptr = Integer.MAX_VALUE;
        } else {
            int end = Math.min(input.length, ptr + chunkSize);
            chunk = Arrays.copyOfRange(input, ptr, end);
            ptr += chunkSize;
        }
        return chunk;
    }

    public boolean next() throws ConnectionException {
        if (ptr <= input.length) {
            return true;
        }
        if (!qr.isDone()) {
            ptr = 0;
            qr = reconnector.queryMore(qr.getQueryLocator());
            input = qr.getRecords();
            return true;
        }
        return false;
    }

}
