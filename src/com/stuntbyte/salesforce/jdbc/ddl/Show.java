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
}
