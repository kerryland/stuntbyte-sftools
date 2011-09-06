package com.fidelma.database.migration;

import com.fidelma.salesforce.jdbc.SfConnection;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 4/09/11
 * Time: 8:53 AM
 * To change this template use File | Settings | File Templates.
 */
public class Hack {
    public static void main(String[] args) throws Exception {

        Class.forName("com.fidelma.salesforce.jdbc.SfDriver");

        Properties info = new Properties();

//        String destUser = "fronde.admin@localist.co.nz.devkerry";
//        String destPwd = "jrP2U0TnCWok3CTtOfnhPC6UjYrOgQzI";


        info = new Properties();
//        info.put("user", destUser);
//        info.put("password", destPwd);

        info.put("user", "kerry.sainsbury@nzpost.co.nz.sandbox");
        info.put("password", "xJiKif3IeCLiZKNervuO3W3ozLxyQ6cm");

        // Get a connection to the database
        SfConnection destSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:https://test.salesforce.com"
                , info);

    }
}
