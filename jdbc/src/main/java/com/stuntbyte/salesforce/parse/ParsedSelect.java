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
package com.stuntbyte.salesforce.parse;

import java.util.List;

/**
 */
public class ParsedSelect {
    private String drivingTable;
    private String parsedSql="";
    private List<ParsedColumn> columns;
    private String drivingSchema;

    public String getDrivingSchema() {
        return drivingSchema;
    }

    public String getDrivingTable() {
        return drivingTable;
    }

    public void setDrivingTable(String drivingTable) {
        this.drivingTable = drivingTable;
    }

    public List<ParsedColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<ParsedColumn> columns) {
        for (ParsedColumn column : columns) {
            column.setTable(drivingTable);
        }
        this.columns = columns;
    }

    public void addToSql(String val) {
        // System.out.print(val + " ");
        parsedSql += val + " ";
    }

    public String getParsedSql() {
        return parsedSql;
    }

    public void setDrivingSchema(String drivingSchema) {
        this.drivingSchema = drivingSchema;
    }
}
