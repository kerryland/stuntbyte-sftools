package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.Deployment;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.metadata.MetadataConnection;

import java.sql.SQLException;

/**
 * ALTER TABLE <tableName> DROP COLUMN <columnName>
 */
public class DropColumn {
    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private MetadataConnection metadataConnection;

    public DropColumn(SimpleParser al, ResultSetFactory metaDataFactory, MetadataConnection metadataConnection) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.metadataConnection = metadataConnection;
    }

    public void execute(String tableName) throws Exception {
        String columnName = al.getToken().getValue();

        createMetadataXml(tableName, columnName);
        metaDataFactory.removeColumn(tableName, columnName);
    }


    public void createMetadataXml(String tableName, String columnName) throws Exception {
        Deployment deployment = new Deployment();
        deployment.addMember("CustomField", tableName + "." + columnName, null);
//        deployment.assemble();

        Deployer deployer = new Deployer(metadataConnection);
        final StringBuilder deployError = new StringBuilder();
        deployer.undeploy(deployment,
                new DeploymentEventListener() {
                    public void error(String message) {
                        deployError.append(message).append("\n");
                    }

                    public void finished(String message) {

                    }
                });
        if (deployError.length() != 0) {
            throw new SQLException(deployError.toString());
        }
    }
}
