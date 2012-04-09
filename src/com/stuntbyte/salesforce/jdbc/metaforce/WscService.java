package com.stuntbyte.salesforce.jdbc.metaforce;

import com.sforce.soap.partner.ChildRelationship;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.PicklistEntry;
import com.sforce.soap.partner.RecordTypeInfo;
import com.sforce.ws.ConnectionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Wraps the Force.com describe calls web service outputting simple data objects.
 * <p/>
 * Uses WSC.
 */
public class WscService {

    private PartnerConnection partnerConnection;

    public WscService(PartnerConnection partnerConnection)  {
        this.partnerConnection = partnerConnection;
    }

    private void log(String message) {
//        System.out.println("ForceMetaDataDriver: " + message);
    }

    /**
     * Grab the describe data and return it wrapped in a factory.
     * @param info
     * @param includeTables
     */
    public ResultSetFactory createResultSetFactory(Properties info, boolean includeTables) throws ConnectionException {

        // Map a table to a map of columns and their related lookup object(s!)
        Map<String, Map<String, List<String>>> relationshipMap = new HashMap<String, Map<String, List<String>>>();

        int dataTypeMode = ResultSetFactory.DATATYPES_SALESFORCE_API;
        
        String dataTypes = info.getProperty("datatypes");
        if (dataTypes != null) {
            if (dataTypes.equalsIgnoreCase("SQL92")) {
                dataTypeMode = ResultSetFactory.DATATYPES_SQL92;
            } else if (dataTypes.equalsIgnoreCase("UI")) {
                dataTypeMode = ResultSetFactory.DATATYPES_SALESFORCE_UI;
            } else if (dataTypes.equalsIgnoreCase("API")) {
                dataTypeMode = ResultSetFactory.DATATYPES_SALESFORCE_API;

            } else {
                throw new ConnectionException("Unknown 'datatypes' property of " + dataTypes);
            }

        }
        ResultSetFactory factory = new ResultSetFactory(dataTypeMode);

        if (!includeTables) {
            return factory;
        }

        Map<String, String> childParentReferenceNames = new HashMap<String, String>();
        Map<String, Boolean> childCascadeDeletes = new HashMap<String, Boolean>();
        List<String> typesList = getSObjectTypes();
        Set<String> typesSet = new HashSet<String>(typesList);
        List<String[]> batchedTypes = batch(typesList);

        Map<String, DescribeSObjectResult> describeCache = new HashMap<String, DescribeSObjectResult>();

        // Need all child references so run through the batches first
        for (String[] batch : batchedTypes) {
            DescribeSObjectResult[] sobs = partnerConnection.describeSObjects(batch);
            if (sobs != null) {
                for (DescribeSObjectResult sob : sobs) {
                    describeCache.put(sob.getName(), sob);

                    ChildRelationship[] crs = sob.getChildRelationships();
                    if (crs != null) {
                        for (ChildRelationship cr : crs) {
                            Map<String, List<String>> lookupColumns = relationshipMap.get(cr.getChildSObject());
                            if (lookupColumns == null) {
                                lookupColumns = new HashMap<String, List<String>>();
                                relationshipMap.put(cr.getChildSObject(), lookupColumns);
                            }

                            if (typesSet.contains(cr.getChildSObject())) {
                                List<String> references = lookupColumns.get(cr.getField());
                                if (references == null) {
                                    references = new ArrayList<String>();
                                }
                                references.add(sob.getName());
                                lookupColumns.put(cr.getField(), references);
                                String qualified = cr.getChildSObject() + '.' + cr.getField();
                                childParentReferenceNames.put(qualified, cr.getRelationshipName());
                                childCascadeDeletes.put(qualified, cr.isCascadeDelete());
                            }
                        }
                    }
                }
            }
        }

        // Run through the batches again now the child references are available
        for (String[] batch : batchedTypes) {
            for (String tableName : batch) {
                DescribeSObjectResult sob = describeCache.get(tableName);
                Field[] fields = sob.getFields();

                StringBuilder recordTypes = new StringBuilder();
                for (RecordTypeInfo rt : sob.getRecordTypeInfos()) {
                    if (recordTypes.length() == 0) {
                        recordTypes.append("RecordTypes: ");
                    } else {
                        recordTypes.append(" | ");
                    }
                    recordTypes.append(rt.getName());
                    if (rt.isDefaultRecordTypeMapping()) {
                        recordTypes.append(" (default)");
                    }

                }

//                String type = sob.getUpdateable() ? "TABLE" : "SYSTEM TABLE";
                String type = "TABLE";

                if (sob.isQueryable()) {
                    Table table = new Table(sob.getName(), getRecordTypes(sob.getRecordTypeInfos()), type);
                    table.setSchema(ResultSetFactory.schemaName);

                    for (Field field : fields) {
                        Column col = recordColumn(
                                factory,
                                relationshipMap,
                                childParentReferenceNames,
                                childCascadeDeletes,
                                typesSet, sob, field);
                        if (col.getName().equalsIgnoreCase("RecordTypeId")) {
                            col.setComments(recordTypes.toString());
                        }
                        table.addColumn(col);
                    }

//                    Collections.sort(table.getColumns(), new Comparator<Column>() {
//                        public int compare(Column o1, Column o2) {
//                            String t1 = o1.getName();
//                            String t2 = o2.getName();
//                            return t1.compareTo(t2);
//                        }
//                    });

                    factory.addTable(table);
                }
            }
//            }
        }

        return factory;
    }

