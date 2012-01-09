package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.misc.Reconnector;
import com.fidelma.salesforce.parse.SimpleParser;

/**
 * ALTER TABLE <tableName> ADD  (
 * <columnName> <dataType>
 *
 * ALTER TABLE <tableName> DROP COLUMN <columnname>
 *
 */
public class AlterTable {
    private SimpleParser al;
    private ResultSetFactory metaDataFactory;
    private Reconnector reconnector;

    public AlterTable(SimpleParser al, ResultSetFactory metaDataFactory, Reconnector reconnector) {
        this.al = al;
        this.metaDataFactory = metaDataFactory;
        this.reconnector = reconnector;
    }


    public void execute() throws Exception {
        String tableName = al.getToken().getValue();

        String addOrDrop = al.getValue();
        if (addOrDrop.equalsIgnoreCase("ADD")) {

            CreateTable createTable = new CreateTable(al, metaDataFactory, reconnector);
            createTable.executeAlter(tableName);

        } else if (addOrDrop.equalsIgnoreCase("DROP")) {

            al.read("COLUMN");
            DropColumn dropColumn = new DropColumn(al, metaDataFactory, reconnector);
            dropColumn.execute(tableName);
        }
    }
}
