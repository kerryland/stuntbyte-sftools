package com.fidelma.salesforce.jdbc.metaforce;


public class Column {

    private Table table;
    private String name;
    private String type;
    private String referencedTable;
    private String referencedColumn;

    private Integer length;
    private boolean nillable;
    private String comments;
    private boolean calculated;
    private boolean autoIncrement;
    private boolean caseSensitive;
    private Integer precision;
    private Integer scale;
    private String label;
    private String relationshipType;
    private String aDefault;
    private boolean hasMultipleRelationships;

    public Column(String name, String type) {
        this.name = name;
        this.type = type;
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

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
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

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public Integer getScale() {
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
        this.type = type;
    }
}
