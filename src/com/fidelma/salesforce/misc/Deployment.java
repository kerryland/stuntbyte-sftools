package com.fidelma.salesforce.misc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
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

    private Document doc;
    private Map<String, List<String>> types = new HashMap<String, List<String>>();
    private List<DeploymentResource> deploymentResources = new ArrayList<DeploymentResource>();
    private boolean assembled;


    public void addMember(String typeName, String member, String code, String metaData) throws Exception {
        if (member.contains(".") && member.endsWith(determineFileSuffix(typeName))) {
            member = member.substring(0, member.lastIndexOf(determineFileSuffix(typeName))-1);
        }
        List<String> members = types.get(typeName);
        if (members == null) {
            members = new ArrayList<String>();
            types.put(typeName, members);
        }
        members.add(member);

        if (code != null) {
            DeploymentResource resource = new DeploymentResource();
            resource.setCode(code);
            resource.setMetaData(metaData);
            resource.setFilepath(determineDirectoryName(typeName) + "/" + member + "." + determineFileSuffix(typeName));
            deploymentResources.add(resource);
        }
    }


    // http://www.salesforce.com/us/developer/docs/api_meta/Content/file_based.htm#component_folders_title
    private String determineDirectoryName(String typeName) throws Exception {
        if (typeName.equalsIgnoreCase("ActionOverride")) return "objects";
        if (typeName.equalsIgnoreCase("ApexClass")) return "classes";
        if (typeName.equalsIgnoreCase("ArticleType")) return "objects";
        if (typeName.equalsIgnoreCase("ApexComponent")) return "components";
        if (typeName.equalsIgnoreCase("ApexPage")) return "pages";
        if (typeName.equalsIgnoreCase("ApexTrigger")) return "triggers";
        if (typeName.equalsIgnoreCase("BusinessProcess")) return "objects";
        if (typeName.equalsIgnoreCase("CustomApplication")) return "applications";
        if (typeName.equalsIgnoreCase("CustomField")) return "objects";
        if (typeName.equalsIgnoreCase("CustomLabel")) return "labels";
        if (typeName.equalsIgnoreCase("CustomObject")) return "objects";
        if (typeName.equalsIgnoreCase("CustomObjectTranslation")) return "objectTranslations";
        if (typeName.equalsIgnoreCase("CustomPageWebLink")) return "weblinks";
        if (typeName.equalsIgnoreCase("CustomSite")) return "sites";
        if (typeName.equalsIgnoreCase("CustomTab")) return "tabs";
        if (typeName.equalsIgnoreCase("Dashboard")) return "dashboards";
        if (typeName.equalsIgnoreCase("DataCategoryGroup")) return "datacategorygroups";
        if (typeName.equalsIgnoreCase("Document")) return "document";
        if (typeName.equalsIgnoreCase("EmailTemplate")) return "email";
        if (typeName.equalsIgnoreCase("EntitlementTemplate")) return "entitlementTemplates";
        if (typeName.equalsIgnoreCase("FieldSet")) return "objects";
        if (typeName.equalsIgnoreCase("HomePageComponent")) return "homePageComponents";
        if (typeName.equalsIgnoreCase("HomePageLayout")) return "homePageLayouts";
        if (typeName.equalsIgnoreCase("Layout")) return "layouts";
        if (typeName.equalsIgnoreCase("Letterhead")) return "letterhead";
        if (typeName.equalsIgnoreCase("ListView")) return "objects";
        if (typeName.equalsIgnoreCase("NamedFilter")) return "objects";
        if (typeName.equalsIgnoreCase("PermissionSet")) return "permissionsets";
        if (typeName.equalsIgnoreCase("Portal")) return "portals";
        if (typeName.equalsIgnoreCase("Profile")) return "profiles";
        if (typeName.equalsIgnoreCase("RecordType")) return "recordtypes";
        if (typeName.equalsIgnoreCase("RemoteSiteSetting")) return "remoteSiteSettings";
        if (typeName.equalsIgnoreCase("Report")) return "reports";
        if (typeName.equalsIgnoreCase("ReportType")) return "reportTypes";
        if (typeName.equalsIgnoreCase("Scontrol")) return "scontrols";
        if (typeName.equalsIgnoreCase("SharingReason")) return "objects";
        if (typeName.equalsIgnoreCase("SharingRecalculation")) return "objects";
        if (typeName.equalsIgnoreCase("StaticResource")) return "staticresources";
        if (typeName.equalsIgnoreCase("Translations")) return "translations";
        if (typeName.equalsIgnoreCase("ValidationRule")) return "objects";
        if (typeName.equalsIgnoreCase("Weblink")) return "objects";
        if (typeName.toUpperCase().startsWith("WORKFLOW")) return "workflows";

        System.err.println("WARNING: Unknown type " + typeName + " -- guessing at 'objects'");

        return "objects";
    }

     private String determineFileSuffix(String typeName) throws Exception {
        String directoryName = determineDirectoryName(typeName);
        String suffix;
        if (directoryName.equals("classes")) {
            suffix = "cls";

        } else if (directoryName.equalsIgnoreCase("StaticResource")) {
            suffix = "resource";
        } else {
            suffix = directoryName.substring(0, directoryName.length()-1);
        }
        return suffix;
    }


    private void assemble() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        doc = parser.newDocument();
        Element packge = doc.createElementNS("http://soap.sforce.com/2006/04/metadata", "Package");
        doc.appendChild(packge);

        for (String typeName : types.keySet()) {
            Node typesNode = packge.appendChild(doc.createElement("types"));

            List<String> members = types.get(typeName);
            for (String member : members) {
                addTextElement(typesNode, "members", member);
            }

            addTextElement(typesNode, "name", typeName);
        }
        addTextElement(packge, "version", ""  + LoginHelper.WSDL_VERSION);

        assembled = true;
    }

    private Element addTextElement(Node parent, String elementName, String value) {
        Element memberElement = doc.createElement(elementName);
        Text memberValue = doc.createTextNode(value);
        memberElement.appendChild(memberValue);
        parent.appendChild(memberElement);
        return memberElement;
    }

    public String getPackageXml() throws Exception {
        if (!assembled) {
            assemble();
        }
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

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

    public boolean hasContent() {
        return types.keySet().size() > 0;
    }

    public Map<String, List<String>> getTypes() {
        return types;
    }
}
