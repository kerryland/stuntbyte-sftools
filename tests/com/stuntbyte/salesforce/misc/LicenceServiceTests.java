package com.stuntbyte.salesforce.misc;

import junit.framework.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.BitSet;
import java.util.Calendar;

/**
 */
public class LicenceServiceTests {

    @Test
    public void testActiveLicenceCheck() throws Exception {
        Calendar now = Calendar.getInstance();

        String licence = KeyGen.checkLicence(1, "Bob", "Bob", "BobCo", KeyGen.getPersonalLicenceFeatures(),
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));

        LicenceService ls = new LicenceService();

        LicenceResult licenceResult = ls.checkLicence("Bob", "Bob Co", licence);

        Assert.assertTrue(licenceResult.getLicenceOk());
    }


    @Test
    public void testExpiredLicenceCheck() throws Exception {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, -1);

        String licence = KeyGen.checkLicence(1, "Bob", "Bob", "BobCo", KeyGen.getPersonalLicenceFeatures(),
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));

        LicenceService ls = new LicenceService();

        try {
            ls.checkLicence("Bob", "Bob Co", licence);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Assert.assertTrue(sw.toString().contains("JDBC Licence has expired"));

        }
    }
}