    private Column recordColumn(ResultSetFactory factory, Map<String, Map<String, List<String>>> relationshipMap,
                                Map<String, String> childParentReferenceNames,
                                Map<String, Boolean> childCascadeDeletes,
                                Set<String> typesSet,
                                DescribeSObjectResult sob,
                                Field field) {
        Column column = new Column(field.getName(), factory.getType(field.getType().toString()));
        column.setLabel(field.getLabel());
        column.setLength(getLength(field));
        column.setAutoIncrement(field.getAutoNumber());
        column.setCaseSensitive(field.getCaseSensitive());
        column.setPrecision(field.getPrecision());
        column.setScale(field.getScale());
        column.setDefault(field.getDefaultValueFormula());
        column.setNillable(field.isNillable());
        column.setUpdateable(field.getUpdateable());

        if ("reference".equals(field.getType().toString())) {
            // MasterDetail vs Reference apparently not
            // in API; cascade delete is though
            String qualified = sob.getName() + "." + field.getName();
            String childParentReferenceName = childParentReferenceNames.get(qualified);
            Boolean cascadeDelete = childCascadeDeletes.get(qualified);
            if (cascadeDelete != null && cascadeDelete) {
                column.setType(factory.getType("masterrecord"));
            }

            Map<String, List<String>> lookupColumns = relationshipMap.get(sob.getName());
            if (lookupColumns != null) {
                List<String> rels = lookupColumns.get(field.getName());

                if (rels != null) {
                    column.setRelationshipType(rels.get(0));
                    String comment;
                    if (rels.size() > 1) {
                        comment = "Relationships: ";
                        for (String rel : rels) {
                            comment += rel + ". ";
                        }
                        column.setHasMultipleRelationships(true);
                    } else {
                        comment = "Relationship: " + column.getRelationshipType();
                    }

                    column.setComments(comment);
                }
            } else if (childParentReferenceName != null && cascadeDelete != null) {
                column.setComments("Referenced: " + childParentReferenceName + (cascadeDelete ? " (cascade delete)" : ""));
            } else if (column.getReferencedTable() != null) {
                column.setComments("Referenced: " + column.getReferencedTable());
            }
        } else {
            column.setComments(getPicklistValues(field.getPicklistValues()));
        }

        // Booleans have this as false so not too
        // helpful; leave off
//        column.setNillable(false);

        // NB Not implemented; see comment in
        // ResultSetFactory class
        Boolean calculated = field.isCalculated() || field.isAutoNumber();
        if (field.getName().equalsIgnoreCase("Id") ||
                field.getName().equalsIgnoreCase("CreatedDate") ||
                field.getName().equalsIgnoreCase("CreatedById") ||
                field.getName().equalsIgnoreCase("LastModifiedDate") ||
                field.getName().equalsIgnoreCase("LastModifiedById") ||
                field.getName().equalsIgnoreCase("SystemModstamp") ||
                field.getName().equalsIgnoreCase("IsDeleted")) {
            calculated = true;
        }

        column.setCalculated(calculated);

        String[] referenceTos = field.getReferenceTo();
        if (referenceTos != null) {
            for (String referenceTo : referenceTos) {
                if (typesSet.contains(referenceTo)) {
                    column.setReferencedTable(referenceTo);
                    column.setReferencedColumn("Id");
                }
            }
        }

        return column;
    }

    private int getLength(Field field) {
        if (field.getLength() != 0) {
            return field.getLength();
        } else if (field.getPrecision() != 0) {
            return field.getPrecision();
        } else if (field.getDigits() != 0) {
            return field.getDigits();
        } else if (field.getByteLength() != 0) {
            return field.getByteLength();
        } else if (field.getType() == FieldType._boolean) {
            return 5; // Long enough for 'false'
        } else if (field.getType() == FieldType.date) {
            return 10;
        } else if (field.getType() == FieldType.datetime) {
            return 15;

        } else {
            // SchemaSpy expects a value
            return 0;
        }
    }

    private String getPicklistValues(PicklistEntry[] entries) {
        if (entries != null && entries.length > 0) {
            StringBuilder sb = new StringBuilder(256);
            for (PicklistEntry entry : entries) {
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append(entry.getValue());
            }
            return "Picklist: " + sb.toString();
        }
        return null;
    }

    private String getRecordTypes(RecordTypeInfo[] rts) {
        if (rts != null && rts.length > 0) {
            StringBuilder sb = new StringBuilder(256);
            for (RecordTypeInfo rt : rts) {
                // Master always present
                if (!rt.getName().equalsIgnoreCase("Master")) {
                    if (sb.length() > 0) {
                        sb.append(" | ");
                    }
                    sb.append(rt.getName());
                    if (rt.isDefaultRecordTypeMapping()) {
                        sb.append(" (default)");
                    }
                }
            }
            if (sb.length() > 0) {
                return "Record Types: " + sb.toString();
            }
        }
        return null;
    }

    // Avoid EXCEEDED_MAX_TYPES_LIMIT on call by breaking into batches
    private List<String[]> batch(List<String> types) {

        List<String[]> batchedTypes = new ArrayList<String[]>();

        final int batchSize = 100;
        for (int batch = 0; batch < (types.size() + batchSize - 1) / batchSize; batch++) {
            int from = batch * batchSize;
            int to = (batch + 1) * batchSize;
            if (to > types.size()) {
                to = types.size();
            }
            List<String> t = types.subList(from, to);
            String[] a = new String[t.size()];
            t.toArray(a);
            batchedTypes.add(a);
        }

        return batchedTypes;
    }

    private List<String> getSObjectTypes() throws ConnectionException {

        DescribeGlobalSObjectResult[] sobs = partnerConnection.describeGlobal().getSobjects();

        List<String> list = new ArrayList<String>();
        for (DescribeGlobalSObjectResult sob : sobs) {
            list.add(sob.getName());
        }
        return list;
    }

}
