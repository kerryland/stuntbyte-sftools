package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.Column;
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
    private PartnerConnection pc;
    private MetadataConnection metadataConnection;

    public CreateTable(SimpleParser al, PartnerConnection pc, MetadataConnection metadataConnection) {
        this.al = al;
        this.pc = pc;
        this.metadataConnection = metadataConnection;
    }

    public void execute() throws Exception {
        String tableName = al.getToken().getValue();

        // TODO: IF NOT EXISTS

        List<Column> columns = new ArrayList<Column>();
        Table table = new Table(tableName, null, columns);

        al.read("(");

        String columnName = al.readIf();
        while (!(columnName == null)) {

            String dataType = publicTypeToSalesforceType(al.readIf());

            // TODO: Validate supported datatypes


            Column col = new Column(columnName, dataType);

            System.out.println("COLUMN is " + columnName + " " + col.getType());
            // TODO: Lots!
            columns.add(col);

            /*
             <xsd:simpleType name="FieldType">
    xsd:restriction base="xsd:string">
     <xsd:enumeration value="AutoNumber"/>
     <xsd:enumeration value="Lookup"/>
     <xsd:enumeration value="MasterDetail"/>
     <xsd:enumeration value="Checkbox"/>
     <xsd:enumeration value="Currency"/>
     <xsd:enumeration value="Date"/>
     <xsd:enumeration value="DateTime"/>
     <xsd:enumeration value="Email"/>
     <xsd:enumeration value="Number"/>
     <xsd:enumeration value="Percent"/>
     <xsd:enumeration value="Phone"/>
     <xsd:enumeration value="Picklist"/>
     <xsd:enumeration value="MultiselectPicklist"/>
     <xsd:enumeration value="Text"/>
     <xsd:enumeration value="TextArea"/>
     <xsd:enumeration value="LongTextArea"/>
     <xsd:enumeration value="Url"/>
     <xsd:enumeration value="EncryptedText"/>
     <xsd:enumeration value="Summary"/>
     <xsd:enumeration value="Hierarchy"/>
    </xsd:restriction>
             */

            String value;
            if (
                    dataType.equalsIgnoreCase("CURRENCY") ||
                    dataType.equalsIgnoreCase("PERCENT") || //?
                    dataType.equalsIgnoreCase("NUMBER") ||
                    dataType.equalsIgnoreCase("INTEGER")) {
                value = al.getValue();
                System.out.println("After " + dataType + " got " + value);

                int precision = 18;
                int scale = 0;
                if (value.equals("(")) {
                    precision = getInteger(al.getValue());

                    value = al.getValue();
                    if (value.equals(",")) {
                        scale = getInteger(al.getValue());
                        value = al.getValue();
                    }
                    al.read(")");
                }
//                col.setLength(precision);
                col.setPrecision(precision);
                col.setScale(scale);

            } else if (dataType.equalsIgnoreCase("TEXT") ||
                    dataType.equalsIgnoreCase("TEXTAREA")) {

                value = al.getValue();
                int length = 20; // TODO: What's the default?

                if (value.equals("(")) {
                    length = getInteger(al.getValue());
                    al.read(")");
                    value = al.getValue();
                }
                col.setLength(length);
            } else {
                value = al.getValue();
            }

            if (!value.equals(",") && !value.equals(")")) {
                throw new SQLException("Expected , or ) -- not " + value);
            }
            System.out.println("End of coldef with=" + value);
            columnName = al.getValue();
        }

        createMetadataXml(table);
    }


    private int getInteger(String value) {
        return Integer.valueOf(value);
    }

    // Convert a public-facing datatype, such as VARCHAR, to the Salesforce metadata api equivalent
    private String publicTypeToSalesforceType(String datatype) {
        if (datatype.equalsIgnoreCase("varchar")) return "Text";
        if (datatype.equalsIgnoreCase("char")) return "Text";
        if (datatype.equalsIgnoreCase("integer")) return "Number";
        if (datatype.equalsIgnoreCase("decimal")) return "Number";
        if (datatype.equalsIgnoreCase("numeric")) return "Number";
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
        addElement(document, rootElement, "enableFeeds", "false");
        addElement(document, rootElement, "deploymentStatus", "Deployed");
        addElement(document, rootElement, "sharingModel", "Private");

        Element nameField = document.createElement("nameField");
        rootElement.appendChild(nameField);
        addElement(document, nameField, "label", "Name");
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

        Deployer deployer = new Deployer(metadataConnection);
        deployer.uploadNonCode(
                "CustomObject",
                "/objects/" + table.getName() + ".object",
                null,
                sw.toString(),
                new DeploymentEventListener() {
                    public void heyListen(String message) {
                        System.out.println("HEY! " + message);
                    }
                });
    }

    private Element addElement(Document document, Element fields, String name, String value) {
        Element em;
        em = document.createElement(name);
        em.appendChild(document.createTextNode(value));
        fields.appendChild(em);
        return em;
    }

}
