package com.fidelma.salesforce.jdbc.dml;

import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.misc.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 12/06/11
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class GrantTests {

    private static SfConnection conn = null;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        conn = TestHelper.getTestConnection();
    }

    @Test
    public void testGrantStatement() throws Exception {

        // TODO: Only allow "grant" -- other permissions are inferred?

//        conn.createStatement().execute("revoke object read, delete on abc__c from 'Standard'");
//        conn.createStatement().execute("grant object create, update, read on abc__c to *");
//        conn.createStatement().execute("grant object create, update, read on abc__c to 'Standard'");
    }

}
