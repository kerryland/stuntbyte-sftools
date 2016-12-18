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

import com.stuntbyte.salesforce.database.migration.MigrationCriteria;
import com.stuntbyte.salesforce.database.migration.Migrator;
import com.stuntbyte.salesforce.database.migration.SimpleKeyBuilder;
import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.misc.StdOutDeploymentEventListener;
import com.stuntbyte.salesforce.misc.TestHelper;
import org.junit.Assert;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class MigratorTest {
    @Test
    // A dummy test so that JUNIT doesn't complain there is nothing to test in this class
    public void testNothing() throws Exception {
    }

//        @Test
    public void testDeleteEverything() throws Exception {

        SfConnection conn = TestHelper.getHackConnection();

        conn.createStatement().execute("drop table i_am_going__c if exists");

        // Create a known table in the hack salesforce instance
        PreparedStatement statement = conn.prepareStatement(
                "create table i_am_going__c(hello__c string(20))");

        statement.execute();
        // Prove the known table really exists
        statement.executeQuery("select count(*) from i_am_going__c");

        // Delete all tables from hack instance
        Migrator m = new Migrator();
        m.deleteAllTables(conn, new StdOutDeploymentEventListener());

        // This should now blow up
        try {
            statement.executeQuery("select count(*) from i_am_going__c");
            Assert.fail("Should have deleted the table!");
        } catch (SQLException e) {
            // Good!
        }
    }


    //    @Test
    public void testNotMuch() throws Exception {
        Class.forName("com.stuntbyte.salesforce.jdbc.SfDriver");

        Properties info = new Properties();
        info.put("user", TestHelper.username);
        info.put("password", TestHelper.password);
//        info.put("standard", "true");
//        info.put("includes", "Lead,Account");

        // Get a connection to the database
        SfConnection sourceSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:" + TestHelper.loginUrl
                , info);

        Class.forName("org.h2.Driver");

        info = new Properties();
//        info.put("user", "salesforce@fidelma.com");
//        info.put("password", "u9SABqa2dQxG0Y3kqWiJQVEwnYtryr1Ja1");
//        info.put("standard", "true");
//        info.put("includes", "Lead,Account");

        // Get a connection to the database
        Connection h2Conn = DriverManager.getConnection(
//                "jdbc:h2:mem:"
                "jdbc:h2:/tmp/sfdc-h2"
                , info);

        info = new Properties();
        info.put("user", "kerry@fidelma.com");
        info.put("password", "g2Py8oPzUAsZJ1VuDEw71tfV2pwqMJN5O");
//        info.put("standard", "true");
//        info.put("includes", "Lead,Account");

        // Get a connection to the database
        SfConnection destSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:" + TestHelper.loginUrl
                , info);

        Migrator migrator = new Migrator();
        migrator.replicate(sourceSalesforce, destSalesforce, h2Conn);
    }


    public static void main(String[] args) throws Exception {

        Class.forName("com.stuntbyte.salesforce.jdbc.SfDriver");

        Properties info = new Properties();
//        info.put("user", "kerry.sainsbury@nzpost.co.nz.sandbox");
//        info.put("password", "xJiKif3IeCLiZKNervuO3W3ozLxyQ6cm");


        info.put("user", "fronde.admin@localist.co.nz");
        info.put("password", "jrP2U0TnW09DesQIaxOmAb3yWiN9lRLu");
        SfConnection sourceSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:https://login.salesforce.com"
                , info);

        Class.forName("org.h2.Driver");

        info = new Properties();
        // Get a connection to the database
        Connection h2Conn = DriverManager.getConnection(
                "jdbc:h2:/tmp/sfdc-prod"
                , info);

        String destUser = "fronde.admin@localist.co.nz.preprod2";
        String destPwd = "jrP2U0TnW09DesQIaxOmAb3yWiN9lRLu";

//        String destUser = "fronde.admin@localist.co.nz.devkerry";
//        String destPwd = "jrP2U0TnCWok3CTtOfnhPC6UjYrOgQzI";
        info = new Properties();
        info.put("user", destUser);
        info.put("password", destPwd);

        // Get a connection to the database
        SfConnection destSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:https://test.salesforce.com"
                , info);

        // TODO: 1. Sort these into a dependency order
        // TODO: 2.
        // TODO: Maybe NOT disable some explicitly listed workflow or triggers?


        // Don't migrate these -- just use the data that's already there
        List<MigrationCriteria> existingDataCriteriaList = new ArrayList<MigrationCriteria>();
        existingDataCriteriaList.add(new MigrationCriteria(
                "c2g__codaCompany__c",
                "",
                "Name",
                new SimpleKeyBuilder("Name")));


        existingDataCriteriaList.add(new MigrationCriteria("c2g__codaAccountingCurrency__c", "",
                "Name",
                new SimpleKeyBuilder("Name")));

