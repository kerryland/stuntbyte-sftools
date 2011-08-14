package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.misc.Deployer;
import com.fidelma.salesforce.misc.Deployment;
import com.fidelma.salesforce.misc.DeploymentEventListener;
import com.fidelma.salesforce.misc.Reconnector;
import com.fidelma.salesforce.parse.SimpleParser;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DROP TABLE <tableName> [ IF EXISTS ]
 */
public class DropTable {
    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private Reconnector reconnector;

    public DropTable(SimpleParser al, ResultSetFactory metaDataFactory, Reconnector reconnector) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.reconnector = reconnector;
    }

    public void execute() throws SQLException {
        String tableName = parse();

        if (tableName != null) {
            List<String> tablesToDrop = new ArrayList<String>();
            tablesToDrop.add(tableName);
            dropTables(tablesToDrop);
        }
    }

    public String parse() throws SQLException {
        try {
            String tableName = al.getToken().getValue();

            String value = al.getValue();
            if (value != null) {
                al.assertEquals("if", value);
                al.read("exists");

                try {
                    metaDataFactory.getTable(tableName);
                } catch (SQLException e) {
                    tableName = null;
                }
            }
            return tableName;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }


    public void dropTables(List<String> tablesToDrop) throws SQLException {
        Deployer deployer = new Deployer(reconnector);
        final StringBuilder deployError = new StringBuilder();

        Deployment deployment = new Deployment();

        try {
            for (String tableName : tablesToDrop) {
                deployment.addMember("CustomObject", tableName, null, null);
            }
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

        } catch (Exception e) {
            throw new SQLException(e);
        }

        if (deployError.length() != 0) {
            throw new SQLException(deployError.toString());
        }

        for (String tableName : tablesToDrop) {
            metaDataFactory.removeTable(tableName);
        }

    }
}
