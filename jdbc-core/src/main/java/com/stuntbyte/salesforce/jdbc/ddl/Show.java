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
package com.stuntbyte.salesforce.jdbc.ddl;

import com.stuntbyte.salesforce.core.metadata.Metadata;
import com.stuntbyte.salesforce.core.metadata.MetadataService;
import com.stuntbyte.salesforce.jdbc.metaforce.ColumnMap;
import com.stuntbyte.salesforce.jdbc.metaforce.ForceResultSet;
import com.stuntbyte.salesforce.parse.ParsedSelect;
import com.stuntbyte.salesforce.parse.SimpleParser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * select * from metadata.Types [ WHERE Identifier LIKE '<likeClause>' ]
 * select * from metadata.ApexClass [ WHERE Identifier LIKE '<likeClause>' ];

 */
public class Show {
    private ParsedSelect parsedSelect;
    private MetadataService metadataService;

    public Show(ParsedSelect parsedSelect, MetadataService metadataService) {
        this.parsedSelect = parsedSelect;
        this.metadataService = metadataService;
    }


    public ResultSet execute() throws Exception {

        String sql = parsedSelect.getParsedSql().substring(parsedSelect.getParsedSql().
                indexOf(parsedSelect.getDrivingTable()) + parsedSelect.getDrivingTable().length());


        SimpleParser al = new SimpleParser(sql);

        String likeValue = null;

        String maybeWhere = al.getValue();
        if (maybeWhere != null) {
            boolean explode = true;
            if ("WHERE".equalsIgnoreCase(maybeWhere)) {
                if ("Identifier".equalsIgnoreCase(al.getValue())) {
                    if ("LIKE".equalsIgnoreCase(al.getValue())) {
                        likeValue = al.getValue();
                        if (likeValue != null) {
                            explode = false;
                        }
                    }
                }
            }
            if (explode) {
                throw new SQLException("Crude metadata query only supports 'WHERE Identifier LIKE '<likeString>' ");
            }
        }

        if (likeValue == null) {
            likeValue = "%";
        }

        String expr = likeValue.toLowerCase().replace(".", "\\.");
        expr = expr.replace("?", ".");
        expr = expr.replace("%", ".*");

        Pattern likePattern = Pattern.compile(expr);

        String metadataType = parsedSelect.getDrivingTable().substring(parsedSelect.getDrivingTable().indexOf(".") + 1);

        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();

        if (metadataType.equalsIgnoreCase("types")) {
            List<String> types = metadataService.getMetadataTypes();

            for (String type : types) {
                Matcher m = likePattern.matcher(type.toLowerCase());
                if (m.matches()) {
                    ColumnMap<String, Object> row = new ColumnMap<String, Object>();
                    row.put("Identifier", type);
                    maps.add(row);
                }
            }

        } else {
            List<Metadata> metadataList = metadataService.getMetadataByType(metadataType);
            Collections.sort(metadataList, new Comparator<Metadata>() {
                public int compare(Metadata metadata, Metadata metadata1) {
                    return metadata.getName().compareTo(metadata1.getName());
                }
            });

            for (Metadata metadata : metadataList) {
                Matcher m = likePattern.matcher(metadata.getSalesforceId().toLowerCase());
                if (m.matches()) {
                    ColumnMap<String, Object> row = new ColumnMap<String, Object>();
                    row.put("Identifier", metadata.getSalesforceId());
                    row.put("Name", metadata.getName());
                    row.put("LastChangedBy", metadata.getLastChangedBy());
                    maps.add(row);
                }
            }
        }

        return new ForceResultSet(maps);

    }

    public ResultSet count() throws Exception {
        ResultSet results = execute();
        Integer count = 0;
        while (results.next()) {
            count++;
        }
        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        ColumnMap<String, Object> row = new ColumnMap<String, Object>();
        row.put("COUNT", count);
        maps.add(row);
        return new ForceResultSet(maps);

    }
}
