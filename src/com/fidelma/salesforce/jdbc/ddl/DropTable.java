package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.SQLException;

/**
 * DROP TABLE <tableName> [ IF EXISTS ]
 */
public class DropTable {
    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private MetadataConnection metadataConnection;

    public DropTable(SimpleParser al, ResultSetFactory metaDataFactory, MetadataConnection metadataConnection) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.metadataConnection = metadataConnection;
    }

    public void execute() throws Exception {
        String tableName = al.getToken().getValue();

        String value = al.getValue();
        if (value != null) {
            al.assertEquals("if", value);
            al.read("exists");

            try {
                metaDataFactory.getTable(tableName);
            } catch (SQLException e) {
                return; // It doesn't exist
            }
        }
        createMetadataXml(tableName);
        metaDataFactory.removeTable(tableName);
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
