import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.misc.TestHelper;
import com.fidelma.salesforce.misc.TypeHelper;
import com.sforce.soap.partner.*;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.sobject.SObject;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.prefs.BackingStoreException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 */

public class SelectEngineTests {

    private static SfConnection conn = null;

    private static String surname;
    private static List<String> deleteMe = new ArrayList<String>();

    private static SObject aaa;
    private static SObject bbb;
    private static SObject ccc;
    private static SObject ddd;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        Class.forName("com.fidelma.salesforce.jdbc.SfDriver");

        Properties info = new Properties();
        info.put("user", TestHelper.username);
        info.put("password", TestHelper.password);
        info.put("standard", "true");
        info.put("includes", "Lead,Account");
        info.put("useLabels", "true");

        // Get a connection to the database
        conn = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:" + TestHelper.loginUrl
                , info);

        SfConnection sfConnection = (SfConnection) conn;
        PartnerConnection pc = sfConnection.getHelper().getPartnerConnection();

        surname = "Smith" + System.currentTimeMillis();

        SObject lead = new SObject();
        lead.setType("Lead");
        lead.addField("Company", "MikeCo");
        lead.addField("FirstName", "Mike");
        lead.addField("LastName", surname);
        String id = checkSaveResult(pc.create(new SObject[]{lead}));

    }


    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        SfConnection sfConnection = (SfConnection) conn;
        PartnerConnection pc = sfConnection.getHelper().getPartnerConnection();

        String[] delete = new String[deleteMe.size()];
        deleteMe.toArray(delete);
        DeleteResult[] drs = pc.delete(delete);
