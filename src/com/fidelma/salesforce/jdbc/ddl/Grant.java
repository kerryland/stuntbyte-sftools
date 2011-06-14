package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.Deployment;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.misc.Downloader;
import com.fidelma.salesforce.misc.LoginHelper;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.Profile;
import com.sforce.soap.metadata.ProfileFieldLevelSecurity;
import com.sforce.soap.metadata.ProfileObjectPermissions;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.UpdateMetadata;
import com.sforce.ws.ConnectionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GRANT OBJECT CREATE, READ, UPDATE, DELETE, MODIFYALL, VIEWALL
 * ON <TABLE> TO <PROFILE> | *
 *
 * REVOKE OBJECT CREATE, READ, UPDATE, DELETE, MODIFY, VIEW
 * ON <TABLE> FROM <PROFILE> | *
 * <p/>
 * GRANT FIELD HIDDEN, EDITABLE ON <TABLE.COLUMN> TO [ <PROFILE>, <PROFILE>, *]
 * REVOKE FIELD HIDDEN, EDITABLE ON <TABLE.COLUMN> FROM <PROFILE>
 * <p/>
 * GRANT LAYOUT <LAYOUT> ON <TABLE> TO <PROFILE>
 * REVOKE LAYOUT <LAYOUT> FROM <PROFILE>
 * <p/>
 * GRANT PAGE <PAGENAME> TO <PROFILE>
 * REVOKE PAGE <PAGENAME> FROM <PROFILE>
 * <p/>
 * GRANT TAB <TABNAME> [DefaultOn] TO <PROFILE>
 * REVOKE TAB <TABNAME> FROM <PROFILE>
 */
public class Grant {
    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private MetadataConnection metadataConnection;
    private List<String> profileNames = null;

    public Grant(SimpleParser al, ResultSetFactory metaDataFactory, MetadataConnection metadataConnection) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.metadataConnection = metadataConnection;

