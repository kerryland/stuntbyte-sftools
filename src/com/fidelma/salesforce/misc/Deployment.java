package com.fidelma.salesforce.misc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class Deployment {

    private String packageXml;
    private Document doc;
    private Element packge;
    private Map<String, List<String>> types = new HashMap<String, List<String>>();
    private List<DeploymentResource> deploymentResources = new ArrayList<DeploymentResource>();


    public void addMember(String typeName, String member, String code) throws Exception {
        List<String> members = types.get(typeName);
        if (members == null) {
            members = new ArrayList<String>();
            types.put(typeName, members);
        }
        members.add(member);

        DeploymentResource resource = new DeploymentResource();
        resource.setCode(code);
        resource.setFilepath(determineDirectoryName(typeName) + "/" + member + "." + determineFileSuffix(typeName));

//        File

        deploymentResources.add(resource);
    }


    // http://www.salesforce.com/us/developer/docs/api_meta/Content/file_based.htm#component_folders_title
    private String determineDirectoryName(String typeName) throws Exception {
        if (typeName.equalsIgnoreCase("ApexClass")) return "classes";
        if (typeName.equalsIgnoreCase("ApexComponent")) return "components";
        if (typeName.equalsIgnoreCase("ApexPage")) return "pages";
        if (typeName.equalsIgnoreCase("ApexTrigger")) return "triggers";
        if (typeName.equalsIgnoreCase("CustomField")) return "objects";
        if (typeName.equalsIgnoreCase("CustomObject")) return "objects";
        throw new Exception("Unsupported type: " + typeName);
    }

    private String determineFileSuffix(String typeName) throws Exception {
        if (typeName.equalsIgnoreCase("ApexClass")) return "cls";
        if (typeName.equalsIgnoreCase("ApexComponent")) return "component";
        if (typeName.equalsIgnoreCase("ApexPage")) return "page";
        if (typeName.equalsIgnoreCase("ApexTrigger")) return "trigger";
        if (typeName.equalsIgnoreCase("CustomField")) return "object";
        if (typeName.equalsIgnoreCase("CustomObject")) return "object";
        throw new Exception("Unsupported type: " + typeName);
    }


    public void assemble() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        doc = parser.newDocument();
        packge = doc.createElementNS("http://soap.sforce.com/2006/04/metadata", "Package");
        doc.appendChild(packge);

        for (String typeName : types.keySet()) {
            Node typesNode = packge.appendChild(doc.createElement("types"));

            List<String> members = types.get(typeName);
            for (String member : members) {
                addTextElement(typesNode, "members", member);
            }

            addTextElement(typesNode, "name", typeName);
        }
        addTextElement(packge, "version", "20.0");
    }

    private Element addTextElement(Node parent, String elementName, String value) {
        Element memberElement = doc.createElement(elementName);
        Text memberValue = doc.createTextNode(value);
        memberElement.appendChild(memberValue);
        parent.appendChild(memberElement);
        return memberElement;
    }

    public String getPackageXml() throws TransformerException {
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
//        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        return sw.toString();
    }

    public List<DeploymentResource> getDeploymentResources() {
        return deploymentResources;
    }
}