//        existingDataCriteriaList.add(new MigrationCriteria("c2g__codaGeneralLedgerAccount__c", ""));


        List<MigrationCriteria> migrationCriteriaList = new ArrayList<MigrationCriteria>();

//          a11Q0000000M5LQIA0 --> a0zS0000000EIGtIAO

        migrationCriteriaList.add(new MigrationCriteria("c2g__codaDimension1__c", ""));
        migrationCriteriaList.add(new MigrationCriteria("c2g__codaDimension2__c", ""));
        migrationCriteriaList.add(new MigrationCriteria("c2g__codaDimension3__c", ""));
        migrationCriteriaList.add(new MigrationCriteria("c2g__codaDimension4__c", ""));
        migrationCriteriaList.add(new MigrationCriteria("c2g__codaGeneralLedgerAccount__c", ""));
        migrationCriteriaList.add(new MigrationCriteria("c2g__codaTaxCode__c", ""));
//        migrationCriteriaList.add(new MigrationCriteria("c2g__codaTaxRate__c", ""));
        migrationCriteriaList.add(new MigrationCriteria("c2g__codaBankAccount__c", ""));
        migrationCriteriaList.add(new MigrationCriteria("c2g__codaIncomeScheduleDefinition__c", ""));


        migrationCriteriaList.add(new MigrationCriteria("Location__c", "where name like 'S%'"));
        migrationCriteriaList.add(new MigrationCriteria("Location_Relationship__c",
                "where Associated_Location__r.Name like 'S%' and Location__r.Name like 'S%'"));

        migrationCriteriaList.add(new MigrationCriteria("Term_Or_Condition_Specification__c"));
        migrationCriteriaList.add(new MigrationCriteria("localist_product_specification__c"));

        migrationCriteriaList.add(new MigrationCriteria("Presence_Category_Group__c",
                "where id in (\n" +
                        "select Presence_Category_Group__c\n" +
                        "from Presence_Category_Group_Member__c\n" +
                        "where Presence_Category__r.name like 'B%')"));

        migrationCriteriaList.add(new MigrationCriteria("Presence_Category_Group__c",
                "where parent__c = null"));

        migrationCriteriaList.add(new MigrationCriteria("Presence_Category__c", "where name like 'B%'"));

        migrationCriteriaList.add(new MigrationCriteria("Presence_Category_Group_Member__c",
                "where Presence_Category__r.name like 'B%' or Presence_Category_Group__r.parent__c = null"));

        migrationCriteriaList.add(new MigrationCriteria("Print_Book_Service__c"));
        migrationCriteriaList.add(new MigrationCriteria("product2"));
        migrationCriteriaList.add(new MigrationCriteria("Localist_Product_Offering_Price__c",
                "where Active__c = 'True'"));

        migrationCriteriaList.add(new MigrationCriteria("Account",
                "where Name = 'Duplicate DO NOT USE - TEST - SMOKE'"));

        migrationCriteriaList.add(new MigrationCriteria("Party_Relationship__c",
                "where Associated_Organisation__c in (select id from account where Name = 'Duplicate DO NOT USE - TEST - SMOKE')" +
                        "and Organisation__c = null"));

        migrationCriteriaList.add(new MigrationCriteria("Contact",
                "where id in \n" +
                        "(select person__c from Party_Relationship__c\n" +
                        "  where Associated_Organisation__r.Name = 'Duplicate DO NOT USE - TEST - SMOKE'\n" +
                        "   and Organisation__c = null)"));

        migrationCriteriaList.add(new MigrationCriteria("Contact_Media__c",
                "where person__c in \n" +
                        "(select person__c from Party_Relationship__c\n" +
                        "  where Associated_Organisation__r.Name = 'Duplicate DO NOT USE - TEST - SMOKE'\n" +
                        "   and Organisation__c = null)"));

        Migrator migrator = new Migrator();
        migrator.migrateData(sourceSalesforce, destSalesforce, h2Conn, migrationCriteriaList, existingDataCriteriaList);


    }


}
