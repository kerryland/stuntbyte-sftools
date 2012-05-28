package com.stuntbyte.salesforce.core.metadata;

import com.stuntbyte.salesforce.jdbc.dml.SObjectChunker;
import com.stuntbyte.salesforce.misc.LoginHelper;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import java.util.*;

/**
 */
public class MetadataServiceImpl implements MetadataService {

    private Reconnector reconnector;
    
    private Map<String, List<Metadata>> metaDataContainerByType = new HashMap<String, List<Metadata>>();


    public MetadataServiceImpl(Reconnector reconnector) {
        this.reconnector = reconnector;
    }


    public List<String> getMetadataTypes() {
        // http://www.salesforce.com/us/developer/docs/dev_lifecycle/Content/plan_proj_meta_components.htm
        // More, or less.

        List<String> metaDataToDownload = new ArrayList<String>();
        metaDataToDownload.add("ActionOverride");
        metaDataToDownload.add("AnalyticSnapshot");
        metaDataToDownload.add("ApexClass");
        metaDataToDownload.add("ArticleType");

        metaDataToDownload.add("ApexComponent");
        metaDataToDownload.add("ApexPage");
        metaDataToDownload.add("ApexTrigger");

        metaDataToDownload.add("BusinessProcess");

        metaDataToDownload.add("CustomApplication");
        metaDataToDownload.add("CustomField");
        metaDataToDownload.add("CustomLabel");

        metaDataToDownload.add("CustomObject");
        metaDataToDownload.add("CustomObjectTranslation");

        metaDataToDownload.add("CustomPageWebLink");
        metaDataToDownload.add("CustomSite");
        metaDataToDownload.add("CustomTab");
        metaDataToDownload.add("Dashboard");
        metaDataToDownload.add("DataCategoryGroup");
        metaDataToDownload.add("Document");
        metaDataToDownload.add("EmailTemplate");
        metaDataToDownload.add("EntitlementTemplate");
        metaDataToDownload.add("FieldSet");
        metaDataToDownload.add("HomePageComponent");
        metaDataToDownload.add("HomePageLayout");
        metaDataToDownload.add("Layout");
        metaDataToDownload.add("Letterhead");
        metaDataToDownload.add("ListView");
        metaDataToDownload.add("NamedFilter");
        metaDataToDownload.add("PermissionSet");
        metaDataToDownload.add("Portal");
        metaDataToDownload.add("Profile");
        metaDataToDownload.add("RecordType");
        metaDataToDownload.add("RemoteSiteSetting");
        metaDataToDownload.add("Report");
        metaDataToDownload.add("ReportType");
        metaDataToDownload.add("Scontrol");
        metaDataToDownload.add("SharingReason");
        metaDataToDownload.add("SharingRecalculation");
        metaDataToDownload.add("StaticResource");
        metaDataToDownload.add("Translations");
        metaDataToDownload.add("ValidationRule");
        metaDataToDownload.add("WebLink");
        metaDataToDownload.add("Workflow");
        metaDataToDownload.add("WorkflowAlert");
        metaDataToDownload.add("WorkflowFieldUpdate");
        metaDataToDownload.add("WorkflowOutboundMessage");
        metaDataToDownload.add("WorkflowRule");
        metaDataToDownload.add("WorkflowTask");
        return metaDataToDownload;
    }