        // blurg();
    }

    private void blurg() {
        ListMetadataQuery lmq = new ListMetadataQuery();
        lmq.setType("Profile");

        ListMetadataQuery[] list = new ListMetadataQuery[]{lmq};

        try {
//            FileProperties[] fps = metadataConnection.listMetadata(list, LoginHelper.WSDL_VERSION);
//            for (FileProperties fp : fps) {
//                System.out.println("KJS '" + fp.getFullName() + "' " + fp.getType());
//            }

            File sourceSchemaDir = new File(System.getProperty("java.io.tmpdir"),
                    "SF-SRC" + System.currentTimeMillis());  // TODO: This must be unique
            sourceSchemaDir.mkdir();

            Downloader dl = new Downloader(metadataConnection, sourceSchemaDir, new DeploymentEventListener() {
                public void error(String message) {

                }

                public void finished(String message) {

                }
            }, null);

            dl.addPackage("Profile", "Standard");
            dl.addPackage("CustomObject", "abc__c");
            dl.download();

        } catch (Exception e) {
            e.printStackTrace();  // TODO properly!
        }

    }

    public void execute(boolean grant) throws SQLException {
        try {
            String value = al.getValue();
            if (value.equalsIgnoreCase("object")) {
                handleParseObject(grant);
            } else if (value.equalsIgnoreCase("field")) {
                handleParseField(grant);

            } else {
                throw new SQLException("Unexpected token: '" + value + "'");
            }
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }


    /*
    * GRANT OBJECT CREATE, READ, UPDATE, DELETE, MODIFYALL, VIEWALL
    *       ON <TABLE> TO <PROFILE> ... *
    */
    private void handleParseObject(boolean grant) throws Exception {

        File sourceSchemaDir = new File(System.getProperty("java.io.tmpdir"),
                "SF-SRC" + System.currentTimeMillis());  // TODO: This must be unique
        sourceSchemaDir.mkdir();
        sourceSchemaDir.deleteOnExit();

        final StringBuilder errors = new StringBuilder();
        Downloader dl = new Downloader(metadataConnection, sourceSchemaDir, new DeploymentEventListener() {
            public void error(String message) {
                errors.append(message);
            }
            public void finished(String message) {
            }
        }, null);
        if (errors.length() != 0) {
            throw new SQLException(errors.toString());
        }

        Map<String, Map<String, Boolean>> settingsByTable = new HashMap<String, Map<String, Boolean>>();

        Map<String, Boolean> settings = new HashMap<String, Boolean>();

        String value;
        do {
            value = al.getValue();

            if (value.equalsIgnoreCase("create")) {
                settings.put("allowCreate", grant);
            } else if (value.equalsIgnoreCase("read")) {
                settings.put("allowRead", grant);
            } else if (value.equalsIgnoreCase("update")) {
                settings.put("allowEdit", grant);
            } else if (value.equalsIgnoreCase("delete")) {
                settings.put("allowDelete", grant);
            } else if (value.equalsIgnoreCase("modifyAll")) {
                settings.put("modifyAllRecords", grant);
            } else if (value.equalsIgnoreCase("viewAll")) {
                settings.put("viewAllRecords", grant);
            } else if (value.equalsIgnoreCase(",")) {
            } else {
                throw new Exception("Unexpected value '" + value + "'");
            }
            value = al.getValue();
        } while (value.equals(","));

        assert (value.equalsIgnoreCase("ON"));
        String table = al.getValue();

        settingsByTable.put(table.toUpperCase(), settings);

        dl.addPackage("CustomObject", table);

        if (grant) {
            al.getToken("TO");
        } else {
            al.getToken("FROM");
        }

        value = al.getValue();
        do {
            if (value.equals("*")) {
                for (String profileName : getProfileNames()) {
                    dl.addPackage("Profile", profileName);
                }

            } else {
                dl.addPackage("Profile", value);
            }

            value = al.getValue();
        } while (value != null && value.equals(","));

        dl.download();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        Deployer deployer = new Deployer(metadataConnection);
        Deployment dep = new Deployment();

        File child = new File(sourceSchemaDir, "profiles");
        File[] profileFiles = child.listFiles();
        for (File profileFile : profileFiles) {
            Document doc = dBuilder.parse(profileFile);

            NodeList licence = doc.getElementsByTagName("userLicense");
            if (licence.getLength() == 1) {
                if (licence.item(0).getTextContent().endsWith("Free")) {
                    // Can't change things that are free
                    continue;
                }
            }

            NodeList downOne = doc.getElementsByTagName("objectPermissions");
            for (int i = 0; i < downOne.getLength(); i++) {

                NodeList permsList = downOne.item(i).getChildNodes();

                // Identify the table we are changing
                for (int j = 0; j < permsList.getLength(); j++) {
                    Node n = permsList.item(j);
                    if (n.getNodeName().equalsIgnoreCase("object")) {
                        settings = settingsByTable.get(n.getTextContent().toUpperCase());

                        for (int k = 0; k < permsList.getLength(); k++) {
                            Node perm = permsList.item(k);
                            Boolean val = settings.get(perm.getNodeName());
                            if (val != null) {
                                perm.setTextContent(Boolean.toString(val));
                            }
                        }
                    }
                }
            }

            StringWriter sw = new StringWriter();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(sw);
            transformer.transform(source, result);

            dep.addMember("Profile", profileFile.getName(), sw.toString());
        }

        deployer.deploy(dep, new DeploymentEventListener() {
            public void error(String message) {
                errors.append(message);
            }
            public void finished(String message) {
            }
        });
        if (errors.length() != 0) {
            throw new SQLException(errors.toString());
        }

        sourceSchemaDir.delete();
    }


    /*
    * GRANT FIELD HIDDEN, EDITABLE ON <TABLE.[COLUMN |* ]> TO <PROFILE>
    */
    private void handleParseField(boolean grant) throws Exception {
        String value;
        value = al.getValue();
        boolean hidden = !grant;
        boolean editable = !grant;
        do {
            if (value.equalsIgnoreCase("hidden")) {
                hidden = grant;
            } else if (value.equalsIgnoreCase("editable")) {
                hidden = grant;
            }
            value = al.getValue();
        } while (value.equals(","));

        assert (value.equalsIgnoreCase("ON"));

        List<ProfileFieldLevelSecurity> pops = new ArrayList<ProfileFieldLevelSecurity>();

        String field = al.getValue();
        if (field.endsWith(".*")) {
            String tableName = field.substring(0, field.indexOf("."));
            Table table = metaDataFactory.getTable(tableName);

            ProfileFieldLevelSecurity pop = new ProfileFieldLevelSecurity();
            for (Column col : table.getColumns()) {
                pop.setField(tableName + "." + col.getName());
                pop.setHidden(hidden);
                pop.setEditable(editable);
                pops.add(pop);
            }
        } else {
            ProfileFieldLevelSecurity pop = new ProfileFieldLevelSecurity();
            pop.setField(field);
            pop.setHidden(hidden);
            pop.setEditable(editable);
            pops.add(pop);
        }

        List<Profile> profiles = new ArrayList<Profile>();

        ProfileFieldLevelSecurity[] popsArray = new ProfileFieldLevelSecurity[pops.size()];
        pops.toArray(popsArray);

        if (grant) {
            assert (value.equalsIgnoreCase("TO"));
        } else {
            assert (value.equalsIgnoreCase("FROM"));
        }

        value = al.getValue();
        do {
            if (value.equals("*")) {
                for (String profileName : getProfileNames()) {
                    Profile profile = new Profile();
                    profile.setFullName(profileName);
                    profile.setFieldLevelSecurities(popsArray);
                    profiles.add(profile);
                }

            } else {
                Profile profile = new Profile();
                profile.setFullName(al.getValue());
                profile.setFieldLevelSecurities(popsArray);
                profiles.add(profile);
            }

            value = al.getValue();
        } while (value != null && value.equals(","));


        UpdateMetadata[] metadata = new UpdateMetadata[profiles.size()];

        int i = 0;
        for (Profile profile : profiles) {
            metadata[i] = new UpdateMetadata();
            metadata[i].setMetadata(profile);
            i++;
        }

        AsyncResult[] asyncResults = metadataConnection.update(metadata);

        for (AsyncResult asyncResult : asyncResults) {
            while (!asyncResult.isDone()) {
                Thread.sleep(500);

            }
            System.out.println("MESSAGE " + asyncResult.getMessage());
        }

    }


    private List<String> getProfileNames() throws ConnectionException {
        if (profileNames == null) {
            profileNames = new ArrayList<String>();

            ListMetadataQuery lmdq = new ListMetadataQuery();
            lmdq.setType("Profile");

            FileProperties[] metadata = metadataConnection.listMetadata(new ListMetadataQuery[]{lmdq}, LoginHelper.WSDL_VERSION);
            for (int i = 0; i < metadata.length; i++) {
                FileProperties fileProperties = metadata[i];
                profileNames.add(fileProperties.getFullName());
            }
        }

        return profileNames;
    }

}
