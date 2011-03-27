package com.fidelma.salesforce.jdbc.dml;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

import java.util.Arrays;

public class SObjectChunker {
    private int chunkSize;
    private PartnerConnection pc;
    private QueryResult qr;
    private int ptr;
    private SObject[] input;

    public SObjectChunker(int chunkSize, PartnerConnection pc, QueryResult qr) {
        this.chunkSize = chunkSize;
        this.pc = pc;
        this.qr = qr;
        input = qr.getRecords();
        ptr = 0;
    }

    public SObject[] nextChunk() {
        SObject[] chunk;
        System.out.println("ptr=" + ptr + " len=" + input.length + " diff=" + (input.length - ptr) + " vs chunksize " + chunkSize);
        if (input.length - ptr <= chunkSize) {
            chunk = Arrays.copyOfRange(input, ptr, input.length);
            System.out.println("END CHUNK WAS " + chunk.length);
            ptr = Integer.MAX_VALUE;
        } else {

            int end = Math.min(input.length, ptr + chunkSize);
            System.out.println("NEXT CHUNK IS " + ptr + " TO " + end);
            chunk = Arrays.copyOfRange(input, ptr, end);
            System.out.println("MID CHUNK WAS " + chunk.length);
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
            qr = pc.queryMore(qr.getQueryLocator());
            input = qr.getRecords();
            return true;
        }
        return false;
    }

}
