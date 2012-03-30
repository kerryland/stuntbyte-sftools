package com.stuntbyte.salesforce.misc;

import com.sforce.ws.ConnectionException;
import com.stuntbyte.salesforce.jdbc.LicenceException;

import java.util.Arrays;
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