//        String msg = "";
//        for (DeleteResult dr : drs) {
//            if (!dr.isSuccess()) {
//                msg += dr.getErrors()[0].getMessage();
//            }
//        }
//        if (!msg.equals("")) {
//            throw new Exception(msg);
//        }
    }


    @Test
    public void testSelectStatement() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "select FirstName, LastName, CreatedDate, CreatedBy.name" +
                        " from Lead where lastName = '" + surname + "'");
        ResultSetMetaData rsmd = rs.getMetaData();

        assertEquals(DatabaseMetaData.columnNullable, rsmd.isNullable(1));
        assertEquals(DatabaseMetaData.columnNoNulls, rsmd.isNullable(2));

        assertEquals(-1, stmt.getUpdateCount());

        int foundCount = 0;
        while (rs.next()) {
            foundCount++;
            assertEquals("Mike", rs.getString("FirstName"));
            assertEquals("Mike", rs.getString("firstname"));
            assertEquals("Mike", rs.getString("FIRSTNAME"));
            assertEquals(surname, rs.getString("LastName"));
            assertEquals("Kerry Sainsbury", rs.getString("CreatedBy.Name"));

            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

            Timestamp created = rs.getTimestamp("CreatedDate");

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(created.getTime());
            assertEquals(now.get(Calendar.YEAR), cal.get(Calendar.YEAR));
            assertEquals(now.get(Calendar.MONTH), cal.get(Calendar.MONTH));
            assertEquals(now.get(Calendar.DAY_OF_YEAR), cal.get(Calendar.DAY_OF_YEAR));

            int firstNameColumn = rs.findColumn("FirstName");
            assertEquals("Mike", rs.getString(firstNameColumn));
        }

        assertEquals(1, foundCount);
    }

    @Test
    public void testSelectAsStatement() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "select FirstName as fn, LastName as ln, CreatedDate, CreatedBy.name as cn" +
                        " from Lead where lastName = '" + surname + "'");
        ResultSetMetaData rsmd = rs.getMetaData();

        // TODO: What about select FirstName as LastName, LastName as FirstName"
        assertEquals(DatabaseMetaData.columnNullable, rsmd.isNullable(1));
        assertEquals(DatabaseMetaData.columnNoNulls, rsmd.isNullable(2));

        assertEquals(-1, stmt.getUpdateCount());

        int foundCount = 0;
        while (rs.next()) {
            foundCount++;
            assertEquals("Mike", rs.getString("fn"));
            assertEquals("Mike", rs.getString("FN"));
            assertEquals(surname, rs.getString("ln"));
            assertEquals("Kerry Sainsbury", rs.getString("cn"));
        }

        assertEquals(1, foundCount);
    }


    @Test
    public void testNoRows() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select FirstName, LastName, CreatedBy.name " +
                "from Lead where lastName = '" + System.currentTimeMillis() + "'");
        assertTrue(rs.getMetaData() != null);
        assertEquals(0, rs.getMetaData().getColumnCount());
        assertFalse(rs.next());
    }

    // Open Office likes saying WHERE 0 = 1, which SF doesn't understand. Let's patch it.
    @Test
    public void testNoRowsZeroEqualsOne() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select FirstName, LastName, CreatedBy.name " +
                "from Lead where 0 = 1");
        assertTrue(rs.getMetaData() != null);
        assertEquals(0, rs.getMetaData().getColumnCount());
        assertFalse(rs.next());
    }

    @Test
    public void testComments() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("----\nselect lastName from Lead where lastName = '" + surname + "' group by lastName");
        assertEquals(1, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals(surname, rs.getString(1));
    }

    @Test
    public void testCount() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select count() from Lead where lastName = '" + surname + "'");
        assertEquals(1, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals("expr0", rs.getMetaData().getColumnName(1));
        assertEquals("expr0", rs.getMetaData().getColumnLabel(1));

        assertEquals(1, rs.getInt(1));
        assertEquals(1, rs.getInt("expr0"));

//        rs = stmt.executeQuery("select count() from AAA__c");
//        assertEquals(1, rs.getMetaData().getColumnCount());
//        assertTrue(rs.next());
//        assertEquals("count", rs.getMetaData().getColumnName(1));
//        assertEquals("count", rs.getMetaData().getColumnLabel(1));
//        assertEquals(0, rs.getInt(1));

    }


    @Test
    public void testCountNoRows() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select count() from aaa__c where name = 'does not exist'");
        assertEquals(1, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));
    }


    @Test
    public void testCountStar() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select count(*) from Lead where lastName = '" + surname + "'");
        assertEquals(1, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
    }

    @Test
    public void testTableAliases() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select l.lastName from Lead l where l.lastName = '" + surname + "'");
        assertEquals(1, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals(surname, rs.getString(1));
        assertEquals(surname, rs.getString("lastName"));
    }


    @Test
    public void testSimpleGroupBy() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select lastName from Lead where lastName = '" + surname + "' group by lastName");
        assertEquals(1, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals(surname, rs.getString(1));
    }

    @Test
    public void testSimpleAggregate() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select company, max(lastName), min(lastName) small, Max(firstName) as maxname from Lead where lastName = '" + surname + "' group by company");
        assertEquals(4, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals("MikeCo", rs.getString(1));
        assertEquals(surname, rs.getString(2));
        assertEquals(surname, rs.getString(3));
        assertEquals("Mike", rs.getString(4));

        assertEquals("MikeCo", rs.getString("Company"));
        assertEquals(surname, rs.getString("expr0"));
        assertEquals(surname, rs.getString("small"));
        assertEquals("Mike", rs.getString("maxname"));
    }


    @Test
    public void testSubquery() throws Exception {
        try {
            String sql = "SELECT Account.Name, x.Type," +
                    " (SELECT b.LastName FROM Account.Contacts b order by Contact.FirstName), BillingCity \n" +
                    "  FROM Account x limit 10";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
//            assertEquals(3, rs.getMetaData().getColumnCount());
//            assertEquals("Account.Name", rs.getMetaData().getColumnName(1));
//            assertEquals("Type", rs.getMetaData().getColumnName(2));
//            assertEquals("BillingCity", rs.getMetaData().getColumnName(3));
            fail("Should have thrown an exception");

        } catch (SQLFeatureNotSupportedException e) {
            // Good!
        }
    }

    /*

http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_soql_select.htm

    //


     */


    @Test
    public void testPreparedQueryString() throws Exception {
        String soql = "select count() from Lead where lastName = ?";

        PreparedStatement stmt = conn.prepareStatement(soql);
        stmt.setString(1, surname);
        ResultSet rs = stmt.executeQuery();
        assertEquals(1, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
    }

    @Test
    public void testMaxRowsAndFetchSize() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("delete from aaa__c");


        PartnerConnection pc = conn.getHelper().getPartnerConnection();

        // Add a 300 rows to 'AAA__C' table
        SObject[] heaps = new SObject[300];
        NumberFormat mf = new DecimalFormat("0000");
        for (int i = 0; i < heaps.length; i++) {
            SObject aaa2 = new SObject();
            aaa2.setType("aaa__c");
            aaa2.addField("Name", "aaa " + mf.format(i + 1));
            heaps[i] = aaa2;
        }

        // Have to insert the rows in chunks
        pc.create(Arrays.copyOfRange(heaps, 0, 199));
        pc.create(Arrays.copyOfRange(heaps, 199, heaps.length));

        String soql = "select count() from aaa__c";
        ResultSet rs = stmt.executeQuery(soql);
        rs.next();
        assertEquals(heaps.length, rs.getInt(1));

        stmt.setMaxRows(1);
        rs = stmt.executeQuery("select name from aaa__c");
        assertTrue(rs.next());    // first row should be found
        assertFalse(rs.next());   // second row should NOT be found

        stmt.setMaxRows(0);
        rs = stmt.executeQuery("select name from aaa__c order by name asc");

        int count = 0;
        for (SObject heap : heaps) {
            count++;
            assertTrue("Did not find row " + count, rs.next());    // row should be found
        }
        assertFalse(rs.next());


        // Test fetch size
        stmt.setFetchSize(1);
        rs = stmt.executeQuery("select name from aaa__c");
        count = 0;
        for (SObject heap : heaps) {
            count++;
            assertTrue("Did not find row " + count, rs.next());    // row should be found
        }
        assertFalse(rs.next());

        // Seeing as we have all this data, lets check we can do a large update
        count = stmt.executeUpdate("update aaa__c set currency__c = 4");
        assertEquals("Did not report update count correctly", 300, count);

        // Check the rows actually got updated
        rs = stmt.executeQuery("select currency__c from aaa__c");
        count = 0;
        while (rs.next()) {
            count++;
            assertEquals("4.0", rs.getBigDecimal(1).toPlainString());
        }
        assertEquals(300, count);
    }


    @Test
    public void testDatabaseMetaData() throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getPrimaryKeys(null, null, "aaa__c");
        assertTrue(rs.next());
        assertEquals("Id", rs.getString("COLUMN_NAME"));

        rs = meta.getTables(null, null, "aaa__c", null);
        assertTrue(rs.next());
        assertEquals("aaa__c", rs.getString("TABLE_NAME"));
        assertFalse(rs.next());

        rs = meta.getTables(null, null, "aaa%", null);
        assertTrue(rs.next());
        assertEquals("aaa__c", rs.getString("TABLE_NAME"));
        assertFalse(rs.next());

        rs = meta.getTables(null, null, "%aa__c", null);
        assertTrue(rs.next());
        assertEquals("aaa__c", rs.getString("TABLE_NAME"));
        assertFalse(rs.next());

        rs = meta.getColumns(null, null, "Account", "Type");
        assertTrue(rs.next());
        assertEquals("Type", rs.getString("COLUMN_NAME"));

        rs = meta.getColumns(null, null, "User", "AccountId");
        assertTrue(rs.next());
        assertEquals(DatabaseMetaData.columnNullable, rs.getInt("NULLABLE"));
        assertEquals("YES", rs.getString("IS_NULLABLE"));
        rs = meta.getColumns(null, null, "User", "Id");
        assertTrue(rs.next());
        assertEquals(DatabaseMetaData.columnNoNulls, rs.getInt("NULLABLE"));
        assertEquals("NO", rs.getString("IS_NULLABLE"));
    }

