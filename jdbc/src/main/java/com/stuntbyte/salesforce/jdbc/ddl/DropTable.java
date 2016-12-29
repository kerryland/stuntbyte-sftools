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
                    metaDataFactory.getTable(ResultSetFactory.schemaName, tableName);
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

        Deployment deployment = new Deployment(reconnector.getSfVersion());

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