    public List<Metadata> getMetadataByType(String metaDataType) {

        List<Metadata> container = null;

        // Try to fix the case if we can
        List<String> types = getMetadataTypes();
        for (String type : types) {
            if (type.equalsIgnoreCase(metaDataType)) {
                metaDataType = type;
                break;
            }
        }

        if (metaDataType.equalsIgnoreCase("EmailTemplate")) {
            container = loadFolderableComponents("EmailTemplate", "Email", "EmailTemplate", "FolderId");

        } else if (metaDataType.equalsIgnoreCase("Document")) {
            container = loadFolderableComponents("Document", "Document", "Document", "FolderId");

        } else if (metaDataType.equalsIgnoreCase("Report")) {
            container = loadFolderableComponents("Report", "Report", "Report", "OwnerId");

        } else if (metaDataType.equalsIgnoreCase("Dashboard")) {
            container = loadFolderableComponents("Dashboard", "Dashboard", "Dashboard", "FolderId");

        } else {

                container = metaDataContainerByType.get(metaDataType);

                if (container == null) {
                    container = new ArrayList<Metadata>();

                    ListMetadataQuery mq = new ListMetadataQuery();
                    mq.setType(metaDataType);

                    ListMetadataQuery[] queries = new ListMetadataQuery[1];
                    queries[0] = mq;
                    FileProperties[] props;
                    try {
                        props = reconnector.listMetadata(queries, reconnector.getSfVersion());

                        for (FileProperties prop : props) {
                            String name;
                            if (prop.getFullName() == null) {
                                name = "[null]";
                            } else {
                                name = prop.getFullName();
                            }
                            Metadata md = new Metadata(name, prop.getLastModifiedByName(), prop.getFullName());
                            container.add(md);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    metaDataContainerByType.put(metaDataType, container);

            } else {
                container = new ArrayList<Metadata>();
            }
        }

        return container;
    }

    private List<Metadata> loadFolderableComponents(String metaDataType, String folderType, String tableName, String folderIdColumn) {
        List<Metadata>  container;
        container = metaDataContainerByType.get(metaDataType);

        if (container == null) {
            try {
                container = listFolderableResult( folderType, tableName, folderIdColumn);
                metaDataContainerByType.put(metaDataType, container);

            } catch (ConnectionException e) {
                throw new RuntimeException(e);
            }
        }
        return container;
    }

    public List<Metadata> listFolderableResult(String folderType, String tableName,
                                               String folderIdColumn) throws ConnectionException {

        // Load up folders
        Map<String, SObject> folderMapById = new HashMap<String, SObject>();

        QueryResult templates = reconnector.query(
                "select id, NamespacePrefix, DeveloperName, LastModifiedBy.Name" +
                        " from Folder where type = '" + folderType + "' order by DeveloperName");
        SObjectChunker chunker = new SObjectChunker(200, reconnector, templates);
        while (chunker.next()) {
            SObject[] chunk = chunker.nextChunk();
            for (SObject sObject : chunk) {
                folderMapById.put(sObject.getId(), sObject);
            }
        }

        Set<String> usedFolders = new HashSet<String>();

        List<Metadata> content = new ArrayList<Metadata>();

        // Load up thing that might be in the folders
        templates = reconnector.query(
                "select id, " + folderIdColumn + ", NamespacePrefix, DeveloperName, LastModifiedBy.Name" +
                        " from " + tableName + " order by DeveloperName");
        chunker = new SObjectChunker(200, reconnector, templates);
        while (chunker.next()) {
            SObject[] chunk = chunker.nextChunk();
            for (SObject sObject : chunk) {
                SObject folder = folderMapById.get((String) sObject.getField(folderIdColumn));

                String sfId = "";
                if (folder != null) {
                    usedFolders.add(folder.getId());

                    String NamespacePrefix = (String) folder.getField("NamespacePrefix");
                    if (NamespacePrefix != null) {
                        sfId += NamespacePrefix + "__";
                    }

                    sfId += folder.getField("DeveloperName") + "/";
                }
                if (sObject.getField("NamespacePrefix") != null) {
                    sfId += sObject.getField("NamespacePrefix") + "__";
                }

                Metadata folderableResult = new Metadata(
                        (String) sObject.getField("DeveloperName"),
                        (String) sObject.getChild("LastModifiedBy").getField("Name"),
                        sfId + sObject.getField("DeveloperName"));

                content.add(folderableResult);
            }
        }

        List<Metadata> result = new ArrayList<Metadata>();
        
        // Put any used folders at the top of the result. These might need to be deployed too
        // if the folders don't already exist.
        for (String usedFolder : usedFolders) {
            SObject sObject = folderMapById.get(usedFolder);

            String changedBy = (String) sObject.getChild("LastModifiedBy").getField("Name");
            String componentName = "/" + (String) sObject.getField("DeveloperName") + "/";

            String sfId = "";
            if (sObject.getField("NamespacePrefix") != null) {
                sfId += sObject.getField("NamespacePrefix") + ".";
            }

            sfId = sfId + sObject.getField("DeveloperName");

            Metadata folderableResult = new Metadata(componentName, changedBy, sfId);
            result.add(folderableResult);
        }
        result.addAll(content);
        return result;
    }


    public void emptyCache() {
        metaDataContainerByType = new HashMap<String, List<Metadata>>();
    }
}
