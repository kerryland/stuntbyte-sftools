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
import com.stuntbyte.salesforce.database.migration.Migrator;
import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.jdbc.metaforce.Table;

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

        Class.forName("com.stuntbyte.salesforce.jdbc.SfDriver");

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
        migrator.migrateData(sourceSalesforce, destSalesforce, h2Conn, migrationCriteriaList, new ArrayList<MigrationCriteria>());
    }
}
