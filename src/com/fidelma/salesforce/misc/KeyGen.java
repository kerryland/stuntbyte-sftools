package com.fidelma.salesforce.misc;

import java.util.Calendar;
import java.util.Date;

/**
 */
public class KeyGen {

    public static void main(String[] args) throws Exception {
        checkLicence(999, "Minion O'Toole", "Minion O'Toole", "Fidelma Company", Licence.USER_LICENCE, 3000, Calendar.DECEMBER, 31);
        checkLicence(999, "Kerry Sainsbury", "Kerry Sainsbury", "Fidelma Company", Licence.USER_LICENCE, 2010, Calendar.DECEMBER, 31);
        checkLicence(1000, "Kerry Sainsbury", "Kerry Sainsbury", "Fidelma Company", Licence.USER_LICENCE, 3000, Calendar.DECEMBER, 31);
        checkLicence(1001, "Jukka Hakala", "Jukka Hakala", "Jukka Company", Licence.USER_LICENCE, 2013, Calendar.DECEMBER, 31);
        checkLicence(1002, "Darko Bohinc", "Darko Bohinc", "Darko Bohinc", Licence.USER_LICENCE, 3000, Calendar.DECEMBER, 31);
        checkLicence(1003, "Fronde Admin", "Fronde Admin", "Fronde Admin", Licence.USER_LICENCE, 3000, Calendar.DECEMBER, 31);
    }

    public static String checkLicence(int customerNumber, String licenceName, String username, String orgname, byte licenceType,
                                       int year, int month, int day) throws Exception {

        Calendar expires = Calendar.getInstance();
        expires.set(year, month, day);

        // Create a licence
        Licence licence = new Licence(customerNumber, licenceName, licenceType, expires);

        Encrypter encrypter = new SyncCrypto(licenceName.toLowerCase());

        String key = encrypter.encrypt(licence.getBytes());
        key = key.replaceAll("=", "");
        System.out.println(licenceName + " licence is " + key);

        LicenceService ls = new LicenceService();

        Calendar today = Calendar.getInstance();

        if (expires.after(today)) {
            if (!ls.checkLicence(username, orgname, key)) {
                throw new Exception("Licence failed for " + customerNumber + " " + username);
            }
        }
        return key;

    }
}
