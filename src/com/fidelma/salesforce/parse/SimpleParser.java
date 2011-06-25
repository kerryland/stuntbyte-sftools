package com.fidelma.salesforce.parse;

import com.fidelma.salesforce.jdbc.sqlforce.LexicalAnalyzer;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class SimpleParser {

    private LexicalAnalyzer al;
    private String commandString;

    public SimpleParser(String commandString) {
        this.commandString = commandString;

        al = new LexicalAnalyzer(
                new ByteArrayInputStream(
                        commandString.getBytes()), null);
    }

    public String getCommandString() {
        return commandString;
    }

    public void assertEquals(String expected, String value) throws SQLException {
        if (!value.equalsIgnoreCase(expected)) {
            throw new SQLException("Expected '" + expected + "' got '" + value + "' in :" +  commandString);
        }
    }

    public void read(String expected) throws SQLException {
        LexicalToken token = al.getToken();
        if (token == null) {
            throw new SQLException("SOQL Command ended unexpected: " + commandString);
        }
        assertEquals(expected, token.getValue());
    }

    public String readIf() throws SQLException {
        LexicalToken token = al.getToken();
        if (token == null) {
            throw new SQLException("SOQL Command ended unexpected: " + commandString);
        }
        return token.getValue();
    }


    public LexicalToken getToken() throws Exception {
        return al.getToken();
    }

    public LexicalToken getToken(String expected) throws Exception {
        return al.getToken(expected);
    }

    public String getValue(String errorIfNull) throws Exception {
        String value = getValue();
        if (value == null) {
            throw new Exception(errorIfNull);
        }
        return value;
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

        while ((token != null) && (!token.getValue().equalsIgnoreCase("FROM"))) {
            if (token.getValue().equals("(")) {
                parsedSelect.addToSql("(");
                token = swallowUntilMatchingBracket(parsedSelect, la);

                if ((token != null) && (!token.getValue().equalsIgnoreCase("FROM") && (!(token.getValue().equals(","))))) {
                    String aliasName;
                    if (token.getValue().equalsIgnoreCase("AS")) {
                        aliasName = la.getValue();
                    } else {
                        aliasName = token.getValue();
                    }

                    ParsedColumn pc = new ParsedColumn(aliasName);
                    pc.setAlias(true);
                    pc.setAliasName(aliasName);
                    pc.setFunction(true);
                    pc.setFunctionName(result.get(result.size() - 1).getName());

                    result.set(result.size() - 1, pc);

                    parsedSelect.addToSql(pc.getName());

                    token = la.getToken();

                } else if (result.size() > 0) {
                    ParsedColumn prevPc = result.get(result.size() - 1);
                    prevPc.setFunction(true);
                    prevPc.setFunctionName(prevPc.getName());
                    prevPc.setName("EXPR" + (expr++));
                }

            } else if (token.getValue().equals(")")) {
                parsedSelect.addToSql(token.getValue());
                token = la.getToken();
                break;

            } else if (token.getValue().equals(",")) {
                parsedSelect.addToSql(token.getValue());
                token = la.getToken();
            } else {
                String columnName = token.getValue().trim();
                String aliasName = null;

                token = la.getToken();

                LexicalToken maybeAs = token;

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

            }
        }

        String table = handleTableAlias(parsedSelect, result, token);

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

            // TODO: Handle quotes    ?
            if (table != null) {
                String alias = la.getValue();  // might be WHERE

                if (alias != null) {
                    String prefix = alias.toUpperCase() + ".";
                    for (ParsedColumn column : result) {
                        if (column.getName().toUpperCase().equals(prefix)) {

                        } else {
                            if (column.getName().toUpperCase().startsWith(prefix)) {
                                String columnName = column.getName().substring(prefix.length()).trim();
                                if (columnName.length() != 0) {
                                    column.setName(columnName);
                                    adjustedColumnList.add(column);
                                }

                            } else {
                                adjustedColumnList.add(column);
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
        LexicalToken token = la.getToken();

        if ((token.getValue().equalsIgnoreCase("SELECT"))) {
            throw new SQLFeatureNotSupportedException("We don't support subselects");
        }


        while ((token != null) && (!token.getValue().equals(")"))) {
            if (parsedSelect != null) {
                parsedSelect.addToSql(token.getValue());
            }

            if (token.getValue().equals("(")) {
                token = swallowUntilMatchingBracket(parsedSelect, la);
            } else {
                token = la.getToken();
            }
        }
        if (token != null) {
            if (parsedSelect != null) {
                parsedSelect.addToSql(token.getValue());
            }
            token = la.getToken();
        }
        return token;
    }


}