/*
    @Test
    public void testRegression() throws Exception {

        Properties info = new Properties();
        info.put("user", "kerry.sainsbury@nzpost.co.nz.sandbox");
        info.put("password", "xJiKif3IeCLiZKNervuO3W3ozLxyQ6cm");
//    info.put("standard", "true");
//    info.put("includes", "Lead,Account");

//        info.put("user", "fronde.admin@localist.co.nz");
//        info.put("password", "jrP2U0TnW09DesQIaxOmAb3yWiN9lRLu");


        // Get a connection to the database
        Connection conn = DriverManager.getConnection(
                "jdbc:sfdc:https://test.salesforce.com"
//                "jdbc:sfdc:https://login.salesforce.com"
                , info);

        String soql = "select Localist_Order__r.Account__r.Customer_State__c, " +
                "Localist_Order__r.State__c, count(Id)\n" +
                "from localist_product__c\n" +
                "where Product_Offering__r.ProductCode = 'D-LP'\n" +
                "and state__c = 'Pending'\n" +
                "group by Localist_Order__r.Account__r.Customer_State__c, Localist_Order__r.State__c";

        Statement stmt = conn.createStatement();


//        System.out.println(stmt.executeUpdate(soql));

        ResultSet rs = stmt.executeQuery(soql);
        assertEquals(3, rs.getMetaData().getColumnCount());

        System.out.println(rs.getMetaData().getColumnName(1) + " " + rs.getMetaData().getColumnLabel(1));
        System.out.println(rs.getMetaData().getColumnName(2) + " " + rs.getMetaData().getColumnLabel(2));
        System.out.println(rs.getMetaData().getColumnName(3) + " " + rs.getMetaData().getColumnLabel(3));

//        System.out.println("LAB IS " + lab);
//        System.out.println("COL IS " + col);
        while (rs.next()) {
//            System.out.println("1>" + rs.getString("Main_Category__c"));
//            System.out.println("1>" + rs.getString("Main_Category__r.Name"));
//            System.out.println("1>" + rs.getInt("expr0"));
            System.out.println("2>" + rs.getString(1));
            System.out.println("2>" + rs.getString(2));
            System.out.println("2>" + rs.getInt(3));
        }

    }
  */

    // Given aaa.bbb__r.ccc__r.ddd__r.name

    @Test
    public void testRelationship() throws Exception {

        SfConnection sfConnection = conn;
        PartnerConnection pc = sfConnection.getHelper().getPartnerConnection();

        String id;
        ddd = new SObject();
        ddd.setType("ddd__c");
        ddd.addField("Name", "ddd Name");
        id = checkSaveResult(pc.create(new SObject[]{ddd}));
        ddd.setId(id);

        ccc = new SObject();
        ccc.setType("ccc__c");
        ccc.addField("Name", "ccc Name");
        ccc.addField("ddd__c", id);
        id = checkSaveResult(pc.create(new SObject[]{ccc}));
        ccc.setId(id);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2010, Calendar.FEBRUARY, 11, 23, 59, 59);

        bbb = new SObject();
        bbb.setType("bbb__c");
        bbb.addField("Name", "bbb Name");
        bbb.addField("SomeDate__c", new java.util.Date(cal.getTimeInMillis())); // date__c
        bbb.addField("ccc__c", id);
        id = checkSaveResult(pc.create(new SObject[]{bbb}));
        bbb.setId(id);

        aaa = new SObject();
        aaa.setType("aaa__c");
        aaa.addField("Name", "aaa Name");
        aaa.addField("bbb__c", id);
        id = checkSaveResult(pc.create(new SObject[]{aaa}));
        aaa.setId(id);

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "select name, " +
                        "bbb__r.name, " +
                        "bbb__r.ccc__r.Name, " +
                        "bbb__r.ccc__r.ddd__r.Name " +
                        " from aaa__c " +
                        "where id = '" + aaa.getId() + "'");

        ResultSetMetaData md = rs.getMetaData();
        assertEquals(4, md.getColumnCount());

        assertEquals("Name", md.getColumnName(1));
        assertEquals("bbb__r.Name", md.getColumnName(2));
        assertEquals("bbb__r.ccc__r.Name", md.getColumnName(3));
        assertEquals("bbb__r.ccc__r.ddd__r.Name", md.getColumnName(4));

        assertEquals("aaa Name", md.getColumnLabel(1));
        assertEquals("bbb Name", md.getColumnLabel(2));
        assertEquals("ccc Name", md.getColumnLabel(3));
        assertEquals("ddd Name", md.getColumnLabel(4));


        int foundCount = 0;
        while (rs.next()) {
            foundCount++;
            assertEquals("aaa Name", rs.getString("Name"));
            assertEquals("bbb Name", rs.getString("bbb__r.Name"));
            assertEquals("ccc Name", rs.getString("bbb__r.ccc__r.Name"));
            assertEquals("ddd Name", rs.getString("bbb__r.ccc__r.ddd__r.Name"));
        }
        assertEquals(1, foundCount);


        String soql = "select \n" +
                "bbb__r.Name,\n " +
                "bbb__c,\n" +
                "count(id)\n" +
                " from aaa__c where bbb__c = '" + bbb.getId() + "' " +
                "group by bbb__c,\n" +
                "bbb__r.Name " +
                "order by bbb__r.name  \n";

        rs = stmt.executeQuery(soql);
        assertEquals(3, rs.getMetaData().getColumnCount());

        assertEquals("bbb__r.Name", rs.getMetaData().getColumnName(1));
        assertEquals("bbb__c", rs.getMetaData().getColumnName(2));
        assertEquals("expr0", rs.getMetaData().getColumnName(3));

        assertTrue(rs.next());

        assertEquals("bbb Name", rs.getString("bbb__r.Name"));
        assertEquals(bbb.getId(), rs.getString("bbb__c"));
        assertEquals(1, rs.getInt("expr0"));


        // Try a group by date
        rs = stmt.executeQuery(
                "select phone__c, " +
                        "bbb__r.SomeDate__c, count(*) " +
                        " from aaa__c " +
                        "where bbb__r.SomeDate__c != null " +
                        "group by phone__c, bbb__r.SomeDate__c");

        md = rs.getMetaData();
        assertEquals(3, md.getColumnCount());
        assertEquals("phone", rs.getMetaData().getColumnTypeName(1));
        assertEquals("date", rs.getMetaData().getColumnTypeName(2));
        assertEquals("string", rs.getMetaData().getColumnTypeName(3)); // TODO: Should be integer!

        assertTrue(rs.next());

        assertEquals("2010-02-11", rs.getDate(2).toString());

        // Remove BBB, CCC and DDD -- we should still get 4 columns returned!
        pc.delete(new String[]{ddd.getId(), ccc.getId(), bbb.getId()});
        rs = stmt.executeQuery(
                "select name, " +
                        "bbb__r.name, " +
                        "bbb__r.ccc__r.name, " +
                        "bbb__r.ccc__r.ddd__r.name " +
                        " from aaa__c " +
                        "where id = '" + aaa.getId() + "'");

        assertEquals(4, rs.getMetaData().getColumnCount());

        assertEquals("Name", rs.getMetaData().getColumnName(1));
        assertEquals("bbb__r.name", rs.getMetaData().getColumnName(2));
        assertEquals("bbb__r.ccc__r.name", rs.getMetaData().getColumnName(3));
        assertEquals("bbb__r.ccc__r.ddd__r.name", rs.getMetaData().getColumnName(4));

        foundCount = 0;
        while (rs.next()) {
            foundCount++;
            assertEquals("aaa Name", rs.getString("Name"));
            assertEquals(null, rs.getString("bbb__r.Name"));
            assertEquals(null, rs.getString("bbb__r.ccc__r.Name"));
            assertEquals(null, rs.getString("bbb__r.ccc__r.ddd__r.Name"));
        }
        assertEquals(1, foundCount);

        // And again, with columns rearranged
        rs = stmt.executeQuery(
                "select " +
                        "bbb__r.ccc__r.ddd__r.name," +
                        "bbb__r.ccc__r.name, " +
                        "bbb__r.name, " +
                        "name " +
                        " from aaa__c " +
                        "where id = '" + aaa.getId() + "'");

        assertEquals(4, rs.getMetaData().getColumnCount());

        assertEquals("Name", rs.getMetaData().getColumnName(4));
        assertEquals("bbb__r.name", rs.getMetaData().getColumnName(3));
        assertEquals("bbb__r.ccc__r.name", rs.getMetaData().getColumnName(2));
        assertEquals("bbb__r.ccc__r.ddd__r.name", rs.getMetaData().getColumnName(1));

        foundCount = 0;
        while (rs.next()) {
            foundCount++;
            assertEquals("aaa Name", rs.getString("Name"));
            assertEquals(null, rs.getString("bbb__r.Name"));
            assertEquals(null, rs.getString("bbb__r.ccc__r.Name"));
            assertEquals(null, rs.getString("bbb__r.ccc__r.ddd__r.Name"));
        }
        assertEquals(1, foundCount);


        // Meta data...
        rs = conn.getMetaData().getExportedKeys(null, null, "bbb__c");
        assertTrue(rs.next());

        assertEquals("bbb__c", rs.getString("PKTABLE_NAME"));
        assertEquals("Id", rs.getString("PKCOLUMN_NAME"));
        assertEquals("aaa__c", rs.getString("FKTABLE_NAME"));
        assertEquals("bbb__c", rs.getString("FKCOLUMN_NAME"));
    }


    @Test
    public void testNonRelationship() throws Exception {

        SfConnection sfConnection = conn;
        PartnerConnection pc = sfConnection.getHelper().getPartnerConnection();

        // Create aaa__c with no reference to bbb__c, but write a query that looks at bbb__r.name
        aaa = new SObject();
        aaa.setType("aaa__c");
        aaa.addField("Name", "aaa Name");
        String id = checkSaveResult(pc.create(new SObject[]{aaa}));
        aaa.setId(id);


        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "select name, " +
                        "bbb__r.name, " +
                        "bbb__r.ccc__r.Name, " +
                        "bbb__r.ccc__r.ddd__r.Name " +
                        " from aaa__c " +
                        "where id = '" + aaa.getId() + "'");

        ResultSetMetaData md = rs.getMetaData();
        int foundCount = 0;
        while (rs.next()) {
            foundCount++;
            assertEquals("aaa Name", rs.getString("Name"));
            assertEquals(null, rs.getString("bbb__r.Name"));
            assertEquals(null, rs.getString("bbb__r.ccc__r.Name"));
            assertEquals(null, rs.getString("bbb__r.ccc__r.ddd__r.Name"));
        }
        assertEquals(1, foundCount);
    }


    @Test
    public void testUpdate() throws Exception {
        Statement stmt = conn.createStatement();
        int count = stmt.executeUpdate("update Lead\n" +
                " set FirstName = 'wibbleX', phone='0800xxxx'," +
                " AnnualRevenue=475000, " +
                " NumberOfEmployees=6 where LastName = '" + surname + "'");
        assertEquals(1, count);
        assertEquals(1, stmt.getUpdateCount());

        ResultSet rs = stmt.executeQuery("select FirstName, Phone, Lastname, AnnualRevenue, NumberOfEmployees" +
                " from Lead where lastName = '" + surname + "'");

        int foundCount = 0;
        while (rs.next()) {
            foundCount++;
            assertEquals("wibbleX", rs.getString("FirstName"));
            assertEquals(surname, rs.getString("LastName"));
            assertEquals("0800xxxx", rs.getString("Phone"));
            assertEquals(475000f, rs.getDouble("AnnualRevenue"), 0.5f);
            assertEquals(6, rs.getInt("NumberOfEmployees"));
        }
        assertEquals(1, foundCount);
    }


    @Test
    public void testPreparedUpdate() throws Exception {

        String soql = "update Lead\n" +
                " set FirstName = ?, phone=?," +
                " AnnualRevenue=?, " +
                " NumberOfEmployees=? where LastName =?";

        PreparedStatement stmt = conn.prepareStatement(soql);
        stmt.setString(1, "Wayne");
        stmt.setString(2, "0800-WAYNE");
        stmt.setFloat(3, 76000f);
        stmt.setInt(4, 15);
        stmt.setString(5, surname);


        int count = stmt.executeUpdate();
        assertEquals(1, count);

        ResultSet rs = stmt.executeQuery("select FirstName, Phone, Lastname, AnnualRevenue, NumberOfEmployees" +
                " from Lead where lastName = '" + surname + "'");

        int foundCount = 0;
        while (rs.next()) {
            foundCount++;
            assertEquals("Wayne", rs.getString("FirstName"));
            assertEquals(surname, rs.getString("LastName"));
            assertEquals("0800-WAYNE", rs.getString("Phone"));
            assertEquals(15, rs.getInt("NumberOfEmployees"));
            assertEquals(76000f, rs.getDouble("AnnualRevenue"), 0.5);
        }
        assertEquals(1, foundCount);
    }

    @Test
    public void testInsert() throws Exception {
        Statement stmt = conn.createStatement();
        String soql = "insert\n into Lead(Company, FirstName, LastName, Phone, AnnualRevenue, NumberOfEmployees)" +
                "values ('CoCo', 'wibbleXYZ'," + "'" + surname + "', '0800xxxx',475001, 7)";

        int count = stmt.executeUpdate(soql);
        assertEquals(1, count);
        ResultSet rs = stmt.getGeneratedKeys();
        assertTrue(rs.next());
        String id = rs.getString("Id");

        rs = stmt.executeQuery("select Id, Company, FirstName, Phone, Lastname, AnnualRevenue, NumberOfEmployees" +
                " from Lead where lastName = '" + surname + "' and Firstname = 'wibbleXYZ'");

        int foundCount = 0;
        while (rs.next()) {
            foundCount++;
            assertEquals(id, rs.getString("Id"));
            assertEquals("CoCo", rs.getString("Company"));
            assertEquals("wibbleXYZ", rs.getString("FirstName"));
            assertEquals(surname, rs.getString("LastName"));
            assertEquals("0800xxxx", rs.getString("Phone"));
            assertEquals("475001.0", rs.getString("AnnualRevenue"));
            assertEquals("7", rs.getString("NumberOfEmployees"));
        }
        assertEquals(1, foundCount);
    }

    @Test
    public void testDatatypes() throws Exception {

        PartnerConnection pc = conn.getHelper().getPartnerConnection();

        bbb = new SObject();
        bbb.setType("bbb__c");
        bbb.addField("Name", "bbb Name");
        String bid = checkSaveResult(pc.create(new SObject[]{bbb}));
//             bbb.setId(id);

        String name = "Willy" + System.currentTimeMillis();

        String soql = "insert into aaa__c(\n" +
                "Name, bbb__c, long_text_1__c, long_text_2__c, \n" +
                "checkbox__c, currency__c, date__c, datetime__c, email__c, \n" +
                "number4dp__c, percent0dp__c, phone__c, picklist__c, multipicklist__c, \n" +
                "textarea__c, textarearich__c, url__c)\n" +
                "values (?,?,?,?,\n" +
                "        ?,?,?,?,?,\n" +
                "        ?,?,?,?,?,\n" +
                "        ?,?,?)";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 31000; i++) {
            sb.append("x");
        }
        // http://www.salesforce.com/us/developer/docs/api/Content/field_types.htm
        PreparedStatement pstmt = conn.prepareStatement(soql);
        int col = 0;
        pstmt.setString(++col, name); // Name
        pstmt.setString(++col, bid);      // reference to bbb__c
        pstmt.setString(++col, sb.toString());
        pstmt.setString(++col, sb.toString().replaceAll("x", "y"));
        pstmt.setBoolean(++col, true);                       // checkbox__c
        pstmt.setBigDecimal(++col, new BigDecimal("12.25")); // currency__c
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2010, Calendar.FEBRUARY, 11, 23, 59, 59);
        pstmt.setDate(++col, new java.sql.Date(cal.getTimeInMillis())); // date__c
        cal.set(2010, Calendar.OCTOBER, 21, 23, 15, 0);
        pstmt.setTimestamp(++col, new Timestamp(cal.getTimeInMillis())); // datetime__c
        pstmt.setString(++col, "noddy@example.com"); // email__c
        pstmt.setBigDecimal(++col, new BigDecimal("17.12345")); // number4dp__c
        pstmt.setBigDecimal(++col, new BigDecimal("96.7777")); // percent0dp__c
        pstmt.setString(++col, "0800-PHONE"); // phone__c
        pstmt.setString(++col, "PickMe"); // picklist__c
        pstmt.setString(++col, "Red;Blue;Green"); // multipicklist__c
        pstmt.setString(++col, "Text Area");
        pstmt.setString(++col, "Text Area Rich");
        pstmt.setString(++col, "http://www.example.com"); // url__c

        int count = pstmt.executeUpdate();

        assertEquals(1, count);

        Statement stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery("select " +
                "Name, bbb__c, long_text_1__c, long_text_2__c, \n" +
                "checkbox__c, currency__c, date__c, datetime__c, email__c, \n" +
                "number4dp__c, percent0dp__c, phone__c, picklist__c, multipicklist__c, \n" +
                "textarea__c, textarearich__c, url__c " +
                " from aaa__c where Name = '" + name + "'");

        int foundCount = 0;
        while (rs.next()) {
            foundCount++;
            assertEquals(name, rs.getString("name"));
            assertEquals(bid, rs.getString("bbb__c"));
            assertEquals(sb.toString(), rs.getString("long_text_1__c"));
            assertEquals(Boolean.TRUE, rs.getBoolean("checkbox__c"));
            assertEquals("12.25", rs.getBigDecimal("currency__c").toPlainString());

            SimpleDateFormat sdf = new SimpleDateFormat(TypeHelper.dateFormat);
            Date d = new Date(rs.getDate("date__c").getTime());
            assertEquals("2010-02-11", sdf.format(d));

            Timestamp ts = rs.getTimestamp("datetime__c");
            d = new Date(ts.getTime());
            sdf = new SimpleDateFormat(TypeHelper.timestampFormat);
            assertEquals("2010-10-21T23:15:00.000Z", sdf.format(d));

            String pick = rs.getString("picklist__c");
            assertEquals("PickMe", pick);

//            Array ar = rs.getArray("picklist__c");
//            assertEquals("string", ar.getBaseTypeName());
//            assertEquals(Types.VARCHAR, ar.getBaseType());
        }
        assertEquals(1, foundCount);

        // Check Name metadata
        ResultSetMetaData rsm = rs.getMetaData();
        assertEquals("aaa Name", rsm.getColumnLabel(1));
        assertEquals("Name", rsm.getColumnName(1));
        assertEquals("", rsm.getCatalogName(1));
        assertEquals("java.lang.String", rsm.getColumnClassName(1));
        assertEquals(80, rsm.getColumnDisplaySize(1));
        assertEquals("string", rsm.getColumnTypeName(1));
        assertEquals(Types.VARCHAR, rsm.getColumnType(1));

        assertEquals("bbb", rsm.getColumnLabel(2));
        assertEquals("bbb__c", rsm.getColumnName(2));
        assertEquals("", rsm.getCatalogName(2));
        assertEquals("java.lang.String", rsm.getColumnClassName(2));
        assertEquals(18, rsm.getColumnDisplaySize(2));
        assertEquals("reference", rsm.getColumnTypeName(2));
        assertEquals(Types.VARCHAR, rsm.getColumnType(2));


        // long_text_1__c
        assertEquals("long text 1", rsm.getColumnLabel(3));
        assertEquals("long_text_1__c", rsm.getColumnName(3));
        assertEquals("", rsm.getCatalogName(3));
        assertEquals("java.lang.String", rsm.getColumnClassName(3));
        assertEquals(32000, rsm.getColumnDisplaySize(3));
        assertEquals("textarea", rsm.getColumnTypeName(3));
        assertEquals(Types.LONGVARCHAR, rsm.getColumnType(3));

        assertEquals("checkbox", rsm.getColumnLabel(5));
        assertEquals("checkbox__c", rsm.getColumnName(5));
        assertEquals("", rsm.getCatalogName(5));
        assertEquals("java.lang.Boolean", rsm.getColumnClassName(5));
        assertEquals(5, rsm.getColumnDisplaySize(5));
        assertEquals("_boolean", rsm.getColumnTypeName(5));
        assertEquals(Types.BOOLEAN, rsm.getColumnType(5));

        assertEquals("currency2dp", rsm.getColumnLabel(6));
        assertEquals("currency__c", rsm.getColumnName(6));
        assertEquals("", rsm.getCatalogName(6));
        assertEquals("java.lang.Double", rsm.getColumnClassName(6));
        assertEquals(14, rsm.getColumnDisplaySize(6));
        assertEquals("currency", rsm.getColumnTypeName(6));
        assertEquals(Types.DOUBLE, rsm.getColumnType(6));
        assertEquals(14, rsm.getPrecision(6));
        assertEquals(2, rsm.getScale(6));

        assertEquals("date", rsm.getColumnLabel(7));
        assertEquals("date__c", rsm.getColumnName(7));
        assertEquals("", rsm.getCatalogName(7));
        assertEquals("java.sql.Date", rsm.getColumnClassName(7));
        assertEquals(10, rsm.getColumnDisplaySize(7));
        assertEquals("date", rsm.getColumnTypeName(7));
        assertEquals(Types.DATE, rsm.getColumnType(7));

        assertEquals("datetime", rsm.getColumnLabel(8));
        assertEquals("datetime__c", rsm.getColumnName(8));
        assertEquals("", rsm.getCatalogName(8));
        assertEquals("java.sql.Timestamp", rsm.getColumnClassName(8));
        assertEquals(15, rsm.getColumnDisplaySize(8));
        assertEquals("datetime", rsm.getColumnTypeName(8));
        assertEquals(Types.TIMESTAMP, rsm.getColumnType(8));

        //TODO:
//       email__c, \n" +            // 9
//        "number4dp__c,            // 10
//        percent0dp__c,            // 11
//        phone__c,                 // 12
//       picklist__c,               // 13

//        assertEquals("picklist", rsm.getColumnLabel(13));
//        assertEquals("picklist__c", rsm.getColumnName(13));
//        assertEquals("", rsm.getCatalogName(13));
//        assertEquals("java.sql.Array", rsm.getColumnClassName(13));
//        assertEquals(255, rsm.getColumnDisplaySize(13));
//        assertEquals("picklist", rsm.getColumnTypeName(13));
//        assertEquals(Types.ARRAY, rsm.getColumnType(13));


        assertEquals("picklist", rsm.getColumnLabel(13));
        assertEquals("picklist__c", rsm.getColumnName(13));
        assertEquals("", rsm.getCatalogName(13));
        assertEquals("java.lang.String", rsm.getColumnClassName(13));
        assertEquals(255, rsm.getColumnDisplaySize(13));
        assertEquals("picklist", rsm.getColumnTypeName(13));
        assertEquals(Types.VARCHAR, rsm.getColumnType(13));

        assertEquals("multipicklist", rsm.getColumnLabel(14));
        assertEquals("multipicklist__c", rsm.getColumnName(14));
        assertEquals("", rsm.getCatalogName(14));
        assertEquals("java.lang.String", rsm.getColumnClassName(14));
        assertEquals(4099, rsm.getColumnDisplaySize(14));    // TODO-4099, really?
        assertEquals("multipicklist", rsm.getColumnTypeName(14));
        assertEquals(Types.VARCHAR, rsm.getColumnType(14));

