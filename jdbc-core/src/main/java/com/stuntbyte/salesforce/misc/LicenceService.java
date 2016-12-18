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

import com.sforce.ws.ConnectionException;
import com.stuntbyte.salesforce.jdbc.LicenceException;

import java.util.Calendar;

/**
 */
public class LicenceService {

    /**
     * Check to see if this is a valid JDBC licence
     *
     * Tested via LoginHelperTests.
     */
    public LicenceResult checkLicence(String userFullName, String organizationName, String key) throws Exception {
        userFullName = userFullName.toLowerCase();
        organizationName = organizationName.toLowerCase();

        Licence licence = decryptLicence("PERSONAL_DEMO".toLowerCase(), key);
        if (licence == null) {
            licence = decryptLicence(userFullName, key);
        }
        if (licence == null) {
            licence = decryptLicence(organizationName, key);
        }

//        boolean licenceOk = (licence != null && licence.supportsPersonalLicence() && licence.supportsJdbcFeature());


        if (licence == null || licence.getExpires().before(Calendar.getInstance())) {
            throw new LicenceException("JDBC Licence has expired or is invalid for " +
                    userFullName + "/" + organizationName + "/" + key);
        }

        LicenceResult result = new LicenceResult();
//        result.setLicenceOk(true); // seems redundant
        result.setLicence(licence);
        return result;
    }


    private Licence decryptLicence(String name, String key) throws Exception {
        Decrypter decrypter = new SyncCrypto(name);

        int padCount = key.length() % 4;
        for (int i = 0; i < padCount; i++) {
            key = key + "=";
        }

        Licence licence = null;
        byte[] decrypted = decrypter.decrypt(key);
        if (decrypted != null) {
            licence = new Licence(decrypted, name);
        }
        return licence;
    }
}
