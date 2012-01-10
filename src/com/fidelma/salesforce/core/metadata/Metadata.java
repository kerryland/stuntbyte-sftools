package com.fidelma.salesforce.core.metadata;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 10/01/12
 * Time: 10:15 AM
 * To change this template use File | Settings | File Templates.
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
