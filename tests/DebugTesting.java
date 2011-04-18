import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Hacky test to find/debug "production" problems
 */
public class DebugTesting {

    @Test
    public void testRegression() throws Exception {

        Class.forName("com.fidelma.salesforce.jdbc.SfDriver");


        Properties info = new Properties();
        info.put("user", "kerry.sainsbury@nzpost.co.nz.sandbox");
        info.put("password", "u9SABqa2dQ8srnC7xytkAKhiKNe8vpazDIy");
//    info.put("standard", "true");
//    info.put("includes", "Lead,Account");

        // Get a connection to the database
        Connection conn = DriverManager.getConnection(
                "jdbc:sfdc:https://test.salesforce.com"
                , info);
//
//        String soql = "insert into Leads_To_Convert__c(lead__c) values ('00QQ0000005BtC1')";
//
//        Statement stmt = conn.createStatement();
//        Boolean ok = stmt.execute(soql);
//
//        PreparedStatement p = conn.prepareStatement(soql);
//        p.executeUpdate()
//        assertTrue(ok);



//        String soql = "select  Preferred_Contact_Medium__r.Email_Address__c,  Person_Name__c, organisation__r.name, LastModifiedDate, Registered_User_Last_Update_By_Website__c  , Localist_Role__c, Role_Type__c\n" +
//                "from person_role__c \n" +
//                " where recordType.DeveloperName = 'Registered_User' \n" +
//                " and Organisation__r.Business_Type__c = 'Agency'\n" +
//                " and Registered_User_Last_Update_By_Website__c = 0\n" +
//                "order by LastModifiedDate desc";
//
//        Statement stmt = conn.createStatement();
//        ResultSet rs = stmt.executeQuery(soql);
//        while (rs.next()) {
//            System.out.println("1>" + rs.getString("Preferred_Contact_Medium__r.Email_Address__c"));
//            System.out.println("2>" + rs.getString(1));
//        }
    }

    
}
