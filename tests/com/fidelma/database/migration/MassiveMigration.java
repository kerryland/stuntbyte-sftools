package com.fidelma.database.migration;

import com.fidelma.salesforce.database.migration.MigrationCriteria;
import com.fidelma.salesforce.database.migration.Migrator;
import com.fidelma.salesforce.jdbc.SfConnection;
import com.fidelma.salesforce.jdbc.metaforce.Table;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Migrate from sandbox to sandbox2
 */
public class MassiveMigration {

    public static void main(String[] args) throws Exception {

        Class.forName("com.fidelma.salesforce.jdbc.SfDriver");

        Properties info = new Properties();
        info.put("user", "kerry.sainsbury@nzpost.co.nz.sandbox");
        info.put("password", "xJiKif3IeCLiZKNervuO3W3ozLxyQ6cm");
        SfConnection sourceSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:https://test.salesforce.com"
                , info);

//        System.exit(0);

        Class.forName("org.h2.Driver");

        info = new Properties();
        // Get a connection to the database
        Connection h2Conn = DriverManager.getConnection(
                "jdbc:h2:/tmp/sfdc-prod;LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0"
                , info);

//        String destUser = "fronde.admin@localist.co.nz.dev1";
//        String destPwd = "jrP2U0TnW09DesQIaxOmAb3yWiN9lRLu";

        String destUser = "fronde.admin@localist.co.nz.sandbox2";
        String destPwd = "jrP2U0TnW09DesQIaxOmAb3yWiN9lRLu";
        info = new Properties();
        info.put("user", destUser);
        info.put("password", destPwd);

        // Get a connection to the database
        SfConnection destSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:https://test.salesforce.com"
                , info);

        // TODO: Sort these into a dependency order
        // TODO: Maybe NOT disable explicitly listed workflow or triggers?

        List<MigrationCriteria> migrationCriteriaList = new ArrayList<MigrationCriteria>();

        for (Table table : sourceSalesforce.getMetaDataFactory().getTables()) {
            if (table.getType().equalsIgnoreCase("table")) { //  && (!table.getName().startsWith("c2g__"))) {
                MigrationCriteria mc = new MigrationCriteria(table.getName(), "");
                migrationCriteriaList.add(mc);
            }
        }

        Migrator migrator = new Migrator();
        migrator.migrateData(sourceSalesforce, destSalesforce, h2Conn, migrationCriteriaList);
    }
}
