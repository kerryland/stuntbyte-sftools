package com.fidelma.salesforce.parse;

/**
 * Holds info about columns found while parsing a SELECT
 */
public class ParseColumn {
    private String name;
    private boolean isAlias;
    private boolean isFunction;
    private String functionName;

    public ParseColumn(String name) {
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
}
