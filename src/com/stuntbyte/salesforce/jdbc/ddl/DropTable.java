package com.stuntbyte.salesforce.jdbc.ddl;

import com.stuntbyte.salesforce.deployment.Deployer;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.deployment.Deployment;
import com.stuntbyte.salesforce.misc.Reconnector;
import com.stuntbyte.salesforce.parse.SimpleParser;

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
                deployment.dropMember("CustomObject", tableName);
            }
            deployer.deploy(deployment, new DdlDeploymentListener(deployError, null));

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