//        "textarea__c,             // 15
//      textarearich__c,            // 16
//      url__c " +                  // 17


        stmt.execute("delete from aaa__c where name = '" + name + "'");
    }

    @Test
    public void testSelectStar() throws Exception {
        int colCount = 29;

        Statement stmt = conn.createStatement();
        int count = stmt.executeUpdate("insert into aaa__c(name, number4dp__c) values ('selectStar', 1)");
        assertEquals(1, count);
        ResultSet rs = stmt.executeQuery("select * from aaa__c where name='selectStar'");
        assertEquals(colCount, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals("selectStar", rs.getString("name"));
        assertEquals("1.0", rs.getBigDecimal("number4dp__c").toPlainString());

        // Try alias.
        rs = stmt.executeQuery("select a.* from aaa__c a where a.name='selectStar'");
        assertEquals(colCount, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals("selectStar", rs.getString("name"));
        assertEquals("1.0", rs.getBigDecimal("number4dp__c").toPlainString());

        // Try quoted table.
        rs = stmt.executeQuery("select a.* from \"aaa__c\" a where a.name='selectStar'");
        assertEquals(colCount, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals("selectStar", rs.getString("name"));
        assertEquals("1.0", rs.getBigDecimal("number4dp__c").toPlainString());
    }

    @Test
    public void testWhoWhat() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("insert into Contact(LastName) values ('Bob')", Statement.RETURN_GENERATED_KEYS);
        ResultSet rs = stmt.getGeneratedKeys();
        rs.next();
        String id = rs.getString("Id");
        deleteMe.add(id);

        stmt.executeUpdate("insert into Task(Subject, WhoId) values ('Glooper', '" + id + "')");
        rs = stmt.getGeneratedKeys();
        rs.next();
        deleteMe.add(rs.getString("Id"));

        rs = stmt.executeQuery("SELECT Id, Subject, Who.Id, Who.LastName, Who.Type FROM Task where Who.Id = '" + id + "' limit 1");
        assertEquals(5, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals("Glooper", rs.getString("Subject"));
        assertEquals(id, rs.getString("Who.Id"));
        assertEquals("Contact", rs.getString("Who.Type"));
        assertEquals("Bob", rs.getString("Who.LastName"));
    }


    // "Type" is a bit of a weird column name, because there's an implicit column called
    // "Type" in all results that we want to ignore.
    // This makes sure that we can select the "Type" column from Account.
    @Test
    public void testSelectType() throws Exception {
        Statement stmt = conn.createStatement();
        String name = "A" + System.currentTimeMillis();
        int count = stmt.executeUpdate("insert into Account(name, Type) values ('" + name + "', 'Blue')");
        assertEquals(1, count);

        ResultSet rs = stmt.executeQuery("select Id, Name, Type from Account where name='" + name + "'");
        assertTrue(rs.next());
        deleteMe.add(rs.getString(1));

        assertEquals(3, rs.getMetaData().getColumnCount());
        assertEquals(name, rs.getString("name"));
        assertEquals("Blue", rs.getString("Type"));

        rs = stmt.executeQuery("select * from Account where name='" + name + "'");
        assertTrue(rs.next());
        assertEquals(name, rs.getString("name"));
        assertEquals("Blue", rs.getString("Type"));

        rs = stmt.executeQuery("select Name, \"Type\" from Account where name='" + name + "'");
        assertTrue(rs.next());
        assertEquals(name, rs.getString("name"));
        assertEquals("Blue", rs.getString("Type"));
    }


    @Test
    public void testDelete() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("delete from aaa__c");

        // Insert 3 rows
        int count = stmt.executeUpdate("insert into aaa__c(name, number4dp__c) values ('testDelete', 1)");
        assertEquals(1, count);
        count = stmt.executeUpdate("insert into aaa__c(name, number4dp__c) values ('testDelete', 2)");
        assertEquals(1, count);
        count = stmt.executeUpdate("insert into aaa__c(name, number4dp__c) values ('testDelete', 3)");
        assertEquals(1, count);

        // Delete one row
        count = stmt.executeUpdate("delete from aaa__c where number4dp__c = 1");
        assertEquals(1, count);

        ResultSet rs = stmt.executeQuery("select count() from aaa__c where name = 'testDelete'");
        rs.next();
        assertEquals(2, rs.getInt(1));

        // Delete all rows
        count = stmt.executeUpdate("delete from aaa__c");
        assertEquals(2, count);

        rs = stmt.executeQuery("select id from aaa__c limit 1");
        assertFalse(rs.next());

    }


    private static String checkSaveResult(SaveResult[] sr) throws Exception {
        String id = null;

        for (SaveResult saveResult : sr) {
            if (!saveResult.isSuccess()) {
                Error[] errors = saveResult.getErrors();
                String msg = "";
                for (Error error : errors) {
                    msg += error.getMessage() + " ";
                }
                throw new Exception(msg);
            } else {
                id = saveResult.getId();

                deleteMe.add(id);

            }
        }
        return id;

    }


}
