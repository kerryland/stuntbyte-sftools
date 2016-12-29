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

import com.stuntbyte.salesforce.misc.Reconnector;
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
