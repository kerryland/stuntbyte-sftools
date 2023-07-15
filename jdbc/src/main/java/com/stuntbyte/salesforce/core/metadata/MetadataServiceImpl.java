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
package com.stuntbyte.salesforce.core.metadata;

import com.stuntbyte.salesforce.jdbc.dml.SObjectChunker;
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

    private static List<String> metaDataTypes;

    {
        // https://developer.salesforce.com/docs/atlas.en-us.api_meta.meta/api_meta/meta_types_list.htm
        // More, or less.
        metaDataTypes = new ArrayList<String>();

        metaDataTypes.add("AccountCriteriaBasedSharingRule");
        metaDataTypes.add("ActionLinkGroupTemplate");
        metaDataTypes.add("AnalyticSnapshot");
        metaDataTypes.add("ApexClass");
        metaDataTypes.add("ApexComponent");
        metaDataTypes.add("ApexPage");
        metaDataTypes.add("ApexTrigger");
        metaDataTypes.add("AppMenu");
        metaDataTypes.add("ApprovalProcess");
        metaDataTypes.add("AssignmentRules");
        metaDataTypes.add("AuthProvider");
        metaDataTypes.add("AuraDefinitionBundle");
        metaDataTypes.add("AutoResponseRules");
        metaDataTypes.add("BusinessProcess");
        metaDataTypes.add("CallCenter");
        metaDataTypes.add("CampaignCriteriaBasedSharingRule");
        metaDataTypes.add("CaseCriteriaBasedSharingRule");
        metaDataTypes.add("Certificate");
        metaDataTypes.add("CleanDataService");
        metaDataTypes.add("Community"); // Zone
        metaDataTypes.add("CommunityTemplateDefinition");
        metaDataTypes.add("CommunityThemeDefinition");
        metaDataTypes.add("CompactLayout");
        metaDataTypes.add("ConnectedApp");
        metaDataTypes.add("ContactCriteriaBasedSharingRule");
        metaDataTypes.add("ContentAsset");
        metaDataTypes.add("CorsWhitelistOrigin");
        metaDataTypes.add("CustomApplication");
        metaDataTypes.add("CustomApplicationComponent");
        metaDataTypes.add("CustomFeedFilter");
        metaDataTypes.add("CustomField");
        metaDataTypes.add("CustomLabel");
        metaDataTypes.add("CustomObject");
        metaDataTypes.add("CustomObjectCriteriaBasedSharingRule");
        metaDataTypes.add("CustomObjectTranslation");
        metaDataTypes.add("CustomPageWebLink");
        metaDataTypes.add("CustomPermission");
        metaDataTypes.add("CustomSite");
        metaDataTypes.add("CustomTab");
        metaDataTypes.add("Dashboard");
        metaDataTypes.add("DataCategoryGroup");
        metaDataTypes.add("DelegateGroup");
        metaDataTypes.add("Document");
        metaDataTypes.add("DuplicateRule");
        metaDataTypes.add("EmailTemplate");
        metaDataTypes.add("EntitlementProcess");
        metaDataTypes.add("EntitlementTemplate");
        metaDataTypes.add("ExternalDataSource");
        metaDataTypes.add("FieldSet");
        metaDataTypes.add("Flow");
        metaDataTypes.add("FlowDefinition");
        metaDataTypes.add("GlobalValueSetTranslation");
        metaDataTypes.add("Group");
        metaDataTypes.add("HomePageComponent");
        metaDataTypes.add("HomePageLayout");
        metaDataTypes.add("InstalledPackage");
        metaDataTypes.add("KeywordList");
        metaDataTypes.add("Layout");
        metaDataTypes.add("LeadCriteriaBasedSharingRule");
        metaDataTypes.add("Letterhead");
        metaDataTypes.add("ListView");
        metaDataTypes.add("LiveChatButton");
        metaDataTypes.add("LiveChatDeployment");
        metaDataTypes.add("LiveChatSensitiveDataRule");
        metaDataTypes.add("ManagedTopics");
        metaDataTypes.add("MatchingRule");
        metaDataTypes.add("MilestoneType");
        metaDataTypes.add("ModerationRule");
        metaDataTypes.add("NamedCredential");
        metaDataTypes.add("NamedFilter");
        metaDataTypes.add("Network");
        metaDataTypes.add("OpportunityCriteriaBasedSharingRule");
        metaDataTypes.add("PathAssistant");
        metaDataTypes.add("PermissionSet");
        metaDataTypes.add("PlatformCachePartition");
        metaDataTypes.add("Portal");
        metaDataTypes.add("PostTemplate");
        metaDataTypes.add("Profile");
        metaDataTypes.add("Queue");
        metaDataTypes.add("QuickAction");
        metaDataTypes.add("RecordType");
        metaDataTypes.add("RemoteSiteSetting");
        metaDataTypes.add("Report");
        metaDataTypes.add("ReportType");
        metaDataTypes.add("Role");
        metaDataTypes.add("SamlSsoConfig");
        metaDataTypes.add("Scontrol");
        metaDataTypes.add("Settings");
        metaDataTypes.add("SharingReason");
        metaDataTypes.add("SharingRules");
        metaDataTypes.add("SiteDotCom");
        metaDataTypes.add("Skill");
        metaDataTypes.add("StandardValueSet");
        metaDataTypes.add("StandardValueSetTranslation");
        metaDataTypes.add("StaticResource");
        metaDataTypes.add("SynonymDictionary");
        metaDataTypes.add("Territory");
        metaDataTypes.add("Territory2");
        metaDataTypes.add("Territory2Model");
        metaDataTypes.add("Territory2Rule");
        metaDataTypes.add("Territory2Type");
        metaDataTypes.add("TransactionSecurityPolicy");
        metaDataTypes.add("Translations");
        metaDataTypes.add("UserCriteriaBasedSharingRule");
        metaDataTypes.add("ValidationRule");
        metaDataTypes.add("WaveApplication");
        metaDataTypes.add("WaveDashboard");
        metaDataTypes.add("WaveDataflow");
        metaDataTypes.add("WaveDataset");
        metaDataTypes.add("WaveLens");
        metaDataTypes.add("WaveTemplateBundle");
        metaDataTypes.add("WebLink");
        metaDataTypes.add("Workflow");
        metaDataTypes.add("WorkflowAlert");
        metaDataTypes.add("WorkflowFieldUpdate");
        metaDataTypes.add("WorkflowFlowAction");
        metaDataTypes.add("WorkflowKnowledgePublish");
        metaDataTypes.add("WorkflowOutboundMessage");
        metaDataTypes.add("WorkflowRule");
        metaDataTypes.add("WorkflowTask");
        metaDataTypes = Collections.unmodifiableList(metaDataTypes);

    }

    public MetadataServiceImpl(Reconnector reconnector) {
        this.reconnector = reconnector;
    }


    public List<String> getMetadataTypes() {
        return metaDataTypes;
    }


    public List<Metadata> getMetadataByType(String metaDataType) {

        List<Metadata> container;

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
