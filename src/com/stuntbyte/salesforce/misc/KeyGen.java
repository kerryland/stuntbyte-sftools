package com.stuntbyte.salesforce.misc;

import java.util.BitSet;
import java.util.Calendar;

/**
 */
public class KeyGen {

    public static void main(String[] args) throws Exception {
        /*


Minion O'Toole licence is 3qj6e8qaLfzdhSQlPeQkDQ 3000-12-31
Kerry Sainsbury licence is 8Y5Ez6AiYm2mTV6devuA5A 2010-12-31
Kerry Sainsbury licence is ntpOo9JjXHTmYiYIyaG0XQ 3000-12-31
Jukka Hakala licence for user Jukka Hakala is yRP5n3YnO9I6UyTEm0W7_A 3000-12-31
Darko Bohinc licence is cKuBGhDLCb1-MZw3FtjPUQ 3000-12-31
Fronde Admin licence is mUv1IP4b5zXQiWf1AYowdQ 3000-12-31
PERSONAL_DEMO licence is MxjetovtygmPUHNqoPAXGQ 2012-10-31
PERSONAL_DEMO licence is PVPSZ3MvLhI2bxoZKhuqrQ 2013-03-31
Free Limited SQL licence is bsCbe26QJkFi_7H_ICMPuQ 3000-3-31
1007 Bo Coughlin licence is gzr9OIltkZ_JMrzD3wx7uQ 3000-12-31

StuntByte demo: support@stuntbyte.com
                licence(0-pblwi-KTAfqhSW8Q9_tg)sfdc(p1sswordncOKWYdk3eBVADueynFLfcCp)
         */

        LicenceSetter USER_JDBC_AND_DEPLOY = new LicenceSetter() {
            public void setFeatures(Licence licence) {
                licence.setJdbcFeature(true);
                licence.setDeploymentFeature(true);
                licence.setPersonalLicence(true);
            }
        };

        LicenceSetter USER_JDBC_ONLY = new LicenceSetter() {
            public void setFeatures(Licence licence) {
                licence.setJdbcFeature(true);
                licence.setDeploymentFeature(false);
                licence.setPersonalLicence(true);
            }
        };


        LicenceSetter FREE_LIMITED_SQL = new LicenceSetter() {
            public void setFeatures(Licence licence) {
                licence.setJdbcFeature(true);
                licence.setPersonalLicence(true);
                licence.setLimitedLicence(true);
            }
        };

        
        checkLicence(998, "Pleb Pleb", "Pleb Pleb", "Fidelma Company", USER_JDBC_AND_DEPLOY, 3000, Calendar.DECEMBER, 31);
        checkLicence(999, "Minion O'Toole", "Minion O'Toole", "Fidelma Company", USER_JDBC_AND_DEPLOY, 3000, Calendar.DECEMBER, 31);
        checkLicence(999, "Kerry Sainsbury", "Kerry Sainsbury", "Fidelma Company", USER_JDBC_AND_DEPLOY, 2010, Calendar.DECEMBER, 31);
        checkLicence(1000, "Kerry Sainsbury", "Kerry Sainsbury", "Fidelma Company", USER_JDBC_AND_DEPLOY, 3000, Calendar.DECEMBER, 31);
        checkLicence(1001, "Jukka Hakala", "Jukka Hakala", "Jukka Hakala", USER_JDBC_AND_DEPLOY, 3000, Calendar.DECEMBER, 31);
        checkLicence(1002, "Darko Bohinc", "Darko Bohinc", "Darko Bohinc", USER_JDBC_AND_DEPLOY, 3000, Calendar.DECEMBER, 31);
        checkLicence(1003, "Fronde Admin", "Fronde Admin", "Fronde Admin", USER_JDBC_AND_DEPLOY, 3000, Calendar.DECEMBER, 31);

        checkLicence(1004, "PERSONAL_DEMO", "PERSONAL_DEMO", "PERSONAL_DEMO", USER_JDBC_AND_DEPLOY, 2013, Calendar.MARCH, 31);


        checkLicence(1005, "PERSONAL_DEMO", "Free Limited SQL", "PERSONAL_DEMO", FREE_LIMITED_SQL, 3000, Calendar.MARCH, 31);
        checkLicence(1006, "PERSONAL_DEMO", "StuntByte Demo", "PERSONAL_DEMO", USER_JDBC_AND_DEPLOY, 3000, Calendar.MARCH, 31);

        checkLicence(1007, "Heather Coughlin", "Bo Coughlin", "Heather Coughlin", USER_JDBC_AND_DEPLOY, 3000, Calendar.DECEMBER, 31);

        checkLicence(1008, "Jeffrey Huth", "Admin GreatVines", "n/a", USER_JDBC_ONLY, 3000, Calendar.DECEMBER, 31);
    }

    interface LicenceSetter {
        void setFeatures(Licence licence);
    }



    public static String checkLicence(int customerNumber,
                                      String licenceName,
                                      String username, String orgname,
                                      LicenceSetter licenceSetter, int year, int month, int day) throws Exception {

        Calendar expires = Calendar.getInstance();
        expires.set(year, month, day);

        // Create a licence
        Licence licence = new Licence(customerNumber, username, expires);
        licenceSetter.setFeatures(licence);

        String key = generateKey(licence);
        
        System.out.println(customerNumber + " " + licenceName + " licence for user " + username + " is " + key + " " + year + "-" + (month + 1) + "-" + day);


        LicenceService ls = new LicenceService();

        Calendar today = Calendar.getInstance();

        if (expires.after(today)) {
            LicenceResult licenceResult = ls.checkLicence(username, orgname, key);
//            System.out.println("Deployable=" + licenceResult.getLicence().supportsDeploymentFeature());
//            LicenceResult licenceResult = ls.checkLicence(username, orgname, key);
//            if (!licenceResult.getLicenceOk()) {
//                throw new Exception("Licence failed for " + customerNumber + " " + username);
//            }
        }
        return key;

    }
    public static String generateKey(Licence licence) throws Exception {
        Encrypter encrypter = new SyncCrypto(licence.getName().toLowerCase());

        String key = encrypter.encrypt(licence.getBytes());
        key = key.replaceAll("=", "");
        return key;

    }


}
