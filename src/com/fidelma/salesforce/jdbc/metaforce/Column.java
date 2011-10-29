package com.fidelma.salesforce.jdbc.metaforce;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Column {

    private Table table;
    private String name;
    private String type;
    private String referencedTable;
    private String referencedColumn;

    private int length;
    private boolean nillable = true;
    private String comments;
    private boolean calculated;
    private boolean autoIncrement;
    private boolean caseSensitive;
    private int precision;
    private int scale;
    private String label;
    private String relationshipType;
    private String aDefault;
    private boolean hasMultipleRelationships;
    public Map<String, String> extraProperties = new HashMap<String, String>();
    private List<String> picklistValues = new ArrayList<String>();
    private String defaultPicklistValue;
    private boolean picklistIsSorted;
    private boolean updateable = true;


    public Column(String name, String type, Boolean isCalculated) {
        this(name, type);
        setCalculated(isCalculated);
    }
    public Column(String name, String type) {
        this.name = name;
        setType(type);
    }

    public Column(String name) {
        this.name = name;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getReferencedTable() {
        return referencedTable;
    }

    public String getReferencedColumn() {
        return referencedColumn;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public boolean isNillable() {
        return nillable;
    }

    public void setNillable(boolean nillable) {
        this.nillable = nillable;
    }

    public void setReferencedTable(String referencedTable) {
        this.referencedTable = referencedTable;
    }

    public void setReferencedColumn(String referencedColumn) {
        this.referencedColumn = referencedColumn;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isCalculated() {
        return calculated;
    }

    public void setCalculated(boolean calculated) {
        this.calculated = calculated;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getPrecision() {
        return precision;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getScale() {
        return scale;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setDefault(String aDefault) {
        this.aDefault = aDefault;
    }

    public String getDefault() {
        return aDefault;
    }

    public void setHasMultipleRelationships(boolean hasMultipleRelationships) {
        this.hasMultipleRelationships = hasMultipleRelationships;
    }

    public boolean hasMultipleRelationships() {
        return hasMultipleRelationships;
    }

    public void setType(String type) {
        if (type.startsWith("_")) {
            type = type.substring(1);
        }
        this.type = type;
    }

    public Boolean isCustom() {
        return name.toUpperCase().endsWith("__C");
    }

    public void addExtraProperty(String name, String value) {
        extraProperties.put(name, value);
    }

    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }

    public void addPicklistValue(String value) {
        picklistValues.add(value);
    }

    public List<String> getPicklistValues() {
        return picklistValues;
    }

    public void setDefaultPicklistValue(String defaultPicklistValue) {
        this.defaultPicklistValue = defaultPicklistValue;
    }

    public String getDefaultPicklistValue() {
        return defaultPicklistValue;
    }

    public void pickListIsSorted(boolean picklistIsSorted) {
        this.picklistIsSorted = picklistIsSorted;
    }

    public boolean isPicklistIsSorted() {
        return picklistIsSorted;
    }

    public void setUpdateable(boolean updateable) {
        this.updateable = updateable;
    }

    public boolean isUpdateable() {
        return updateable;
    }
}
