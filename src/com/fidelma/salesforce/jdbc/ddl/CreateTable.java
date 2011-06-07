package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.Deployment;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.table.TableStringConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * CREATE TABLE <tableName> (
 * <columnName> <dataType>
 * [ DEFAULT <expression> ]
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
    private MetadataConnection metadataConnection;

    public CreateTable(SimpleParser al, ResultSetFactory metaDataFactory, MetadataConnection metadataConnection) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.metadataConnection = metadataConnection;
    }

    public void executeCreate() throws Exception {
        String tableName = al.getToken().getValue();
        execute(tableName, false);
    }


    public void executeAlter(String tableName) throws Exception {
        execute(tableName, true);
    }

    public void createTables(List<Table> tables) throws SQLException {
        try {
            final StringBuilder deployError = new StringBuilder();
            Deployer deployer = new Deployer(metadataConnection);
            Deployment deployment = new Deployment();

            for (Table table : tables) {
                String xml = createMetadataXml(table);
                deployment.addMember("CustomObject", table.getName(), xml);
            }

            deployer.deploy(deployment, new DeploymentEventListener() {
                public void error(String message) {
                    deployError.append(message);
                    deployError.append("\n");
                }

                public void finished(String message) {
                }
            });

            if (deployError.length() != 0) {
                throw new SQLException(deployError.toString());
            }

            for (Table table : tables) {
                // Add some system generated columns to the metaDataFactory, so
                // we can query them.
                table.addColumn(new Column("Id", "Id", true));
                table.addColumn(new Column("CreatedById", "Text", true));
                table.addColumn(new Column("CreatedDate", "DateTime", true));
                table.addColumn(new Column("LastModifiedById", "Text", true));
                table.addColumn(new Column("LastModifiedDate", "DateTime", true));
                table.addColumn(new Column("Name", "Text", false));
                //        table.addColumn(new Column("OwnerId", "Text", true)); // TODO: Some custom objects do have this?
                table.addColumn(new Column("SystemModstamp", "DateTime", true));

                patchDataTypes(table.getColumns());

                metaDataFactory.addTable(table);
            }

        } catch (Exception e) {
            throw new SQLException(e);
        }
    }


    private void execute(String tableName, boolean alterMode) throws Exception {
        Table table = parse(tableName, alterMode);
        List<Table> tables = new ArrayList<Table>(1);
        tables.add(table);
        createTables(tables);
    }

    public Table parse() throws Exception {
        String tableName = al.getToken().getValue();
        return parse(tableName, false);
    }

    private Table parse(String tableName, boolean alterMode) throws Exception {
        // TODO: IF NOT EXISTS

        Table table = new Table(tableName, null, "TABLE");

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
                al.read("key");
                al.read("(");
                al.getValue(); // TODO: Do I care?
                al.read(")");
                value = al.getValue();

            } else {

                String dataType = al.readIf();

                boolean autoGeneratedIdColumn = false;

                Column col = new Column(columnName, dataType);
                if (columnName.equalsIgnoreCase("Id")) {
                    autoGeneratedIdColumn = true;
                }

                /*
   AutoNumber
   Lookup
   MasterDetail
   Checkbox
   Currency
   Date
   DateTime
   Email
   Number
   Percent
   Phone
   Picklist
   MultiselectPicklist
   Text
   TextArea
   LongTextArea
   Url
   EncryptedText
   Summary
   Hierarchy

                */

                if (dataType.equalsIgnoreCase("CURRENCY") ||
                        dataType.equalsIgnoreCase("PERCENT") || //?
                        dataType.equalsIgnoreCase("NUMBER") ||
                        dataType.equalsIgnoreCase("INTEGER")) {
                    value = al.getValue();

                    int precision = 18;
                    int scale = 0;
                    if (value != null && value.equals("(")) {
                        precision = getInteger(al.getValue());

                        value = al.getValue();
                        if (value.equals(",")) {
                            scale = getInteger(al.getValue());
                            value = al.getValue();
                        }
                        al.read(")");
                    }
                    col.setPrecision(precision);
                    col.setScale(scale);

                } else if (dataType.equalsIgnoreCase("LOOKUP") || dataType.equalsIgnoreCase("MasterDetail")) {
                    al.read("REFERENCES");
                    al.read("(");
                    col.setReferencedTable(al.getValue());
                    col.setReferencedColumn("Id");
                    al.read(")");
                    value = al.getValue();

                } else if (dataType.equalsIgnoreCase("TEXT") ||
                        dataType.equalsIgnoreCase("TEXTAREA")) {

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
                    table.addColumn(col);
                }
            }

            if (alterMode) {
                columnName = null;
            } else {
                if (value == null || (!value.equals(",") && !value.equals(")"))) {
                    throw new SQLException("Expected ',' or ')' -- not '" + value + "'");
                }
//                System.out.println("End of coldef with=" + value);
                columnName = al.getValue();
            }
        }

        return table;
    }

    // This is so we can update metadata locally
    private void patchDataTypes(List<Column> columns) {
        for (Column column : columns) {
            column.setType(publicTypeToSalesforceType(column.getType()));
        }
    }


    private int getInteger(String value) {
        return Integer.valueOf(value);
    }

    // Convert a public-facing datatype, such as Text, to the Salesforce metadata api equivalent
    private String publicTypeToSalesforceType(String datatype) {
        if (datatype.equalsIgnoreCase("AutoNumber")) return "string";
        if (datatype.equalsIgnoreCase("Lookup")) return "reference";
        if (datatype.equalsIgnoreCase("MasterDetail")) return "masterrecord";
        if (datatype.equalsIgnoreCase("Text")) return "string";
        if (datatype.equalsIgnoreCase("TextAreaLong")) return "string";
        if (datatype.equalsIgnoreCase("Checkbox")) return "boolean";
        if (datatype.equalsIgnoreCase("Number")) return "decimal";
        if (datatype.equalsIgnoreCase("Id")) return "String";
        return datatype.toLowerCase();
    }


    public String createMetadataXml(Table table) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document = documentBuilder.newDocument();
        Element rootElement = document.createElementNS("http://soap.sforce.com/2006/04/metadata", "CustomObject");
        document.appendChild(rootElement);

        addElement(document, rootElement, "label", table.getName());
        addElement(document, rootElement, "pluralLabel", table.getName() + "s");
