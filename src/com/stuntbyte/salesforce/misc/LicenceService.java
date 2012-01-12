package com.stuntbyte.salesforce.misc;

import java.util.Arrays;
import java.util.Calendar;

/**
 */
public class LicenceService {


    // Tested via LoginHelperTests
    public boolean checkLicence(String userFullName, String organizationName, String key) throws Exception {
        userFullName = userFullName.toLowerCase();
        organizationName = organizationName.toLowerCase();

        boolean result = true;

        if (!checkLicenceOk(userFullName, key, Licence.USER_LICENCE)) {
            if (!checkLicenceOk(organizationName, key, Licence.ORG_LICENCE)) {
                result = false;
            }
        }
        return result;
    }


    private boolean checkLicenceOk(String name, String key, byte licenceType) throws Exception {
        Boolean result = false;

        Decrypter decrypter = new SyncCrypto(name);

        int padCount = key.length() % 4;
        for (int i = 0; i < padCount; i++) {
            key = key + "=";
        }

        byte[] decrypted = decrypter.decrypt(key);
        if (decrypted != null) {
            Licence licence = new Licence(decrypted, name);

            if (licence.getExpires().before(Calendar.getInstance())) {
                throw new Exception("JDBC Licence has expired");
            }

            if (licence.getType() == licenceType) {

                byte[] nameHash = licence.calculateNameHash(name);

                if (Arrays.equals(nameHash, licence.getStoredNameHash())) {
                    result = true;

                }
            }
        }
        return result;
    }
}
