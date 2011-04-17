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


    public void assertEquals(String expected, String value) throws SQLException {
        if (!value.equalsIgnoreCase(expected)) {
            throw new SQLException("Expected " + expected + " got " + value);
        }
    }

    public void read(String expected) throws SQLException {
        LexicalToken token = al.getToken();
        if (token == null) {
            throw new SQLException("SOQL Command ended unexpected");
        }
        assertEquals(expected, token.getValue());
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

    public List<ParsedSelect> extractColumnsFromSoql() throws Exception {
        List<ParsedSelect> selects = new ArrayList<ParsedSelect>();
        ParsedSelect parsedSelect = new ParsedSelect();
        extractColumnsFromSoql(parsedSelect, selects);
        return selects;
    }

    private void extractColumnsFromSoql(ParsedSelect parsedSelect, List<ParsedSelect> selects) throws Exception {
        List<ParsedColumn> result = new ArrayList<ParsedColumn>();
        SimpleParser la = this;
        la.getToken("SELECT");
        parsedSelect.addToSql("SELECT");

        LexicalToken token = la.getToken();
        int expr = 0;
        boolean prevTokenComma = false;

        while ((token != null) && (!token.getValue().equalsIgnoreCase("FROM"))) {
            if (token.getValue().equals("(")) {

                if (!prevTokenComma) {
                    // TODO: addToSql?
                    token = swallowUntilMatchingBracket(parsedSelect, la);
                } else {
                    parsedSelect.addToSql("(");
                    extractColumnsFromSoql(parsedSelect, selects);
                    token = swallowUntilMatchingBracket(parsedSelect, la);
                }

                if ((token != null) && (!token.getValue().equalsIgnoreCase("FROM") && (!(token.getValue().equals(","))))) {
                    ParsedColumn pc = new ParsedColumn(token.getValue());
                    pc.setAlias(true);
                    pc.setFunction(true);
                    pc.setFunctionName(result.get(result.size() - 1).getName());

                    result.set(result.size() - 1, pc);

                    parsedSelect.addToSql(pc.getName());

                    token = la.getToken();

                } else if (result.size() > 0) {
//                    if (token != null) {
//                        parsedSelect.addToSql(token.getValue());
//                    }
                    ParsedColumn prevPc = result.get(result.size() - 1);
                    prevPc.setFunction(true);
                    prevPc.setFunctionName(prevPc.getName());
                    prevPc.setName("EXPR" + (expr++));
                }
                prevTokenComma = false;
            } else if (token.getValue().equals(")")) {
                parsedSelect.addToSql(token.getValue());
                token = la.getToken();
                break;

            } else if (token.getValue().equals(",")) {
                parsedSelect.addToSql(token.getValue());
                token = la.getToken();
                prevTokenComma = true;
            } else {
                System.out.println("\nBlurged with " + token.getValue());
                String columnName = token.getValue().trim();
                String aliasName = null;

                token = la.getToken();
                LexicalToken maybeAs = token;

// TODO: Handle AS properly
                if (maybeAs != null) {
                    if (maybeAs.getValue().equalsIgnoreCase("AS")) {
                        aliasName = la.getValue();
                        token = la.getToken();
                    } else {
                        token = maybeAs;
                    }

                }
                // Ending with dot indicates a table alias was used. We don't need that
                if ((columnName.length() > 0) && (!columnName.endsWith("."))) {
                    ParsedColumn pc = new ParsedColumn(columnName);
                    pc.setAliasName(aliasName);
                    result.add(pc);

                    parsedSelect.addToSql(columnName);
                }

                prevTokenComma = false;
            }
        }
//        parsedSelect.addToSql("<COLUMNS>");

        String table = handleTableAlias(parsedSelect, result, token);

//        List<ParsedColumn> columns = parsedSelect.getColumns();
//        boolean first = true;
//        String
//        for (ParsedColumn column : columns) {
//            if (!first) {
//                parsedSelect.addToSql(", ");
//            }
//            parsedSelect.addToSql(column.getName());
//            System.out.println("COL! = " + column.getName());
//            first = false;
//        }
        parsedSelect.addToSql(al.unparsedString());
        parsedSelect.setDrivingTable(table);
        parsedSelect.setColumns(result);

        selects.add(parsedSelect);
    }

    private String handleTableAlias(ParsedSelect parsedSelect, List<ParsedColumn> result, LexicalToken token) throws Exception {
        SimpleParser la = this;
        String table;

        List<ParsedColumn> adjustedColumnList = new ArrayList<ParsedColumn>();

        if ((token != null) && (token.getValue().equalsIgnoreCase("from"))) {
            parsedSelect.addToSql(token.getValue());

            table = la.getValue();
            parsedSelect.addToSql(table);

            System.out.println("TABLE=" + table);

            // TODO: Handle quotes    ?
            if (table != null) {
                String alias = la.getValue();  // might be WHERE
                System.out.println("ALIAS=" + alias);

                if (alias != null) {
                    String prefix = alias.toUpperCase() + ".";
                    for (ParsedColumn column : result) {
                        if (column.getName().toUpperCase().equals(prefix)) {

                        } else {
                            if (column.getName().toUpperCase().startsWith(prefix)) {
                                String columnName = column.getName().substring(prefix.length()).trim();
                                System.out.println("1 adding " + columnName);
                                if (columnName.length() != 0) {
                                    column.setName(columnName);
                                    adjustedColumnList.add(column);
                                }

                            } else {
                                System.out.println("2 adding " + column.getName());
                                adjustedColumnList.add(column);
                                // TODO: What is this for?
//                                if (!column.equals("")) {
//                                    fresh.add(column);
//                                }
                            }
                        }
                    }
                    result.clear();
                    result.addAll(adjustedColumnList);
                    parsedSelect.addToSql(alias);
//                    parsedSelect.setTableAlias(alias);
                }
            }
        } else {
            throw new SQLException("Missing FROM clause. Found " + token);
        }
        return table;
    }

    private LexicalToken swallowUntilMatchingBracket(ParsedSelect parsedSelect, SimpleParser la) throws Exception {
        parsedSelect.addToSql("(");
        LexicalToken token = la.getToken();

        while ((token != null) && (!token.getValue().equals(")"))) {
            parsedSelect.addToSql(token.getValue());

            if (token.getValue().equals("(")) {
                token = swallowUntilMatchingBracket(parsedSelect, la);
            } else {
                token = la.getToken();
            }
        }
        if (token != null) {
            parsedSelect.addToSql(token.getValue());
            token = la.getToken();
        }
        return token;
    }


}
