package com.stuntbyte.salesforce.jdbc.ddl;

import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.deployment.Deployer;
import com.stuntbyte.salesforce.deployment.Deployment;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.stuntbyte.salesforce.parse.SimpleParser;

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
        Deployment deployment = new Deployment(reconnector.getSfVersion());
        deployment.dropMember("CustomField", tableName + "." + columnName);

        Deployer deployer = new Deployer(reconnector);
        final StringBuilder deployError = new StringBuilder();
        deployer.deploy(deployment, new DdlDeploymentListener(deployError, null));

        if (deployError.length() != 0) {
            throw new SQLException(deployError.toString());
        }
    }
}
