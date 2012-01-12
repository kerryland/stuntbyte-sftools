package com.stuntbyte.salesforce.parse;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 */
public class SimpleParserTests {

    @Test
    public void testTableAlias() throws Exception {
        String sql = "select a.Company, a.* from Lead a";
        SimpleParser p = new SimpleParser(sql);
        List<ParsedSelect> pses = p.extractColumnsFromSoql();
        ParsedSelect ps = pses.get(0);
        assertEquals("Lead", ps.getDrivingTable());

        assertEquals(2, ps.getColumns().size());

        assertEquals("Company", ps.getColumns().get(0).getName());
        assertEquals(false, ps.getColumns().get(0).isAlias());
        assertEquals(false, ps.getColumns().get(0).isFunction());

        assertEquals("*", ps.getColumns().get(1).getName());
        assertEquals(false, ps.getColumns().get(1).isAlias());
        assertEquals(false, ps.getColumns().get(1).isFunction());

    }

    @Test
    public void testColumnAliasAndFunctions() throws Exception {

        String sql = "select " +
                "company, " +
                "max(firstName), " +
                "min(lastName) small, " +
                "max(lastName) " +
                "from Lead where lastName = 'Smith' group by company";
        SimpleParser p = new SimpleParser(sql);
        List<ParsedSelect> pses = p.extractColumnsFromSoql();
        ParsedSelect ps = pses.get(0);

        assertEquals("Lead", ps.getDrivingTable());

//        for (ParseColumn col : ps.getColumns()) {
//            System.out.println(col.getName() + " a:" + col.isAlias() + " f:" + col.isFunction());
//        }
//

        assertEquals(4, ps.getColumns().size());
        assertEquals("company", ps.getColumns().get(0).getName());
        assertEquals(false, ps.getColumns().get(0).isAlias());
        assertEquals(false, ps.getColumns().get(0).isFunction());

        assertEquals("EXPR0", ps.getColumns().get(1).getName());
        assertEquals(false, ps.getColumns().get(1).isAlias());
        assertEquals(true, ps.getColumns().get(1).isFunction());
        assertEquals("max", ps.getColumns().get(1).getFunctionName());

        assertEquals("small", ps.getColumns().get(2).getName());
        assertEquals(true, ps.getColumns().get(2).isAlias());
        assertEquals(true, ps.getColumns().get(2).isFunction());
        assertEquals("min", ps.getColumns().get(2).getFunctionName());


        assertEquals("EXPR1", ps.getColumns().get(3).getName());
        assertEquals(false, ps.getColumns().get(3).isAlias());
        assertEquals(true, ps.getColumns().get(3).isFunction());
        assertEquals("max", ps.getColumns().get(3).getFunctionName());



    }
}
