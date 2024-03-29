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
package com.stuntbyte.salesforce.deployment;

import com.stuntbyte.salesforce.misc.FileUtil;
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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 */
public class Deployment {

    private double sfVersion;

    private Map<String, Set<String>> typesToDeploy = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> typesToDrop = new HashMap<String, Set<String>>();
    private List<DeploymentResource> deploymentResources = new ArrayList<DeploymentResource>();
//    private boolean assembled;


    public Deployment(double sfVersion) {
        this.sfVersion = sfVersion;
    }

    // Does NOT store the member in the package.xml. Useful when working with CustomFields
    public void addDeploymentResource(String typeName, String member, String code, String metaData) throws Exception {
//        member = rememberMember(typeName, member, typesToDeploy);
        if (code != null) {
            DeploymentResource resource = new DeploymentResource();
            resource.setCode(code);
            resource.setMetaData(metaData);
            resource.setFilepath(FileUtil.determineDirectoryName(typeName) + "/" + member + "." + FileUtil.determineFileSuffix(typeName));
            deploymentResources.add(resource);
        }
    }


    public void addMember(String typeName, String member, String code, String metaData) throws Exception {
        member = rememberMember(typeName, member, typesToDeploy);
        addDeploymentResource(typeName, member, code, metaData);
    }

    public void dropMember(String typeName, String member) throws Exception {
        rememberMember(typeName, member, typesToDrop);
    }


    private String rememberMember(String typeName, String member, Map<String, Set<String>> types) throws Exception {
        if (member.contains(".") && member.endsWith(FileUtil.determineFileSuffix(typeName))) {
            member = member.substring(0, member.lastIndexOf(FileUtil.determineFileSuffix(typeName)) - 1);
        }

        Set<String> members = types.get(typeName);
        if (members == null) {
            // These have to be sorted or Salesforce gets confused. -- not really
            members = new TreeSet<String>(new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            });
            types.put(typeName, members);
        }
        members.add(member);
        return member;
    }




    private String assemble(Map<String, Set<String>> types) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document doc = parser.newDocument();
        Element packge = doc.createElementNS("http://soap.sforce.com/2006/04/metadata", "Package");
        doc.appendChild(packge);

        for (String typeName : types.keySet()) {
            Node typesNode = packge.appendChild(doc.createElement("types"));

            Set<String> members = types.get(typeName);
            for (String member : members) {
                addTextElement(doc, typesNode, "members", member);
            }

            addTextElement(doc, typesNode, "name", typeName);
        }
        addTextElement(doc, packge, "version", "" + sfVersion);

        return FileUtil.docToXml(doc);
    }

    private Element addTextElement(Document doc, Node parent, String elementName, String value) {
        Element memberElement = doc.createElement(elementName);
        Text memberValue = doc.createTextNode(value);
        memberElement.appendChild(memberValue);
        parent.appendChild(memberElement);
        return memberElement;
    }

    public String getPackageXml() throws Exception {
        return assemble(typesToDeploy);
    }


    public String getDestructiveChangesXml() throws Exception {
        return assemble(typesToDrop);
    }

    public List<DeploymentResource> getDeploymentResources() {
        return deploymentResources;
    }

    public boolean hasContent() {
        return typesToDeploy.keySet().size() > 0;
    }

    public boolean hasDestructiveChanges() {
        return typesToDrop.keySet().size() > 0;
    }


    public Map<String, Set<String>> getTypesToDeploy() {
        return typesToDeploy;
    }

    public Map<String, Set<String>> getTypesToDrop() {
        return typesToDrop;
    }
}
