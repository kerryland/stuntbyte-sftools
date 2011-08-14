package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.Column;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.Table;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.Deployment;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.misc.Downloader;
import com.fidelma.salesforce.misc.LoginHelper;
import com.fidelma.salesforce.misc.Reconnector;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
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
import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GRANT OBJECT CREATE, READ, UPDATE, DELETE, MODIFYALL, VIEWALL
 * ON <TABLE> TO <PROFILE> | *
 * <p/>
 * REVOKE OBJECT CREATE, READ, UPDATE, DELETE, MODIFY, VIEW
 * ON <TABLE> FROM <PROFILE> | *
 * <p/>
 * GRANT FIELD VISIBLE, EDITABLE ON <TABLE.COLUMN> TO [ <PROFILE>, <PROFILE>, *]
 * REVOKE FIELD VISIBLE, EDITABLE ON <TABLE.COLUMN> FROM <PROFILE>
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
    private Reconnector reconnector;
    private List<String> profileNames = null;

    public Grant(SimpleParser al, ResultSetFactory metaDataFactory,
                 Reconnector reconnector) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.reconnector = reconnector;
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
    * GRANT OBJECT CREATE, READ, UPDATE | EDIT, DELETE, MODIFYALL, VIEWALL
    *       ON <TABLE> TO <PROFILE> ... *
    */
    private void handleParseObject(boolean grant) throws Exception {

        PrepareDownload prepareDownload = new PrepareDownload().invoke();
        Downloader dl = prepareDownload.getDl();
        File sourceSchemaDir = prepareDownload.getSourceSchemaDir();
        final StringBuilder errors = prepareDownload.getErrors();

        // Keyed by Table, Property/Value
        final Map<String, Map<String, Boolean>> settingsByTable = new HashMap<String, Map<String, Boolean>>();

        // Property/Value
        final Map<String, Boolean> settings = new HashMap<String, Boolean>();

        String value;
        do {
            value = al.getValue();

            if (value.equalsIgnoreCase("create")) {
                settings.put("allowCreate", grant);
            } else if (value.equalsIgnoreCase("read")) {
                settings.put("allowRead", grant);
            } else if (value.equalsIgnoreCase("update") || value.equalsIgnoreCase("edit")) {
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

        final boolean allProfiles = addProfilesToPackage(grant, dl);

        dl.download();

        final Deployment dep = new Deployment();

        foreachProfile(sourceSchemaDir, new ProfileProcessor() {
            public void processProfileXml(Document doc, File profileFile) throws Exception {
                if (allProfiles && !includeThisProfile(doc)) {
                    return;
                }

                NodeList downOne = doc.getElementsByTagName("objectPermissions");
                for (int i = 0; i < downOne.getLength(); i++) {

                    NodeList permsList = downOne.item(i).getChildNodes();

                    for (int j = 0; j < permsList.getLength(); j++) {
                        Node n = permsList.item(j);
                        // Identify the table we are changing
                        if (n.getNodeName().equalsIgnoreCase("object")) {
                            Map<String, Boolean> settings = settingsByTable.get(n.getTextContent().toUpperCase());

                            if (settings != null) {
                                // And change the permissions
                                resetProperties(permsList, settings);
                            }
                        }
                    }
                }
                generateProfileXml(dep, profileFile, doc);
            }
        });

        deployProfiles(errors, dep);

        sourceSchemaDir.delete();
    }

    private boolean includeThisProfile(Document doc) {
        boolean includeProfile = true;
        NodeList licence = doc.getElementsByTagName("userLicense");
        if (licence.getLength() == 1) {
            if (licence.item(0).getTextContent().endsWith("Free")) {
                // Can't change things that are free.
                // Salesforce gives a "Cannot rename standard profile" error
                includeProfile = false;
            }
        }
        return includeProfile;
    }


    interface ProfileProcessor {
        void processProfileXml(Document doc, File profileFile) throws Exception;
    }

    private void foreachProfile(File sourceSchemaDir, ProfileProcessor profileProcessor) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();


        File child = new File(sourceSchemaDir, "profiles");
        File[] profileFiles = child.listFiles();
        for (File profileFile : profileFiles) {
            profileProcessor.processProfileXml(dBuilder.parse(profileFile), profileFile);
        }

    }


    private void deployProfiles(final StringBuilder errors, Deployment dep) throws Exception {
        Deployer deployer = new Deployer(reconnector);

        deployer.deploy(dep, new DeploymentEventListener() {
            public void error(String message) {
                errors.append(message).append(". ");
            }

            public void finished(String message) {
            }

            public void progress(String message) {

            }
        });
        if (errors.length() != 0) {
            throw new SQLException(errors.toString());
        }
    }

    private void generateProfileXml(Deployment dep, File profileFile, Document doc) throws Exception {
        StringWriter sw = new StringWriter();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(sw);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(source, result);

        dep.addMember("Profile", profileFile.getName(), sw.toString(), null);
    }


    /*
    * GRANT FIELD VISIBLE, EDITABLE ON <TABLE.[COLUMN |* ]> TO <PROFILE>
    */
    private void handleParseField(boolean grant) throws Exception {

        PrepareDownload prepareDownload = new PrepareDownload().invoke();
        Downloader dl = prepareDownload.getDl();
        File sourceSchemaDir = prepareDownload.getSourceSchemaDir();
        final StringBuilder errors = prepareDownload.getErrors();

        // Keyed by Table, Property/Value
        final Map<String, Map<String, Boolean>> settingsByColumn = new HashMap<String, Map<String, Boolean>>();

        // Property/Value
        Map<String, Boolean> settings = new HashMap<String, Boolean>();
        String value;

        do {
            value = al.getValue();
            if (value.equalsIgnoreCase("visible")) {
                settings.put("hidden", !grant);
                // If it's not visible it must also be not editable
                if (!grant) {
                    settings.put("editable", false);
                }
            } else if (value.equalsIgnoreCase("editable")) {
                settings.put("editable", grant);
            } else {
                throw new Exception("Unexpected value '" + value + "'");
            }
            value = al.getValue();
        } while (value.equals(","));

        assert (value.equalsIgnoreCase("ON"));

        String field = al.getValue();

        String tableName = field.substring(0, field.indexOf("."));
        Table table = metaDataFactory.getTable(tableName);

        if (field.endsWith(".*")) {
            for (Column col : table.getColumns()) {
                if (col.isCustom()) {
                    settingsByColumn.put((tableName + "." + col.getName()).toUpperCase(), settings);
                    dl.addPackage("CustomField", tableName + "." + col.getName());
                }
            }
        } else {

            if (!grant) {
                String columnName = field.substring(field.indexOf(".") + 1);
                Column col = table.getColumn(columnName);
                if (!col.isCustom()) {
                    throw new SQLException("Cannot change permissions of standard fields");
                }
            }
            settingsByColumn.put(field.toUpperCase(), settings);
            dl.addPackage("CustomField", field);
        }

        final boolean allProfiles = addProfilesToPackage(grant, dl);

        File zip = dl.download();
        System.out.println("DOWNLOADED EXISTING PROFILES TO " + zip.getName());

        final Deployment dep = new Deployment();

        foreachProfile(sourceSchemaDir, new ProfileProcessor() {
            public void processProfileXml(Document doc, File profileFile) throws Exception {
                if (allProfiles && !includeThisProfile(doc)) {
                    return;
                }

                NodeList downOne = doc.getElementsByTagName("fieldLevelSecurities");
                for (int i = 0; i < downOne.getLength(); i++) {

                    NodeList permsList = downOne.item(i).getChildNodes();

                    for (int j = 0; j < permsList.getLength(); j++) {
                        Node n = permsList.item(j);
                        // Identify the field we are changing
                        if (n.getNodeName().equalsIgnoreCase("field")) {
                            Map<String, Boolean> settings = settingsByColumn.get(n.getTextContent().toUpperCase());
                            if (settings != null) {
                                resetProperties(permsList, settings);
                            }
                        }
                    }
                }
                generateProfileXml(dep, profileFile, doc);
            }
        });

        deployProfiles(errors, dep);

//        sourceSchemaDir.delete();
    }

    private void resetProperties(NodeList permsList, Map<String, Boolean> settings) {
        // And change the permissions
        for (int k = 0; k < permsList.getLength(); k++) {
            Node perm = permsList.item(k);
            Boolean val = settings.get(perm.getNodeName());
            if (val != null) {
                perm.setTextContent(Boolean.toString(val));
            }
        }
    }


    // @return true if 'ALL' profiles added
    private boolean addProfilesToPackage(boolean grant, Downloader dl) throws Exception {
        String value;
        if (grant) {
            al.getToken("TO");
        } else {
            al.getToken("FROM");
        }

        value = al.getValue();

        boolean allProfiles = (value.equals("*"));
        do {
            if (allProfiles) {
                for (String profileName : getProfileNames()) {
                    dl.addPackage("Profile", profileName);
                }

            } else {
                dl.addPackage("Profile", value);
            }

            value = al.getValue();
        } while (value != null && value.equals(","));

        return allProfiles;
    }


    private List<String> getProfileNames() throws ConnectionException {
        if (profileNames == null) {
            profileNames = new ArrayList<String>();

            ListMetadataQuery lmdq = new ListMetadataQuery();
            lmdq.setType("Profile");

            FileProperties[] metadata = reconnector.listMetadata(new ListMetadataQuery[]{lmdq}, LoginHelper.WSDL_VERSION);
            for (int i = 0; i < metadata.length; i++) {
                FileProperties fileProperties = metadata[i];
                profileNames.add(fileProperties.getFullName());
            }
        }

        return profileNames;
    }

    private class PrepareDownload {
        private File sourceSchemaDir;
        private StringBuilder errors;
        private Downloader dl;

        public File getSourceSchemaDir() {
            return sourceSchemaDir;
        }

        public StringBuilder getErrors() {
            return errors;
        }

        public Downloader getDl() {
            return dl;
        }

        public PrepareDownload invoke() throws IOException, SQLException {

            sourceSchemaDir = new File(System.getProperty("java.io.tmpdir"),
                    "SF-SRC" + System.currentTimeMillis());
            sourceSchemaDir.mkdir();
            sourceSchemaDir.deleteOnExit();

            errors = new StringBuilder();
            dl = new Downloader(reconnector, sourceSchemaDir, new DeploymentEventListener() {
                public void error(String message) {
                    errors.append(message);
                }

                public void finished(String message) {
                }

                public void progress(String message) {

                }
            }, null);
            if (errors.length() != 0) {
                throw new SQLException(errors.toString());
            }
            return this;
        }
    }
}
