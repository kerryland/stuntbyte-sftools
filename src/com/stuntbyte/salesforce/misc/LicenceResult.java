package com.stuntbyte.salesforce.misc;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 14/01/12
 * Time: 5:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class LicenceResult {
    private Boolean licenceOk;
    private Licence licence;

    public Boolean getLicenceOk() {
        return licenceOk;
    }

    public void setLicenceOk(Boolean licenceOk) {
        this.licenceOk = licenceOk;
    }

    public Licence getLicence() {
        return licence;
    }

    public void setLicence(Licence licence) {
        this.licence = licence;
    }
}
