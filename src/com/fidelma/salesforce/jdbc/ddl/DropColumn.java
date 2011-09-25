package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.deployment.Deployer;
import com.fidelma.salesforce.deployment.Deployment;
import com.fidelma.salesforce.deployment.DeploymentEventListener;
import com.fidelma.salesforce.misc.Reconnector;
import com.fidelma.salesforce.parse.SimpleParser;
import com.sforce.soap.metadata.AsyncResult;

import java.sql.SQLException;

/**
 * ALTER TABLE <tableName> DROP COLUMN <columnName>
 */
public class DropColumn {
    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private Reconnector reconnector;

    public DropColumn(SimpleParser al, ResultSetFactory metaDataFactory, Reconnector reconnector) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.reconnector = reconnector;
    }

    public void execute(String tableName) throws Exception {
        String columnName = al.getToken().getValue();

        createMetadataXml(tableName, columnName);
        metaDataFactory.removeColumn(tableName, columnName);
    }


    public void createMetadataXml(String tableName, String columnName) throws Exception {
        Deployment deployment = new Deployment();
        deployment.dropMember("CustomField", tableName + "." + columnName);

        Deployer deployer = new Deployer(reconnector);
        final StringBuilder deployError = new StringBuilder();
        deployer.deploy(deployment, new DdlDeploymentListener(deployError, null));

        if (deployError.length() != 0) {
            throw new SQLException(deployError.toString());
        }
    }
}
