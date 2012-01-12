package com.stuntbyte.salesforce.deployment;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 1/05/11
 * Time: 7:54 AM
 * To change this template use File | Settings | File Templates.
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
