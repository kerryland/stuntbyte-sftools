/**
 * The MIT License
 * Copyright © 2011-2017 Kerry Sainsbury
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.stuntbyte.salesforce.misc;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

/**
 */
public class LicenceServiceTests {
    private Licence licence;

    @Before
    public void setup() {
        licence = new Licence(123, "Bob", Calendar.getInstance());
        licence.setJdbcFeature(true);
        licence.setDeploymentFeature(true);
        licence.setPersonalLicence(true);
    }

    
    @Test
    public void testLicenceProperties() {
        // Create a licence for JDBC only
//        Calendar now = Calendar.getInstance();
//
//        String licence = KeyGen.checkLicence(1, "Bob", "Bob", "BobCo", KeyGen.getPersonalLicenceFeatures(),
//                now.get(Calendar.YEAR),
//                now.get(Calendar.MONTH),
//                now.get(Calendar.DAY_OF_MONTH));
//
//        KeyGen.setPersonalLicenceFeatures(licence);

//        LicenceService ls = new LicenceService();
//
//        LicenceResult licenceResult = ls.checkLicence("Bob", "Bob Co", licence);

    }

    @Test
    public void testActiveLicenceCheck() throws Exception {
//        Calendar now = Calendar.getInstance();
//
//        String licence = KeyGen.checkLicence(1, "Bob", "Bob", "BobCo", KeyGen.getPersonalLicenceFeatures(),
//                now.get(Calendar.YEAR),
//                now.get(Calendar.MONTH),
//                now.get(Calendar.DAY_OF_MONTH));
//
        LicenceService ls = new LicenceService();

        LicenceResult licenceResult = ls.checkLicence("Bob", "Bob Co", KeyGen.generateKey(licence));

        Assert.assertTrue(licenceResult != null);
    }


    @Test
    public void testExpiredLicenceCheck() throws Exception {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        licence.setExpires(yesterday);

//        String licence = KeyGen.checkLicence(1, "Bob", "Bob", "BobCo", KeyGen.getPersonalLicenceFeatures(),
//                yesterday.get(Calendar.YEAR),
//                yesterday.get(Calendar.MONTH),
//                yesterday.get(Calendar.DAY_OF_MONTH));

        LicenceService ls = new LicenceService();

        try {
            ls.checkLicence("Bob", "Bob Co", KeyGen.generateKey(licence));
//            LicenceResult licenceResult = ls.checkLicence("Bob", "Bob Co", KeyGen.generateKey(licence));

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Assert.assertTrue(sw.toString().contains("JDBC Licence has expired"));

        }
    }
}
