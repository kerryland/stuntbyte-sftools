package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.partner.PartnerConnection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
    private PartnerConnection pc;

    public CreateTable(SimpleParser al, PartnerConnection pc) {
        this.al = al;
        this.pc = pc;
    }

    public void execute() throws Exception {
        String tableName = al.getToken().getValue();

        // TODO: IF NOT EXISTS

        List<Column> columns = new ArrayList<Column>();
        Table table = new Table(tableName, null, columns);

        al.read("(");

        String columnName = al.readIf();
        while (!(columnName == null)) {
            String dataType = al.readIf();

            // TODO: Validate supported datatypes


            Column col = new Column(columnName, publicTypeToSalesforceType(dataType));
            // TODO: Lots!
            columns.add(col);

            String value = al.getValue();

            if (dataType.equalsIgnoreCase("DECIMAL") || dataType.equalsIgnoreCase("NUMERIC")) {
                if (value.equals("(")) {
                    int precision = getInteger(al.getValue());
                    col.setLength(precision);
                    col.setPrecision(precision);

                    value = al.getValue();
                    if (value.equals(",")) {
                        col.setScale(getInteger(al.getValue()));
                    }
                    al.read(")");
                }
            }

            if (dataType.equalsIgnoreCase("VARCHAR") ||
                    dataType.equalsIgnoreCase("CHAR") ||
                    dataType.equalsIgnoreCase("TEXTAREA")) {

                if (value.equals("(")) {
                    col.setLength(getInteger(al.getValue()));
                    al.read(")");
                }
            }


            if (!value.equals(",") && !value.equals(")")) {
                throw new SQLException("Expected , or ) -- not " + value);
            }
            columnName = al.getValue();
        }

        createMetadataXml(table);
    }


    private int getInteger(String value) {
        return Integer.valueOf(value);
    }

    // Convert a public-facing datatype, such as VARCHAR, to the Salesforce equivalent
    private String publicTypeToSalesforceType(String datatype) {
        if (datatype.equalsIgnoreCase("varchar")) return "string";
        return datatype.toLowerCase();
    }


    public void createMetadataXml(Table table) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();


        Document document = documentBuilder.newDocument();
        Element rootElement = document.createElementNS("http://soap.sforce.com/2006/04/metadata", "CustomObject");
        document.appendChild(rootElement);

        Element em = document.createElement("enableFeeds");
        em.appendChild(document.createTextNode("true"));
        rootElement.appendChild(em);

        Element fields = document.createElement("fields");
        rootElement.appendChild(fields);

        List<Column> cols = table.getColumns();
        for (Column col : cols) {
            addElement(document, fields, "fullName", col.getName());
            addElement(document, fields, "defaultValue", "false");
            addElement(document, fields, "externalId", "false");
            addElement(document, fields, "label", col.getName());
            addElement(document, fields, "required", col.isNillable() ? "false" : "true");
            addElement(document, fields, "trackFeedHistory", "false");
            addElement(document, fields, "trackHistory", "false");
            addElement(document, fields, "type", col.getType());
            addElement(document, fields, "unique", "false");
            addElement(document, fields, "precision", "" + col.getPrecision());
            addElement(document, fields, "scale", "" + col.getScale());
            addElement(document, fields, "length", "" + col.getLength());
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
        StreamResult result = new StreamResult(System.out);
        transformer.transform(source, result);


        // package.xml
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

    }

    private void addElement(Document document, Element fields, String name, String value) {
        Element em;
        em = document.createElement(name);
        em.appendChild(document.createTextNode(value));
        fields.appendChild(em);
    }

}
