package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * DROP TABLE <tableName>
 */
public class DropTable {
    private SimpleParser al;
    private MetadataConnection metadataConnection;

    public DropTable(SimpleParser al, MetadataConnection metadataConnection) {
        this.al = al;
        this.metadataConnection = metadataConnection;
    }

    public void execute() throws Exception {
        String tableName = al.getToken().getValue();

        // TODO: IF EXISTS
        createMetadataXml(tableName);

        // TODO: Update local cache of table and column information
    }

    public void createMetadataXml(String tableName) throws Exception {
        Deployer deployer = new Deployer(metadataConnection);
        deployer.dropNonCode(
                "CustomObject",
                "/objects/" + tableName + ".object",
                new DeploymentEventListener() {
                    public void heyListen(String message) {
                        System.out.println("HEY! " + message);
                    }
                });
    }
}