//        addElement(document, rootElement, "enableFeeds", "false");
        addElement(document, rootElement, "deploymentStatus", "Deployed");
        addElement(document, rootElement, "sharingModel", "ReadWrite");

        // TODO: Handle auto number namefields! (and length != 15 :-)
        Element nameField = document.createElement("nameField");
        rootElement.appendChild(nameField);
        addElement(document, nameField, "label", "Name");
        addElement(document, nameField, "fullName", "Name");
        addElement(document, nameField, "type", "Text");
        addElement(document, nameField, "length", "15");

//<displayFormat>C-{00000}</displayFormat>
//<label>Candidate Number</label>
//<type>AutoNumber</type>
//</nameField>

        List<Column> cols = table.getColumns();
        for (Column col : cols) {
            Element fieldsX = document.createElement("fields");
            rootElement.appendChild(fieldsX);
            if (!col.getName().equalsIgnoreCase("Id")) {

                List<Element> fields = new ArrayList<Element>();

                addElement(document, fields, "fullName", col.getName());
//            addElement(document, fields, "defaultValue", "false");
//                addElement(document, fields, "externalId", "false");

                if (!col.getExtraProperties().containsKey("label")) {
                    addElement(document, fields, "label", col.getName());
                }
                addElement(document, fields, "trackFeedHistory", "false");
                addElement(document, fields, "trackHistory", "false");
                addElement(document, fields, "type", col.getType());
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
//                if (col.getScale() != 0) {
//
//                }
                if (col.getLength() != 0) {
                    addElement(document, fields, "length", "" + col.getLength());
                }

                for (String name : col.getExtraProperties().keySet()) {
                    addElement(document, fields, name, col.getExtraProperties().get(name));
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
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);

        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        transformer.transform(source, result);

        // package.xml
        /*
        document = documentBuilder.newDocument();
        rootElement = document.createElementNS("http://soap.sforce.com/2006/04/metadata", "Package");
        document.appendChild(rootElement);
        Element types = document.createElement("types");
        rootElement.appendChild(types);

        addElement(document, types, "members", table.getName());
        addElement(document, types, "name", "CustomObject");
        addElement(document, rootElement, "version", "20.0");

        source = new DOMSource(document);
        result = new StreamResult(System.out);
        transformer.transform(source, result);
        */
        return sw.toString();

    }

    private Element addElement(Document document, Element field, String name, String value) {
        Element em;
        em = document.createElement(name);
        em.appendChild(document.createTextNode(value));
        field.appendChild(em);
        return em;
    }

    private Element addElement(Document document, List<Element> fields, String name, String value) {
        Element em;
        em = document.createElement(name);
        em.appendChild(document.createTextNode(value));
        fields.add(em);
        return em;
    }


}
