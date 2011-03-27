import com.fidelma.salesforce.jdbc.SfConnection;
import com.sforce.soap.partner.*;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.sobject.SObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import static org.junit.Assert.*;

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
        info.put("user", "salesforce@fidelma.com");
        info.put("password", "u9SABqa2dQxG0Y3kqWiJQVEwnYtryr1Ja1");
        info.put("standard", "true");
        info.put("includes", "Lead,Account");

        // Get a connection to the database
        conn = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:https://login.salesforce.com"
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
        ResultSet rs = stmt.executeQuery("select FirstName, LastName, CreatedDate, CreatedBy.name from Lead where lastName = '" + surname + "'");

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
    public void testNoRows() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select FirstName, LastName, CreatedBy.name " +
                "from Lead where lastName = '" + System.currentTimeMillis() + "'");
        assertTrue(rs.getMetaData() != null);
        assertEquals(0, rs.getMetaData().getColumnCount());
        assertFalse(rs.next());
    }


    @Test
    public void testCount() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select count() from Lead where lastName = '" + surname + "'");
        assertEquals(1, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
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
        ResultSet rs = stmt.executeQuery("select company, max(lastName), min(lastName) small from Lead where lastName = '" + surname + "' group by company");
        assertEquals(3, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals("MikeCo", rs.getString(1));
        assertEquals(surname, rs.getString(2));
        assertEquals(surname, rs.getString(3));

        assertEquals("MikeCo", rs.getString("Company"));
        assertEquals(surname, rs.getString("expr0"));
        assertEquals(surname, rs.getString("small"));
    }

    /*
    @Test
    public void testSubquery() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT Account.Name, " +
                "(SELECT Contact.LastName FROM Account.Contacts) lname FROM Account limit 1");
        assertEquals(2, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());

        assertEquals("MikeCo", rs.getString("Account.Name"));
        assertEquals(surname, rs.getString("expr0"));

    }
    */
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
    }

    @Test
    public void testResultSetMetaData() throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getPrimaryKeys(null, null, "Lead");
        while (rs.next()) {
            System.out.println(rs.getString("COLUMN_NAME"));
            System.out.println(rs.getString("PK_NAME"));
        }

    }


    /*
@Test
    public void testRegression() throws Exception {

    Properties info = new Properties();
    info.put("user", "kerry.sainsbury@nzpost.co.nz.sandbox");
    info.put("password", "u9SABqa2dQ8srnC7xytkAKhiKNe8vpazDIy");
//    info.put("standard", "true");
//    info.put("includes", "Lead,Account");

    // Get a connection to the database
    Connection conn = DriverManager.getConnection(
            "jdbc:sfdc:https://test.salesforce.com"
            , info);


       Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select Localist_Product__r.name,\n" +
                "Localist_Product__r.Localist_Service__r.name,\n" +
                "Localist_Product__r.Localist_Service__r.class__c,\n" +
                "Localist_Product__r.Localist_Product_Specification__r.Name\n" +
                "from\n" +
                "Localist_Product_Category_Member__c\n" +
                "where Localist_Product__r.Localist_Service__r.class__c = 'Online Listing'\n" +
                "and Localist_Product__r.Localist_Product_Specification__r.Name != 'Listing Page'\n" +
                "and Presence_Category__c in\n" +
                "(select Presence_Category__c from \n" +
                "Presence_Category_Group_Member__c\n" +
                "where Presence_Category_Group__r.Category_Group_Tree_ID__c = 'ONL-STD-STANDARDONLINECATEGORIES')");
        assertTrue(rs.next());
    }
    */

    // Given aaa.bbb__r.ccc__r.ddd__r.name

    @Test
    public void testRelationship() throws Exception {

        SfConnection sfConnection = (SfConnection) conn;
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

        bbb = new SObject();
        bbb.setType("bbb__c");
        bbb.addField("Name", "bbb Name");
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
        int colCount = md.getColumnCount();
        Set<String> colNames = new HashSet<String>();
        System.out.println("COL COUNT=" + colCount);
        for (int i = 1; i <= colCount; i++) {
            System.out.println("MD " + i + "=" + md.getColumnName(i));
            colNames.add(md.getColumnName(i));
        }

//        assertTrue(colNames.contains("Parent.Name"));
//        assertTrue(colNames.contains("Parent.CreatedBy.LastName"));
        int foundCount = 0;
        while (rs.next()) {
            foundCount++;
            assertEquals("aaa Name", rs.getString("Name"));
            assertEquals("bbb Name", rs.getString("bbb__r.Name"));
            assertEquals("ccc Name", rs.getString("bbb__r.ccc__r.Name"));
            assertEquals("ddd Name", rs.getString("bbb__r.ccc__r.ddd__r.Name"));
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

        System.out.println(soql);
        int count = stmt.executeUpdate(soql);
        assertEquals(1, count);

        ResultSet rs = stmt.executeQuery("select Company, FirstName, Phone, Lastname, AnnualRevenue, NumberOfEmployees" +
                " from Lead where lastName = '" + surname + "' and Firstname = 'wibbleXYZ'");

        int foundCount = 0;
        while (rs.next()) {
            foundCount++;
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


    /*
    public void testMe() throws Exception {

//        rs = stmt.executeQuery("select Name, Description, CreatedBy.name from account limit 3");
        rs = stmt.executeQuery("select Name, Description, CreatedBy.name, id,  createdby.id, createdby.lastname from account limit 3");
//        rs = stmt.executeQuery("SELECT Title, Id,\n" +
//                "       magic__c,\n" +
//                "       Associated_Lead__r.id\n" +
//                "FROM Lead\n" +
//                "where lastname = 'Sainsbury'\n" +
//                "order by magic__c limit 1\n" +
//                "");

        ResultSetMetaData rsmd = rs.getMetaData();
        System.out.println("COL COUNT=" + rsmd.getColumnCount());
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            System.out.println(i + " " + rsmd.getColumnLabel(i));
        }


        while (rs.next()) {
            System.out.print("GOTx ");
            for (int i = 1; i < rsmd.getColumnCount(); i++) {
                System.out.println(rsmd.getColumnLabel(i) + "=" + rs.getString(i) + " ");
            }
            System.out.println("<");
        }

//        ResultSetMetaData rsMeta = rs.getMetaData();
//
//        System.out.println("COUNT=" + rsMeta.getColumnCount());
//        for (int i = 1; i < rsMeta.getColumnCount(); i++) {
//            System.out.println("BLOOP : "+ i + " " + rsMeta.getColumnName(i));
//        }

//        assertEquals(3, rsMeta.getColumnCount());
//        assertEquals("Name", rsMeta.getColumnName(1));
//        assertEquals("Description", rsMeta.getColumnName(2));
//        assertEquals("CreatedBy.Name", rsMeta.getColumnName(3));


        // Crude prepared statement
//        PreparedStatement pstnt = conn.prepareStatement("select name from account limit 2");
//        rs = pstnt.executeQuery();
//        while (rs.next()) {
//            System.out.println("GOTx " + rs.getString("Name"));
//        }

//        rs = pstnt.executeQuery("select name, description from account limit 3");
//        while (rs.next()) {
//            System.out.println("GOTy " + rs.getString("Name") + " " + rs.getString("Description"));
//        }
//
//        // Meta data
//        rs = conn.getMetaData().getTables(null, null, null, null);
//        while (rs.next()) {
//            System.out.println("TAB: " + rs.getString("TABLE_NAME"));
//        }


    }
    */


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
