package com.fidelma.salesforce.parse;

import com.fidelma.salesforce.jdbc.sqlforce.LexicalAnalyzer;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class SimpleParser {

    private LexicalAnalyzer al;

    public SimpleParser(String commandString) {
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

    public String readIf() throws SQLException {
        LexicalToken token = al.getToken();
        if (token == null) {
            throw new SQLException("SOQL Command ended unexpected");
        }
        return token.getValue();
    }


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

    public List<ParseSelect> extractColumnsFromSoql() throws Exception {
        List<ParseSelect> selects = new ArrayList<ParseSelect>();
        extractColumnsFromSoql(selects);
        return selects;
    }

    private void extractColumnsFromSoql(List<ParseSelect> selects) throws Exception {
        List<ParseColumn> result = new ArrayList<ParseColumn>();
        SimpleParser la = this;
        la.getToken("SELECT");
        LexicalToken token = la.getToken();
        int expr = 0;
        boolean prevTokenComma = false;

        while ((token != null) && (!token.getValue().equalsIgnoreCase("FROM"))) {
            if (token.getValue().equals("(")) {
                if (!prevTokenComma) {
                    token = swallowUntilMatchingBracket(la);
                } else {
                    extractColumnsFromSoql(selects);
                    token = swallowUntilMatchingBracket(la);
                }

                if ((token != null) && (!token.getValue().equalsIgnoreCase("FROM") && (!(token.getValue().equals(","))))) {
                    ParseColumn pc = new ParseColumn(token.getValue());
                    pc.setAlias(true);
                    pc.setFunction(true);
                    pc.setFunctionName(result.get(result.size() - 1).getName());

                    result.set(result.size() - 1, pc);
                    token = la.getToken();
                } else if (result.size() > 0) {
                    ParseColumn prevPc = result.get(result.size() - 1);
                    prevPc.setFunction(true);
                    prevPc.setFunctionName(prevPc.getName());
                    prevPc.setName("EXPR" + (expr++));
                }
                prevTokenComma = false;
            } else if (token.getValue().equals(")")) {
                token = la.getToken();
                break;

            } else if (token.getValue().equals(",")) {
                token = la.getToken();
                prevTokenComma = true;
            } else {
                String x = token.getValue().trim();
                if (x.length() > 0) {
                    ParseColumn pc = new ParseColumn(x);
                    result.add(pc);
                }
                token = la.getToken();
                prevTokenComma = false;
            }
        }

        String table = handleColumnAlias(result, token);

        ParseSelect parseSelect = new ParseSelect();
        parseSelect.setDrivingTable(table);
        parseSelect.setColumns(result);

        selects.add(parseSelect);
    }

    private String handleColumnAlias(List<ParseColumn> result, LexicalToken token) throws Exception {
        SimpleParser la = this;
        String table = null;

        List<ParseColumn> fresh = new ArrayList<ParseColumn>();

        if ((token != null) && (token.getValue().equalsIgnoreCase("from"))) {
            table = la.getValue();
            // TODO: Handle quotes    ?
            if (table != null) {
                String alias = la.getValue();
                if (alias != null) {
                    String prefix = alias.toUpperCase() + ".";
                    for (ParseColumn column : result) {
                        if (column.getName().toUpperCase().equals(prefix)) {

                        } else {
                            if (column.getName().toUpperCase().startsWith(prefix)) {
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
            throw new SQLException("Missing FROM clause. Found " + token);
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
