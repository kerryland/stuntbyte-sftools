import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.misc.TypeHelper;
import com.sforce.soap.partner.*;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.sobject.SObject;
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
        assertEquals(1, rs.getInt("count"));
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

        String soql = "select  Preferred_Contact_Medium__r.Email_Address__c,  Person_Name__c, organisation__r.name, LastModifiedDate, Registered_User_Last_Update_By_Website__c  , Localist_Role__c, Role_Type__c\n" +
                "from person_role__c \n" +
                " where recordType.DeveloperName = 'Registered_User' \n" +
                " and Organisation__r.Business_Type__c = 'Agency'\n" +
                " and Registered_User_Last_Update_By_Website__c = 0\n" +
                "order by LastModifiedDate desc";

        soql = "insert into leads_to_convert__c(lead__c) values ('00QQ0000005CJ7GMAW')";

        Statement stmt = conn.createStatement();
        System.out.println(stmt.executeUpdate(soql));
//        ResultSet rs = stmt.executeQuery(soql);
//        while (rs.next()) {
//            System.out.println("1>" + rs.getString("Preferred_Contact_Medium__r.Email_Address__c"));
//            System.out.println("2>" + rs.getString(1));
//        }
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
//        assertEquals(0, rsm.getPrecision(6));  // TODO: Broken?
//        assertEquals(0, rsm.getScale(6));      // TODO: Broken?

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
//        ", , , , email__c, \n" +
//        "number4dp__c, percent0dp__c, phone__c, picklist__c, multipicklist__c, \n" +
//        "textarea__c, textarearich__c, url__c " +


        stmt.execute("delete from aaa__c where name = '" + name + "'");
    }

    @Test
    public void testSelectStar() throws Exception {
        Statement stmt = conn.createStatement();
        int count = stmt.executeUpdate("insert into aaa__c(name, number4dp__c) values ('selectStar', 1)");
        ResultSet rs = stmt.executeQuery("select * from aaa__c where name='selectStar'");
        assertEquals(27, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals("selectStar", rs.getString("name"));
        assertEquals("1.0", rs.getBigDecimal("number4dp__c").toPlainString());

        // Try alias.
        rs = stmt.executeQuery("select a.* from aaa__c a where a.name='selectStar'");
        assertEquals(27, rs.getMetaData().getColumnCount());
        assertTrue(rs.next());
        assertEquals("selectStar", rs.getString("name"));
        assertEquals("1.0", rs.getBigDecimal("number4dp__c").toPlainString());

        // Try quoted table.
        // TODO: This is not implemented yet:
//        rs = stmt.executeQuery("select a.* from \"aaa__c\" a where a.name='selectStar'");
//        assertEquals(27, rs.getMetaData().getColumnCount());
//        assertTrue(rs.next());
//        assertEquals("selectStar", rs.getString("name"));
//        assertEquals("1.0", rs.getBigDecimal("number4dp__c").toPlainString());

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
