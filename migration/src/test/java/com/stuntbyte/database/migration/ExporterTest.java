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
import com.stuntbyte.salesforce.database.migration.Exporter;
import com.stuntbyte.salesforce.database.migration.Migrator;
import com.stuntbyte.salesforce.jdbc.SfConnection;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

/**
 */
public class ExporterTest {
    private static SfConnection sfconn = null;

    private static MigrationTestHelper testHelper = new MigrationTestHelper();

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        sfconn = testHelper.getSourceConnection();
    }


    @Test
    @Ignore // TODO: Broken by API version 29. Subsequently fixed in 31, but I don't want to deal with that update just yet!
            // TODO: This test does too much
    // https://success.salesforce.com/issues_view?id=a1p30000000T5S8AAK
    public void testMigration() throws Exception {
        // Get a connection to the local database
        Properties info = new Properties();
        Connection h2Conn = DriverManager.getConnection(
//                "jdbc:h2:mem:"
                "jdbc:h2:/tmp/sfdc-h2"
                , info);

        // create salesforce tables with a relationship
        Statement sfdc = sfconn.createStatement();

        /*

         | TWO |
            ^      (Lookup)
         | ONE |
            ^      (Masterdetail)
         | THREE |
            ^      (Masterdetail)
         | FOUR |


        one__c.tworef__c    -- Lookup --> two__c
        three__c.one__c     -- Master/Detail child --> one__c
        four__c.threeref__c  -- Master/Detail child --> three__c
         */
        sfdc.addBatch("drop table three__c if exists");
        sfdc.addBatch("drop table one__c if exists");
        sfdc.addBatch("drop table two__c if exists");
        sfdc.addBatch("drop table four__c if exists");
        sfdc.executeBatch();

        sfdc.addBatch("create table two__c(Name__c string(20), SomeDate__c DateTime)");
        sfdc.addBatch("create table one__c(tworef__c reference references(two__c) with (relationshipName 'RefLookup'))");
        sfdc.addBatch("create table three__c(oneref__c masterrecord references(one__c) with (relationshipName 'oneChild'))");
        sfdc.addBatch("create table four__c(" +
                     "threeref__c masterRecord references(three__c) with (relationshipName 'threeChild'))");

        sfdc.executeBatch();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        PreparedStatement psfdc = sfconn.prepareStatement("insert into two__c(Name__c, SomeDate__c) values (?,?)");
        psfdc.setString(1, "Norman");
        cal.clear();
        cal.set(2010, Calendar.FEBRUARY, 11, 17, 0);
        psfdc.setTimestamp(2, new Timestamp(cal.getTimeInMillis()));
        psfdc.addBatch();

        psfdc.setString(1, "Wibbles");
        cal.set(2010, Calendar.FEBRUARY, 15, 17, 0);
        psfdc.setTimestamp(2, new Timestamp(cal.getTimeInMillis()));
        psfdc.addBatch();

        psfdc.executeBatch();

//        sfdc.addBatch("insert into two__c(Name__c) values ('Norman')");
//        sfdc.addBatch("insert into two__c(Name__c) values ('Wibbles')");
//        sfdc.executeBatch();

        ResultSet keyRs = psfdc.getGeneratedKeys();
        keyRs.next();
        String twoNormanId = keyRs.getString(1);
        keyRs.next();
        String twoWibblesId = keyRs.getString(1);

        sfdc.addBatch("insert into one__c(tworef__c) values ('" + twoNormanId + "')");
        sfdc.addBatch("insert into one__c(tworef__c) values ('" + twoWibblesId + "')");
        sfdc.executeBatch();
        keyRs = sfdc.getGeneratedKeys();
        keyRs.next();
        String oneNormanId = keyRs.getString(1);
        keyRs.next();
        String oneWibblesId = keyRs.getString(1);

        sfdc.execute("insert into three__c(Name, oneref__c) values ('Kerry', '" + oneNormanId + "')");
        keyRs = sfdc.getGeneratedKeys();
        keyRs.next();
        String threeId = keyRs.getString(1);

        sfdc.execute("insert into four__c(threeref__c) values ('" + threeId + "')");


        // Pull data down into local database
        List<MigrationCriteria> criteriaList = new ArrayList<MigrationCriteria>();
        MigrationCriteria criteria = new MigrationCriteria("one__c");
        criteriaList.add(criteria);
        criteria = new MigrationCriteria("two__c");
        criteriaList.add(criteria);
        criteria = new MigrationCriteria("three__c");
        criteriaList.add(criteria);
        criteria = new MigrationCriteria("four__c");
        criteriaList.add(criteria);

        Set<String> processedTables = new HashSet<>();

        Exporter exporter = new Exporter();
        exporter.createLocalSchema(sfconn, h2Conn, null);
        exporter.downloadData(sfconn, h2Conn, criteriaList, processedTables);

        // Delete the data from the destination salesforce
        sfdc.execute("delete from one__c");
        sfdc.execute("delete from two__c");
        sfdc.execute("delete from three__c");
        sfdc.execute("delete from four__c");

        Migrator migrator = new Migrator();
        criteriaList.clear();
        criteria = new MigrationCriteria("one__c");
        criteriaList.add(criteria);
        criteria = new MigrationCriteria("two__c");
        criteriaList.add(criteria);
        criteria = new MigrationCriteria("three__c");
        criteriaList.add(criteria);
        criteria = new MigrationCriteria("four__c");
        criteriaList.add(criteria);


        // Push rows back to Salesforce
        migrator.restoreRows(sfconn, h2Conn, criteriaList,
                new ArrayList<MigrationCriteria>(), "Bob");  // TODO: Test this!

        // Check salesforce has the same data, but with different ids
        ResultSet rs = sfdc.executeQuery("select * from two__c order by Name__c");
        rs.next();
        String newTwoNormanId = rs.getString("Id");
        Assert.assertNotSame(newTwoNormanId, twoNormanId);
        Assert.assertEquals("Norman", rs.getString("Name__c"));
        rs.next();
        String newTwoWibbleId = rs.getString("Id");
        Assert.assertNotSame(newTwoWibbleId, twoWibblesId);
        Assert.assertEquals("Wibbles", rs.getString("Name__c"));
        Assert.assertFalse(rs.next());

        rs = sfdc.executeQuery("select Id, tworef__c, twoRef__r.Name__c from one__c order by twoRef__r.Name__c");
        rs.next();
        String newOneNormanId = rs.getString("Id");
        Assert.assertNotSame(newOneNormanId, oneNormanId);
        Assert.assertEquals("Norman", rs.getString("tworef__r.Name__c"));
        Assert.assertEquals(newTwoNormanId, rs.getString("tworef__c"));  // Do we refer to the new "two__c.Id"?
        rs.next();
        String newOneWibblesId = rs.getString("Id");
        Assert.assertNotSame(newOneWibblesId, oneWibblesId);
        Assert.assertEquals("Wibbles", rs.getString("tworef__r.Name__c"));
        Assert.assertEquals(newTwoWibbleId, rs.getString("tworef__c"));  // Do we refer to the new "two__c.Id"?

        Assert.assertFalse(rs.next());

        rs = sfdc.executeQuery("select Name, OneRef__r.TwoRef__r.Name__c, OneRef__r.TwoRef__r.SomeDate__c from three__c");
        rs.next();
        Assert.assertEquals("Kerry", rs.getString(1));
        Assert.assertEquals("Norman", rs.getString(2));
        Timestamp ts = rs.getTimestamp(3);

        cal.setTimeInMillis(ts.getTime());
        System.out.println("Back " + ts.getTime());
        Assert.assertEquals(2010, cal.get(Calendar.YEAR));
        Assert.assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH));
        Assert.assertEquals(11, cal.get(Calendar.DATE));
        Assert.assertEquals(17, cal.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, cal.get(Calendar.MINUTE));

    }
}
