package com.stuntbyte.salesforce.misc;

import com.sforce.ws.ConnectionException;

import java.util.Arrays;
import java.util.Calendar;

/**
 */
public class LicenceService {


    // Tested via LoginHelperTests
    public boolean checkLicence(String userFullName, String organizationName, String key) throws Exception {
        userFullName = userFullName.toLowerCase();
        organizationName = organizationName.toLowerCase();

        Licence licence = decryptLicence("PERSONAL_DEMO".toLowerCase(), key);

        boolean licenceOk = (licence != null &&
                licence.isFeatureAvailable(Licence.PERSONAL_USER_LICENCE_BIT) &&
                licence.isFeatureAvailable(Licence.JDBC_LICENCE_BIT));


        if (!licenceOk) {
            licence = decryptLicence(userFullName, key);

            licenceOk = (licence != null &&
                    licence.isFeatureAvailable(Licence.PERSONAL_USER_LICENCE_BIT) &&
                    licence.isFeatureAvailable(Licence.JDBC_LICENCE_BIT));

            if (!licenceOk) {
                licence = decryptLicence(organizationName, key);

                licenceOk = (licence != null && licence.isFeatureAvailable(Licence.ORGANISATION_LICENCE_BIT) &&
                        licence.isFeatureAvailable(Licence.JDBC_LICENCE_BIT));


            }
        }

        if (licence == null || licence.getExpires().before(Calendar.getInstance())) {
            throw new ConnectionException("JDBC Licence has expired or is invalid");
        }

        return licenceOk;
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
