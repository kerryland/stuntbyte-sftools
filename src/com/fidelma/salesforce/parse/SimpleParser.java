package com.fidelma.salesforce.parse;

import com.fidelma.salesforce.jdbc.sqlforce.LexicalAnalyzer;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 26/03/11
 * Time: 7:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleParser {

    private String commandString;
    private LexicalAnalyzer al;

    public SimpleParser(String commandString) {
        this.commandString = commandString;
        al = new LexicalAnalyzer(
                new ByteArrayInputStream(
                        commandString.getBytes()), null);
    }


    public void read(String expected) throws SQLException {
        LexicalToken token = al.getToken();
        if (token == null) {
            throw new SQLException("SOQL Command ended unexpected");
        }
        if (!token.getValue().equalsIgnoreCase(expected)) {
            throw new SQLException("Expected " + expected + " got " + token);
        }
    }

//    private LexicalToken readIf(LexicalToken.Type type) throws SQLException {
//        LexicalToken token = al.getToken();
//        if (token == null) {
//            throw new SQLException("SOQL Command ended unexpected");
//        }
//        if (!token.getType().equals(type)) {
//            throw new SQLException("Expected " + type.name());
//        }
//        return token;
//    }


    public LexicalToken getToken() throws Exception {
        return al.getToken();
    }

    public LexicalToken getToken(String expected) throws Exception {
        return al.getToken(expected);
    }

    public String getValue() throws Exception {
        LexicalToken token = getToken();
        if (token == null) {
            return null;
        }
        return token.getValue();
    }

    public ParseSelect extractColumnsFromSoql() throws Exception {
        List<ParseColumn> result = new ArrayList<ParseColumn>();

        SimpleParser la = this;
        la.getToken("SELECT");
        LexicalToken token = la.getToken();
        int expr = 0;

        while ((token != null) && (!token.getValue().equalsIgnoreCase("FROM"))) {
            if (token.getValue().equals("(")) {

                token = swallowUntilMatchingBracket(la);

//                System.out.println("Swallowed bracket until " + token.getValue());
                if ((token != null) && (!token.getValue().equalsIgnoreCase("FROM") && (!(token.getValue().equals(","))))) {
                    ParseColumn pc = new ParseColumn(token.getValue().toUpperCase());
                    pc.setAlias(true);
                    pc.setFunction(true);
                    pc.setFunctionName(result.get(result.size() - 1).getName());

//                   System.out.println("Splatting " + result.get(result.size()-1).getName() + " with " + pc.getName());
                    result.set(result.size() - 1, pc);
                    token = la.getToken();
                } else if (result.size() > 0) {
//                    System.out.println("Functionising " + result.get(result.size()-1).getName());
                    ParseColumn prevPc = result.get(result.size() - 1);
                    prevPc.setFunction(true);
                    prevPc.setFunctionName(prevPc.getName());
                    prevPc.setName("EXPR" + (expr++));
                }
            } else if (token.getValue().equals(",")) {
                token = la.getToken();
            } else {
                String x = token.getValue().trim();
                if (x.length() > 0) {
                    ParseColumn pc = new ParseColumn(x.toUpperCase());
//                    System.out.println("Boring store of " + x);
                    result.add(pc);
                }
                token = la.getToken();
            }

        }

        String table = handleColumnAlias(result, token);

        ParseSelect parseSelect = new ParseSelect();
        parseSelect.setDrivingTable(table);
        parseSelect.setColumns(result);

        return parseSelect;
    }

    private String handleColumnAlias(List<ParseColumn> result, LexicalToken token) throws Exception {
        SimpleParser la = this;
        String table = null;

        List<ParseColumn> fresh = new ArrayList<ParseColumn>();

        if ((token != null) && (token.getValue().equalsIgnoreCase("from"))) {
            table = la.getValue();
            // TODO: Handle quotes    ?
            System.out.println("TABLE=" + table);
            if (table != null) {
                String alias = la.getValue();
                if (alias != null) {
                    String prefix = alias.toUpperCase() + ".";
                    for (ParseColumn column : result) {
                        if (column.getName().equals(prefix)) {

                        } else {
                            if (column.getName().startsWith(prefix)) {
                                String x = column.getName().substring(prefix.length()).trim();
                                if (x.length() != 0) {
                                    column.setName(x);
                                    fresh.add(column);
                                }

                            } else {
                                fresh.add(column);
                                // TODO: What is this for?
//                            if (!column.equals("")) {
//                                freshResult.add(column);
//                            }
                            }
                        }
                    }
                    result.clear();
                    result.addAll(fresh);
                }
            }
        } else {
            throw new SQLException("Missing FROM clause");
        }
        return table;
    }

    private LexicalToken swallowUntilMatchingBracket(SimpleParser la) throws Exception {
        LexicalToken token = la.getToken();

        while ((token != null) && (!token.getValue().equals(")"))) {
            if (token.getValue().equals("(")) {
                token = swallowUntilMatchingBracket(la);
            } else {
                token = la.getToken();
            }
        }
        if (token != null) {
            token = la.getToken();
        }
        return token;
    }


}
