package com.fidelma.salesforce.misc;

import com.fidelma.salesforce.jdbc.sqlforce.LexicalAnalyzer;
import com.fidelma.salesforce.jdbc.sqlforce.LexicalToken;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;

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
}
