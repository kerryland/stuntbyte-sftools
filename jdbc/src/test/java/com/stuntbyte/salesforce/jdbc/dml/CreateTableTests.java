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
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 */

public class CreateTableTests {

    private static SfConnection conn = null;

    private static TestHelper testHelper = new TestHelper();

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        createConnection();
    }

    private static void createConnection() throws SQLException {
        conn = testHelper.getTestConnection();
    }


    @Test
    public void testCreateStatement() throws Exception {
        conn.createStatement().execute("drop table wibble__c if exists");

        String sql = "create table wibble__c(" +
                "Spang__c INT, " +
                "Namero__c string(20)," +
                "price__c decimal(16,2)," +
                "description__c longTextArea(12000) with (visibleLines 3)," +
                "colour__c picklist('red', 'blue' default, 'green') sorted" +
                ")";
        conn.createStatement().execute(sql);


        ResultSet rs = conn.getMetaData().getColumns(null, null, "wibble__c", "%");

        Set<String> names = new HashSet<String>();
        names.add("Namero__c");
        names.add("Spang__c");
        names.add("price__c");
        names.add("colour__c");
//        names.add("Hoover__c");

        while (rs.next()) {
            names.remove(rs.getString("COLUMN_NAME"));
        }
        Assert.assertEquals(0, names.size());


        PreparedStatement pstmt = conn.prepareStatement("insert into wibble__c(Spang__c, Namero__c, price__c) values (?,?,?)");
        pstmt.setInt(1, 70);
        pstmt.setString(2, "Seventy");
        pstmt.setBigDecimal(3, new BigDecimal("70.77"));
        pstmt.execute();


        // TODO: Improve this test

        sql = "alter table wibble__c add Hoover__c int";
        conn.createStatement().execute(sql);


        // TODO: This doesn't pass:

        rs = conn.getMetaData().getColumns(null, null, "wibble__c", "%");
        names = new HashSet<String>();
//        names.add("Namero__c");      // TODO: Make this pass
//        names.add("Spang__c");       // TODO: Make this pass
//        names.add("price__c");       // TODO: Make this pass
        names.add("Hoover__c");

        while (rs.next()) {
            names.remove(rs.getString("COLUMN_NAME"));
        }
        Assert.assertEquals(0, names.size());

        sql = "alter table wibble__c drop column spang__c";

        conn.createStatement().execute(sql);

        createConnection();
        rs = conn.getMetaData().getColumns(null, null, "wibble__c", "%");
        names = new HashSet<String>();
        names.add("Namero__c");
        names.add("price__c");
        names.add("Hoover__c");
        names.add("colour__c");
        while (rs.next()) {
            names.remove(rs.getString("COLUMN_NAME"));
        }
        Assert.assertEquals(0, names.size());

        rs = conn.prepareStatement("select * from wibble__c").executeQuery();
        Assert.assertTrue(rs.next());
        Assert.assertEquals("blue", rs.getString("colour__c"));
        Assert.assertFalse(rs.next());
    }


    @Test
    public void testAutonumberName() throws Exception {
        conn.createStatement().execute("drop table wibble__c if exists");

        String sql = "create table wibble__c(" +
                "name autonumber, " +    // TODO: Handle case automatically if needed
                "custom_field__c string(10) )";

        conn.createStatement().execute(sql);

        Statement stmt = conn.createStatement();
        stmt.execute("insert into wibble__c(custom_field__c) values ('Zeta')");
        stmt.execute("insert into wibble__c(custom_field__c) values ('Alpha')");

        ResultSet rs = conn.prepareStatement("select Name, Custom_Field__c from wibble__c order by name").executeQuery();
        Assert.assertTrue(rs.next());
        Assert.assertEquals("0000000000", rs.getString("name"));
        Assert.assertEquals("Zeta", rs.getString("custom_field__c"));
        Assert.assertTrue(rs.next());
        Assert.assertEquals("0000000001", rs.getString("name"));
        Assert.assertEquals("Alpha", rs.getString("custom_field__c"));
        Assert.assertFalse(rs.next());
    }


    // Do we create a name column automatically?
    @Test
    public void testDefaultNameColumn() throws Exception {
        conn.createStatement().execute("drop table wibble__c if exists");

        String sql = "create table wibble__c(" +
                "custom_field__c string(10) )";

        conn.createStatement().execute(sql);

        SfConnection conn2 = testHelper.getTestConnection();

        Assert.fail("Clean this up");
        ResultSet wibble__c = conn.getMetaData().getTables(null, null, "wibble__c", null);
        while(wibble__c.next()) {
            System.out.println(wibble__c.getString("TABLE_NAME"));
        }

        wibble__c = conn2.getMetaData().getTables(null, null, "wibble__c", null);
        while(wibble__c.next()) {
            System.out.println(wibble__c.getString("TABLE_NAME"));
            ResultSet cols = conn2.getMetaData().getColumns(null, null, wibble__c.getString("TABLE_NAME"), "*");

            while(cols.next()) {
                System.out.println("  COL " + cols.getString("COLUMN_NAME"));

            }

        }



        Statement stmt = conn.createStatement();
        stmt.execute("insert into wibble__c(name, custom_field__c) values ('aaa', 'Zeta')");
        stmt.execute("insert into wibble__c(name, custom_field__c) values ('bbb', 'Alpha')");

        ResultSet rs = conn.prepareStatement("select * from wibble__c order by name").executeQuery();
        Assert.assertTrue(rs.next());
        Assert.assertEquals("aaa", rs.getString("name"));
        Assert.assertEquals("Zeta", rs.getString("custom_field__c"));
        Assert.assertTrue(rs.next());
        Assert.assertEquals("bbb", rs.getString("name"));
        Assert.assertEquals("Alpha", rs.getString("custom_field__c"));
        Assert.assertFalse(rs.next());
    }


    @Test
    public void testDefaultNameColumnExplicity() throws Exception {
        conn.createStatement().execute("drop table wibble__c if exists");

        String sql = "create table wibble__c(" +
                "name String(10), " +
                "custom_field__c string(10) )";

        conn.createStatement().execute(sql);

        Statement stmt = conn.createStatement();
        stmt.execute("insert into wibble__c(name, custom_field__c) values ('aaa', 'Zeta')");
        stmt.execute("insert into wibble__c(name, custom_field__c) values ('bbb', 'Alpha')");

        ResultSet rs = conn.prepareStatement("select name, custom_field__c from wibble__c order by name").executeQuery();
        Assert.assertTrue(rs.next());
        Assert.assertEquals("aaa", rs.getString("name"));
        Assert.assertEquals("Zeta", rs.getString("custom_field__c"));
        Assert.assertTrue(rs.next());
        Assert.assertEquals("bbb", rs.getString("name"));
        Assert.assertEquals("Alpha", rs.getString("custom_field__c"));
        Assert.assertFalse(rs.next());
    }


        @Test
    public void testCreateReference() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.addBatch("drop table four__c if exists");
        stmt.addBatch("drop table three__c if exists");
        stmt.addBatch("drop table one__c if exists");
        stmt.addBatch("drop table two__c if exists");
        stmt.executeBatch();

        stmt.addBatch("create table two__c(Spang__c int with (label 'Spang No'), Name__c string(20))");
        stmt.addBatch("create table one__c(two_ref__c reference references(two__c) " +
                "with (relationshipName 'RefLookup', relationshipLabel 'RefLookupLbl')," +
                " Name__c string(20), ext__c string(15) with (externalId true))");
        stmt.executeBatch();

        stmt.execute("insert into two__c(Spang__c, Name__c) values (3, 'Norman')");
        ResultSet keyRs = stmt.getGeneratedKeys();
        keyRs.next();
        String twoId = keyRs.getString(1);

        stmt.execute("insert into one__c(two_ref__c, name__c, ext__c) values ('" + twoId + "', 'Bates', 'x')");

        ResultSet rs = stmt.executeQuery("select two_ref__r.Name__c from one__c");
        Assert.assertTrue(rs.next());
        Assert.assertEquals("Norman", rs.getString(1));

        // Insert a record that just has an id and system values
        stmt.execute("insert into one__c() values ()");
        rs = stmt.executeQuery("select count() from one__c");
        rs.next();
        assertEquals(2, rs.getInt(1));

    }

}
