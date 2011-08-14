package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.Deployment;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.misc.Reconnector;
import com.fidelma.salesforce.parse.SimpleParser;

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
        deployment.addMember("CustomField", tableName + "." + columnName, null, null);
//        deployment.assemble();

        Deployer deployer = new Deployer(reconnector);
        final StringBuilder deployError = new StringBuilder();
        deployer.undeploy(deployment,
                new DeploymentEventListener() {
                    public void error(String message) {
                        deployError.append(message).append("\n");
                    }

                    public void finished(String message) {

                    }

                    public void progress(String message) {

                    }
                });
        if (deployError.length() != 0) {
            throw new SQLException(deployError.toString());
        }
    }
}
