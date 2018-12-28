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

import com.stuntbyte.salesforce.deployment.Deployer;
import com.stuntbyte.salesforce.deployment.Deployment;
import com.stuntbyte.salesforce.jdbc.metaforce.Column;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.jdbc.metaforce.Table;
import com.stuntbyte.salesforce.misc.FileUtil;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.stuntbyte.salesforce.parse.ParseException;
import com.stuntbyte.salesforce.parse.SimpleParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.sql.SQLException;
import java.util.*;

/**
 * CREATE TABLE <tableName> (
 * <columnName> <dataType>
 * [ DEFAULT <expression> ]     -- TODO?
 * [ NOT | NOT NULL ]
 * [ IDENTITY ]
 * [ COMMENT <expression> ]
 * [ PRIMARY KEY | UNIQUE ]
 * )
 * <p/>
 * //
 */
public class CreateTable {
    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private Reconnector reconnector;
    private boolean alterMode = false;

    public CreateTable(SimpleParser al, ResultSetFactory metaDataFactory, Reconnector reconnector) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.reconnector = reconnector;
    }

    public void executeCreate() throws Exception {
        String tableName = al.getToken().getValue();
        Table table = new Table(tableName, null, "TABLE");
        Collection<Column> freshColumns = parse(false);

        createTables(Collections.singleton(table), Collections.singletonMap(table.getName().toUpperCase(), freshColumns));
    }

    public void executeAlter(String tableName) throws Exception {
        alterMode = true;
        Table table = metaDataFactory.getTable(tableName);
        Collection<Column> freshColumns = parse(alterMode);

        createTables(Collections.singleton(table), Collections.singletonMap(table.getName().toUpperCase(), freshColumns));
    }

    public Table executeCreateBatch() throws Exception {
        String tableName = al.getToken().getValue();
        Table table = new Table(tableName, null, "TABLE");
        List<Column> freshColumns = parse(false);

        for (Column freshColumn : freshColumns) {
            table.addColumn(freshColumn);
        }

        return table;
    }

    public void createTables(Collection<Table> tables, Map<String, Collection<Column>> freshColumnMap) throws SQLException {
        try {
            final StringBuilder deployError = new StringBuilder();
            Deployer deployer = new Deployer(reconnector);
            Deployment deployment = new Deployment(reconnector.getSfVersion());

            boolean needProfile = false;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document doc = parser.newDocument();
            Element profile = doc.createElementNS("http://soap.sforce.com/2006/04/metadata", "Profile");
            doc.appendChild(profile);

            for (Table table : tables) {
                Collection<Column> freshColumns = freshColumnMap.get(table.getName().toUpperCase());

                String xml = createMetadataXml(table, freshColumns);

                if (alterMode) {
                    metaDataFactory.removeTable(table.getName());  // We put it back later in this method

                    for (Column col : freshColumns) {
                        if (!col.getName().equalsIgnoreCase("Id") && !col.getName().equalsIgnoreCase("Name")) {
                            deployment.addMember("CustomField", table.getName() + "." + col.getName(), null, null);
                        }
                    }

                    deployment.addDeploymentResource("CustomObject", table.getName(), xml, null);
                } else {
                    deployment.addMember("CustomObject", table.getName(), xml, null);
                }

                for (Column col : freshColumns) {
                    if (col.isCustom()) {
                        Node fieldPermissionsNode = profile.appendChild(doc.createElement("fieldPermissions"));

                        addTextElement(doc, fieldPermissionsNode, "field", table.getName() + "." + col.getName());
                        addTextElement(doc, fieldPermissionsNode, "editable", "true");
                        addTextElement(doc, fieldPermissionsNode, "hidden", "false");
                        addTextElement(doc, fieldPermissionsNode, "readable", "true");

                        needProfile = true;
                    }
                }
            }

            if (needProfile) {
                deployment.addMember("Profile", "Admin", FileUtil.docToXml(doc), null);
                deployment.addMember("Profile", "Standard", FileUtil.docToXml(doc), null);
            }

            deployer.deploy(deployment, new DdlDeploymentListener(deployError, null));

            if (deployError.length() != 0) {
                throw new SQLException(deployError.toString());
            }


            boolean customName = false;
            for (Table table : tables) {
                Collection<Column> freshColumns = freshColumnMap.get(table.getName().toUpperCase());
                List<Column> clone = new ArrayList<>();
                for (Column freshColumn : freshColumns) {
                    clone.add(freshColumn);
                    if (freshColumn.getName().equalsIgnoreCase("name")) {
                        customName = true;
                    }
                }

                for (Column freshColumn : clone) {
                    table.removeColumn(freshColumn.getName());
                    table.addColumn(freshColumn);
                }

                if (!alterMode) {
                    // Add some system generated columns to the metaDataFactory, so
                    // we can query them. But really we should reload completely from WscService
                    table.addColumn(new Column("Id", "Id", true));
                    table.addColumn(new Column("CreatedById", "string", true));
                    table.addColumn(new Column("CreatedDate", "dateTime", true));
                    table.addColumn(new Column("LastModifiedById", "string", true));
                    table.addColumn(new Column("LastModifiedDate", "dateTime", true));

                    if (!customName) {
                        table.addColumn(new Column("Name", "string", false));
                    }
                    //        table.addColumn(new Column("OwnerId", "Text", true)); // TODO: Some custom objects do have this?
                    table.addColumn(new Column("SystemModstamp", "dateTime", true));
                    table.setSchema(ResultSetFactory.schemaName);
                }

                metaDataFactory.addTable(table);
            }

        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private Element addTextElement(Document doc, Node parent, String elementName, String value) {
        Element memberElement = doc.createElement(elementName);
        Text memberValue = doc.createTextNode(value);
        memberElement.appendChild(memberValue);
        parent.appendChild(memberElement);
        return memberElement;
    }

    private List<Column> parse(boolean alterMode) throws Exception {
        // TODO: IF NOT EXISTS

        List<Column> freshColumns = new ArrayList<>();

        try {

            if (!alterMode) {
                al.read("(");
            }

            String columnName = al.readIf();
            while (!(columnName == null)) {

                String value;
                if (columnName.equalsIgnoreCase("unique")) {
                    al.read("(");
                    al.getValue(); // TODO: Do I care?
                    al.read(")");
                    value = al.getValue();

                } else if (columnName.equalsIgnoreCase("primary")) {
                    al.read("licence");
                    al.read("(");
                    al.getValue(); // TODO: Do I care?
                    al.read(")");
                    value = al.getValue();

                } else {

                    String dataType = al.readIf();

                    // Validate the datatype
                    ResultSetFactory.lookupJdbcType(dataType);

                    boolean autoGeneratedIdColumn = false;

                    Column col = new Column(columnName, dataType);
                    if (columnName.equalsIgnoreCase("Id")) {
                        autoGeneratedIdColumn = true;
                        value = throwAwaySizeDefinition();
                    }

                    if (dataType.equalsIgnoreCase("CURRENCY") ||
                            dataType.equalsIgnoreCase("PERCENT") ||
//                        dataType.equalsIgnoreCase("NUMBER") ||
                            dataType.equalsIgnoreCase("DOUBLE") ||
                            dataType.equalsIgnoreCase("_DOUBLE") ||
                            dataType.equalsIgnoreCase("INT") ||
                            dataType.equalsIgnoreCase("DECIMAL") ||
                            dataType.equalsIgnoreCase("_INT")) {
//                        dataType.equalsIgnoreCase("INTEGER")) {

                        value = al.getValue();

                        int precision = 18;
                        int scale = 0;
                        if (value != null && value.equals("(")) {
                            precision = getInteger(al.getValue());

                            value = al.getValue();
                            if (value.equals(",")) {
                                scale = getInteger(al.getValue());
                            }
                            al.read(")");
                            value = al.getValue();
                        }
                        col.setPrecision(precision);
                        col.setScale(scale);

                    } else if (dataType.equalsIgnoreCase("Picklist") ||
                            dataType.equalsIgnoreCase("combobox") ||
                            dataType.equalsIgnoreCase("multipicklist")) {
                        //    colour__c picklist('green', 'blue' default, 'red') [ sorted ]
                        al.read("(");
                        do {
                            value = al.getValue();
                            String picklistValue = value;
                            col.addPicklistValue(picklistValue);
                            value = al.getValue();
                            if (value == null) {
                                throw new SQLException("Unexpected end of picklist definition");
                            }
                            if (value.equalsIgnoreCase("default")) {
                                col.setDefaultPicklistValue(picklistValue);
                                value = al.getValue();
                            }
                        } while (value.equals(","));
                        assert (value.equals(")"));
                        value = al.getValue();
                        if (value.equalsIgnoreCase("sorted")) {
                            col.pickListIsSorted(true);
                            value = al.getValue();
                        }

                    } else if (dataType.equalsIgnoreCase("Reference") || dataType.equalsIgnoreCase("MasterRecord")) {
                        value = throwAwaySizeDefinition();

                        if (value == null || !value.equalsIgnoreCase("references")) {
                            throw new SQLException("Expected REFERENCES, not '" + value + "'");
                        }

                        al.read("(");
                        col.setReferencedTable(al.getValue());
                        col.setReferencedColumn("Id");
                        al.read(")");
                        value = al.getValue();

                    } else if (dataType.equalsIgnoreCase("String") ||
                            dataType.equalsIgnoreCase("TEXTAREA") ||
                            dataType.equalsIgnoreCase("base64") ||
                            dataType.equalsIgnoreCase("encryptedstring") ||
                            dataType.equalsIgnoreCase("LongTextArea")) {

                        value = al.getValue();
                        if (value != null && value.equals("(")) {
                            col.setLength(getInteger(al.getValue()));
                            al.read(")");
                            value = al.getValue();
                        }

                    } else {
                        value = al.getValue();
                    }

                    if (value != null && value.equalsIgnoreCase("null")) {
                        col.setNillable(true);
                        value = al.getValue();

                    } else if (value != null && value.equalsIgnoreCase("not")) {
                        al.getToken("null");
                        col.setNillable(true);
                        value = al.getValue();

//                } else if (value.equalsIgnoreCase("generated")) {
//                    al.getToken("by");
//                    al.getToken("default");
//                    al.getToken("as");
//                    al.getToken("identity");
//                    value = al.getValue();
//
//                    autoGeneratedIdColumn = true;


                    } else if ((value != null) && value.equalsIgnoreCase("with")) {
                        al.getToken("(");
                        do {
                            col.addExtraProperty(al.getValue(), al.getValue());
                            value = al.getValue();
                        } while (value.equals(","));

                        assert (value.equals(")"));
                        value = al.getValue();
                    }

                    if (!autoGeneratedIdColumn) {
                        freshColumns.add(col);
                    }
                }

                if (alterMode) {
                    columnName = null;
                } else {
                    if (value == null || (!value.equals(",") && !value.equals(")"))) {
                        throw new ParseException("Expected ',' or ')' -- not '" + value + "' at or after " + columnName);
                    }
                    columnName = al.getValue();
                }
            }
        } catch (ParseException e) {
            if (freshColumns.size() > 0) {
                throw new ParseException("Error after column " + freshColumns.get(freshColumns.size() - 1).getName() + ": " + e.getMessage(), e);
            } else {
                throw e;
            }
        }

        return freshColumns;
    }

    private String throwAwaySizeDefinition() throws Exception {
        String value;
        value = al.getValue();
        // We don't care if they specify a length
        if (value != null && value.equals("(")) {
            getInteger(al.getValue());
            al.read(")");
            value = al.getValue();
        }
        return value;
    }

    private int getInteger(String value) {
        return Integer.valueOf(value);
    }

    public String createMetadataXml(Table table, Collection<Column> freshColumns) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document = documentBuilder.newDocument();
        Element rootElement = document.createElementNS("http://soap.sforce.com/2006/04/metadata", "CustomObject");
        document.appendChild(rootElement);

        if (!alterMode) {
            // TODO: Remove hard-coding of these values -- support "with" on the end of create table
            addElement(document, rootElement, "label", table.getName());
            addElement(document, rootElement, "pluralLabel", table.getName() + "s");
//        addElement(document, rootElement, "enableFeeds", "false");
            addElement(document, rootElement, "deploymentStatus", "Deployed");
            addElement(document, rootElement, "sharingModel", "ReadWrite");

            Element nameField = document.createElement("nameField");
            rootElement.appendChild(nameField);

            // Handle auto number namefields! (and length != 15 :-)
            defineNameField(document, nameField, freshColumns);
        }

        Collection<Column> cols = freshColumns;
        for (Column col : cols) {
            if (!col.getName().equalsIgnoreCase("Id") && !col.getName().equalsIgnoreCase("Name")) {
                Element fieldsX = document.createElement("fields");
                rootElement.appendChild(fieldsX);

                List<Element> fields = new ArrayList<Element>();

                addElement(document, fields, "fullName", col.getName());
//            addElement(document, fields, "defaultValue", "false");
//                addElement(document, fields, "externalId", "false");

                if (!col.getExtraProperties().containsKey("label")) {
                    addElement(document, fields, "label", col.getName());
                }
                addElement(document, fields, "trackFeedHistory", "false");
                addElement(document, fields, "trackHistory", "false");

                String adjustedDataType = ResultSetFactory.getNiceName(col.getType());

                addElement(document, fields, "type", adjustedDataType);
                if (col.getReferencedTable() != null) {
                    addElement(document, fields, "referenceTo", col.getReferencedTable());
                } else {
                    addElement(document, fields, "required", col.isNillable() ? "false" : "true");
                }
                addElement(document, fields, "unique", "false");
                if (col.getPrecision() != 0) {
                    addElement(document, fields, "precision", "" + col.getPrecision());
                    addElement(document, fields, "scale", "" + col.getScale());
                }

                // Formula fields can't have a length defined
                if ((col.getLength() != 0) && (!col.getExtraProperties().containsKey("formula"))) {
                    addElement(document, fields, "length", "" + col.getLength());
                }

                for (String name : col.getExtraProperties().keySet()) {
                    addElement(document, fields, name, col.getExtraProperties().get(name));
                }

                // TODO: Handle CONTROLLING FIELD
                if (col.getPicklistValues().size() > 0) {
                    Element valueSet = document.createElement("valueSet");
                    Element valueSetDefinition = addElement(document, valueSet, "valueSetDefinition");
                    for (String val : col.getPicklistValues()) {
                        Element picklistValues = document.createElement("value");
                        addElement(document, picklistValues, "fullName", val);
                        addElement(document, picklistValues, "label", val);
                        addElement(document, picklistValues, "default",
                                val.equalsIgnoreCase(col.getDefaultPicklistValue()) ? "true" : "false");

                        valueSetDefinition.appendChild(picklistValues);
                    }
                    fieldsX.appendChild(valueSet);

                    if (col.isPicklistIsSorted()) {
                        addElement(document, valueSetDefinition, "sorted", "true");
                    }
                }

                Collections.sort(fields, new Comparator<Element>() {
                    public int compare(Element o1, Element o2) {
                        String name1 = o1.getTagName();
                        String name2 = o2.getTagName();
                        return name1.compareToIgnoreCase(name2);
                    }
                });

                for (Element em : fields) {
                    fieldsX.appendChild(em);
                }
            }
        }


        /*

    <fields>
        <fullName>Authorised_To_Update__c</fullName>
        <defaultValue>false</defaultValue>
        <externalId>false</externalId>
        <label>Authorised To Update</label>
        <trackFeedHistory>false</trackFeedHistory>
        <trackHistory>false</trackHistory>
        <type>Checkbox</type>
    </fields>
    */
        return FileUtil.docToXml(document);
    }

    private void defineNameField(Document document, Element nameField, Collection<Column> freshColumns) throws SQLException {
        Collection<Column> cols = freshColumns;

        Column nameColumn = null;

        for (Column col : cols) {
            if (col.getName().equalsIgnoreCase("name")) {
                nameColumn = col;
                break;
            }
        }

        // No explicit name column
        if (nameColumn == null) {
            addElement(document, nameField, "label", "Name");
            addElement(document, nameField, "fullName", "Name");
            addElement(document, nameField, "type", "Text"); // vs AutoNumber
            addElement(document, nameField, "length", "80");

        } else {

            if (nameColumn.getLabel() == null) {
                nameColumn.setLabel(nameColumn.getName());
            }
            addElement(document, nameField, "fullName", "Name");
            if (nameColumn.getType().equalsIgnoreCase("AutoNumber")) {
                String displayFormat = nameColumn.getExtraProperties().get("displayFormat");
                if (displayFormat == null) {
                    displayFormat = "{0000000000}";
                    addElement(document, nameField, "displayFormat", displayFormat);
                }

            } else {
                addElement(document, nameField, "length", "" + nameColumn.getLength());
            }

            addElement(document, nameField, "label", nameColumn.getLabel());

            String adjustedDataType = ResultSetFactory.getNiceName(nameColumn.getType());

            addElement(document, nameField, "type", adjustedDataType);

            for (String name : nameColumn.getExtraProperties().keySet()) {
                addElement(document, nameField, name, nameColumn.getExtraProperties().get(name));
            }

//            System.out.println("KJS create with type " + nameColumn.getType());


//            if (nameColumn.getType().equalsIgnoreCase("AutoNumber")) {
//
//
//            } else {
//                addElement(document, nameField, "length", "" + nameColumn.getLength());
//            }
        }


//<displayFormat>C-{00000}</displayFormat>
//<label>Candidate Number</label>
//<type>AutoNumber</type>
//</nameField>


    }

    private Element addElement(Document document, Element field, String name) {
        Element em = document.createElement(name);
        field.appendChild(em);
        return em;
    }

    private Element addElement(Document document, Element field, String name, String value) {
        Element em = document.createElement(name);
        em.appendChild(document.createTextNode(value));
        field.appendChild(em);
        return em;
    }

    private Element addElement(Document document, List<Element> fields, String name, String value) {
        Element em = document.createElement(name);
        em.appendChild(document.createTextNode(value));
        fields.add(em);
        return em;
    }


}
