package com.stuntbyte.salesforce.misc;

import javax.swing.text.DateFormatter;
import java.text.DateFormat;
import java.util.BitSet;
import java.util.Calendar;

/**
 */
public class KeyGen {

    public static void main(String[] args) throws Exception {
        /*
Minion O'Toole licence is 3qj6e8qaLfxP72hE5M2noA
Kerry Sainsbury expired licence is 8Y5Ez6AiYm1cYvbB4MMqog
Kerry Sainsbury licence is ntpOo9JjXHQ2uOYAlM724w
Jukka Hakala licence is sPZB0H-rlfKrSUZlsYEQhQ
Darko Bohinc licence is cKuBGhDLCb0lzpw80CNM8w
Fronde Admin licence is mUv1IP4b5zWa4m-Er8EiLw
         */
        
        BitSet USER_LICENCE = getPersonalLicenceFeatures();
        
        checkLicence(999, "Minion O'Toole", "Minion O'Toole", "Fidelma Company", USER_LICENCE, 3000, Calendar.DECEMBER, 31);
        checkLicence(999, "Kerry Sainsbury", "Kerry Sainsbury", "Fidelma Company", USER_LICENCE, 2010, Calendar.DECEMBER, 31);
        checkLicence(1000, "Kerry Sainsbury", "Kerry Sainsbury", "Fidelma Company", USER_LICENCE, 3000, Calendar.DECEMBER, 31);
        checkLicence(1001, "Jukka Hakala", "Jukka Hakala", "Jukka Company", USER_LICENCE, 2013, Calendar.DECEMBER, 31);
        checkLicence(1002, "Darko Bohinc", "Darko Bohinc", "Darko Bohinc", USER_LICENCE, 3000, Calendar.DECEMBER, 31);
        checkLicence(1003, "Fronde Admin", "Fronde Admin", "Fronde Admin", USER_LICENCE, 3000, Calendar.DECEMBER, 31);

        checkLicence(1004, "PERSONAL_DEMO", "PERSONAL_DEMO", "PERSONAL_DEMO", USER_LICENCE, 2012, Calendar.MARCH, 31);
    }

    public static String checkLicence(int customerNumber, String licenceName, String username, String orgname, BitSet licenceType,
                                      int year, int month, int day) throws Exception {

        Calendar expires = Calendar.getInstance();
        expires.set(year, month, day);

        // Create a licence
        Licence licence = new Licence(customerNumber, licenceName, expires);
        licence.setFeatures(licenceType);

        Encrypter encrypter = new SyncCrypto(licenceName.toLowerCase());

        String key = encrypter.encrypt(licence.getBytes());
        key = key.replaceAll("=", "");
        System.out.println(licenceName + " licence is " + key + " " + year + "-" + (month + 1) + "-" + day);

        LicenceService ls = new LicenceService();

        Calendar today = Calendar.getInstance();

        if (expires.after(today)) {
            if (!ls.checkLicence(username, orgname, key)) {
                throw new Exception("Licence failed for " + customerNumber + " " + username);
            }
        }
        return key;

    }

    public static BitSet getPersonalLicenceFeatures() {
        BitSet bits = new BitSet(8);
        bits.set(Licence.JDBC_LICENCE_BIT);
        bits.set(Licence.DEPLOYMENT_TOOL_LICENCE_BIT);
        bits.set(Licence.PERSONAL_USER_LICENCE_BIT);
        return bits;
    }

}
