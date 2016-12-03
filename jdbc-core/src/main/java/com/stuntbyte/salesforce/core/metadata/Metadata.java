package com.stuntbyte.salesforce.core.metadata;

/**
 */
public class Metadata {
    
    private String name;
    private String lastChangedBy;
    private String salesforceId;

    public Metadata(String name, String lastChangedBy, String salesforceId) {
        this.name = name;
        this.lastChangedBy = lastChangedBy;
        this.salesforceId = salesforceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastChangedBy() {
        return lastChangedBy;
    }

    public void setLastChangedBy(String lastChangedBy) {
        this.lastChangedBy = lastChangedBy;
    }

    public String getSalesforceId() {
        return salesforceId;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
    }
}
