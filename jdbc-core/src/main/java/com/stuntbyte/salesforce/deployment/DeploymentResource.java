package com.stuntbyte.salesforce.deployment;

/**
 */
public class DeploymentResource {
    private String filepath;
    private String code;
    private String metaData;

    public String getFilepath() {
        return filepath;
    }

    public String getCode() {
        return code;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }
}
