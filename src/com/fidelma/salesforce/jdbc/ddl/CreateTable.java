package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
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

    public void execute(String tableName, boolean alterMode) throws Exception {
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

                if (
                        dataType.equalsIgnoreCase("CURRENCY") ||
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
                }

                if (!autoGeneratedIdColumn) {
                    table.addColumn(col);
                }
            }

            if (alterMode) {
                columnName = null;
            } else {
                if (value == null || (!value.equals(",") && !value.equals(")"))) {
                    throw new SQLException("Expected , or ) -- not " + value);
                }
                System.out.println("End of coldef with=" + value);
                columnName = al.getValue();
            }
        }

        createMetadataXml(table);

        patchDataTypes(table.getColumns());
        metaDataFactory.addTable(table);
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
        if (datatype.equalsIgnoreCase("Text")) return "string";
        if (datatype.equalsIgnoreCase("TextAreaLong")) return "string";
        if (datatype.equalsIgnoreCase("Checkbox")) return "boolean";
        if (datatype.equalsIgnoreCase("Number")) return "decimal";
        return datatype.toLowerCase();
    }


    public void createMetadataXml(Table table) throws Exception {
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
            Element fields = document.createElement("fields");
            rootElement.appendChild(fields);
            if (!col.getName().equalsIgnoreCase("Id")) {

                addElement(document, fields, "fullName", col.getName());
//            addElement(document, fields, "defaultValue", "false");
                addElement(document, fields, "externalId", "false");
                addElement(document, fields, "label", col.getName());
                addElement(document, fields, "required", col.isNillable() ? "false" : "true");

                addElement(document, fields, "trackFeedHistory", "false");
                addElement(document, fields, "trackHistory", "false");
                addElement(document, fields, "type", col.getType());
                addElement(document, fields, "unique", "false");
                if (col.getPrecision() != null) {
                    addElement(document, fields, "precision", "" + col.getPrecision());
                }
                if (col.getScale() != null) {
                    addElement(document, fields, "scale", "" + col.getScale());
                }
                if (col.getLength() != null) {
                    addElement(document, fields, "length", "" + col.getLength());
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

        final StringBuilder deployError = new StringBuilder();
        Deployer deployer = new Deployer(metadataConnection);
        deployer.uploadNonCode(
                "CustomObject",
                "/objects/" + table.getName() + ".object",
                null,
                sw.toString(),
                new DeploymentEventListener() {
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
    }

    private Element addElement(Document document, Element fields, String name, String value) {
        Element em;
        em = document.createElement(name);
        em.appendChild(document.createTextNode(value));
        fields.appendChild(em);
        return em;
    }


}