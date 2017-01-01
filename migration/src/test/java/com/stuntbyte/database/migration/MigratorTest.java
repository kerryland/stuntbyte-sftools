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
package com.stuntbyte.database.migration;

import com.stuntbyte.salesforce.database.migration.Exporter;
import com.stuntbyte.salesforce.database.migration.MigrationCriteria;
import com.stuntbyte.salesforce.database.migration.Migrator;
import com.stuntbyte.salesforce.database.migration.SimpleKeyBuilder;
import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.jdbc.metaforce.ResultSetFactory;
import com.stuntbyte.salesforce.jdbc.metaforce.Table;
import org.junit.Assert;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class MigratorTest {
    private static MigrationTestHelper testHelper = new MigrationTestHelper();

    @Test
    // Migrate data from one instance of Salesforce to another
    public void testMigrate() throws Exception {
        Class.forName("com.stuntbyte.salesforce.jdbc.SfDriver");

        // Get a connection to the database
        SfConnection sourceSalesforce = testHelper.getSourceConnection();

        // Somewhere to store data en-route to the destination
        Class.forName("org.h2.Driver");
        Connection h2Conn = DriverManager.getConnection(
                "jdbc:h2:mem:"
//                "jdbc:h2:/tmp/sfdc-h2"
                , new Properties());

        // Create the table in the source database, and populate it
        Statement sourceStatement = sourceSalesforce.createStatement();
        sourceStatement.execute("drop table i_am_going__c if exists");
        sourceStatement.execute("create table i_am_going__c(hello__c string(20))");
        sourceStatement.execute("insert into i_am_going__c(hello__c) values ('one')");
        sourceStatement.execute("insert into i_am_going__c(hello__c) values ('two')");
        sourceStatement.execute("insert into i_am_going__c(hello__c) values ('three')");

        // Create the table in the destination database, unpopulated
        SfConnection destSalesforce = testHelper.getDestConnection();
        Statement destStatement = destSalesforce.createStatement();

        destStatement.execute("drop table i_am_going__c if exists");
        destStatement.execute("create table i_am_going__c(hello__c string(20))");

        List<MigrationCriteria> existingDataCriteriaList = new ArrayList<>();
        // TODO: Create table (eg: country codes) in source and dest but do not migrate it as example of existingDataCriteriaList
        // Don't migrate these -- just use the data that's already there in the destination instance.
        // Define the primary key as 'name' so we can map source data to the destination rows.
//        existingDataCriteriaList.add(new MigrationCriteria(
//                "country_code__c",
//                "",
//                "Name",
//                new SimpleKeyBuilder("Name")));
//
        Migrator migrator = new Migrator();

        Exporter exporter = new Exporter();
        List<Table> tables = exporter.createLocalSchema(sourceSalesforce, h2Conn, "i_am_going__c");

        // Migrate our tables
        List<MigrationCriteria> criteriaList = new ArrayList<>();
        for (Table table : tables) {
            if (table.getType().equals("TABLE") && table.getSchema().equals(ResultSetFactory.schemaName)) {
                MigrationCriteria criteria = new MigrationCriteria(table.getName());
                criteriaList.add(criteria);
            }
        }

        migrator.migrateData(sourceSalesforce, destSalesforce, h2Conn, criteriaList, existingDataCriteriaList, "Bob");

        // Prove the known table really exists in the destination database
        ResultSet resultSet = destSalesforce.createStatement().executeQuery("select count(*) from i_am_going__c");
        Assert.assertTrue(resultSet.next());
        Assert.assertEquals(3, resultSet.getInt(1));

    }

    // TODO: Migrate master-detail
    // TODO: Migrate with user
    // TODO: Migrate with existing data (existingDataCriteriaList)



}
