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
package com.stuntbyte.salesforce.jdbc.dml;

import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.misc.TestHelper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 */
public class GrantTests {

    private static SfConnection conn = null;
    private static SfConnection plebconn = null;

    private static TestHelper testHelper = new TestHelper();

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        conn = testHelper.getTestConnection();
    }

    @Test
    public void testGrantStatement() throws Exception {

        Statement godStmt = conn.createStatement();

        godStmt.execute("drop table grant_test__c if exists");
        godStmt.execute("create table grant_test__c(my_num__c Int)");
        godStmt.execute("grant object create,update,delete,read on grant_test__c to *");

        // Insert a row
        plebconn = testHelper.getPlebConnection();
        Statement plebStmt = plebconn.createStatement();

        plebStmt.execute("insert into grant_test__c(my_num__c) values (1)");

        // Revoke create, update and delete
        conn.createStatement().execute("revoke object create,update,delete on grant_test__c from *");

        // Try to insert a second row -- it should fail (but it doesn't!)
        try {
            plebStmt.execute("insert into grant_test__c(my_num__c) values (2)");
            Assert.fail("Should not be able to insert");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage(),
                    e.getMessage().startsWith("entity type cannot be inserted: grant_test__c."));
        }

        // Try to change the first row -- it should fail
        try {
            plebStmt.executeUpdate("update grant_test__c set my_num__c = 3");
            Assert.fail("Exception should have been thrown");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("entity type cannot be updated: grant_test__c"));
        }

        // Try to delete the first row -- it should fail
        try {
            plebStmt.executeUpdate("delete from grant_test__c");
            Assert.fail("Exception should have been thrown");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("insufficient access rights on object"));
        }

        // Enable create, update and delete
        godStmt.execute("grant object create,update,delete on grant_test__c to *");

        // Try to change the first row -- it should pass
        int count = plebStmt.executeUpdate("update grant_test__c set my_num__c = 3 where my_num__c = 1");
        Assert.assertEquals(1, count);

        // Try to delete the first row -- it should pass
        count = plebStmt.executeUpdate("delete from grant_test__c where my_num__c = 3");
        Assert.assertEquals(1, count);

        // Try to insert a second row -- it should pass
        plebStmt.execute("insert into grant_test__c(my_num__c) values (2)");

        // Confirm we can read the data
        ResultSet rs = plebStmt.executeQuery("select my_num__c from grant_test__c");
        count = 0;
        while (rs.next()) {
            count++;
            Assert.assertEquals(2, rs.getInt("my_num__c"));
        }
        Assert.assertEquals(1, count);


        // Make the field non-visible
        godStmt.execute("REVOKE FIELD VISIBLE ON grant_test__c.my_num__c FROM *");

        try {
            plebStmt.executeQuery("select my_num__c from grant_test__c");
            Assert.fail("Exception should have been thrown");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("No such column 'my_num__c' on entity 'grant_test__c'"));
        }
    }

    @Test
    public void testCantChangeStandardFields() throws Exception {
        try {
            conn.createStatement().execute("REVOKE FIELD EDITABLE ON abc__c.Name FROM 'MarketingProfile'");
            Assert.fail("Should've thrown an exception");
        } catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("Cannot change permissions of standard fields"));
        }

    }
}
