package com.stuntbyte.salesforce.parse;

/**
 * Holds info about columns found while parsing a SELECT
 */
public class ParsedColumn {
    private String name;
    private boolean isAlias;
    private boolean isFunction;
    private String functionName;
    private String table;
    private String aliasName;

    public String getExpressionContents() {
        return expressionContents;
    }

    private String expressionContents;

    public ParsedColumn(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAlias() {
        return isAlias;
    }

    public void setAlias(boolean alias) {
        isAlias = alias;
    }

    public boolean isFunction() {
        return isFunction;
    }

    public void setFunction(boolean function) {
        isFunction = function;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getTable() {
        return table;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setExpressionContents(String expressionContents) {
        this.expressionContents = expressionContents;
    }
}
