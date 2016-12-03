package com.stuntbyte.salesforce.misc;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Calendar;

/**
 */
public class Licence {

    // Licence type bits
    private static int JDBC_LICENCE_BIT = 0;
    private static int DEPLOYMENT_TOOL_LICENCE_BIT = 1;
    private static int PERSONAL_USER_LICENCE_BIT = 2; // on = personal. off = organisation
    private static int FREE_LIMITED_LICENCE_BIT = 3;

    private int customerNumber;
    private String name;
    private Calendar expires;
    private byte[] storedNameHash;

    private BitSet bits = new BitSet(8);

    public Licence(Integer customerNumber, String name, Calendar expiresCalendar) {
        this.customerNumber = customerNumber;
        this.name = name;
        this.expires = expiresCalendar;
    }


    void setJdbcFeature(boolean enable) {
        bits.set(Licence.JDBC_LICENCE_BIT, enable);
    }

    void setDeploymentFeature(boolean enable) {
        bits.set(Licence.DEPLOYMENT_TOOL_LICENCE_BIT, enable);
    }

    void setPersonalLicence(boolean enable) {
        bits.set(Licence.PERSONAL_USER_LICENCE_BIT, enable);
    }

    void setLimitedLicence(boolean enable) {
        bits.set(Licence.FREE_LIMITED_LICENCE_BIT, enable);
    }

    public boolean supportsJdbcFeature() {
        return bits.get(Licence.JDBC_LICENCE_BIT);
    }

    public boolean supportsDeploymentFeature() {
        return bits.get(Licence.DEPLOYMENT_TOOL_LICENCE_BIT);
    }

    public boolean supportsPersonalLicence() {
        return bits.get(Licence.PERSONAL_USER_LICENCE_BIT);
    }

    public boolean supportsLimitedLicence() {
        return bits.get(Licence.FREE_LIMITED_LICENCE_BIT);
    }



    public void setExpires(Calendar expires) {
        this.expires = expires;
    }


    private byte bitsToByte(BitSet bits) {
        byte result = 0;
        for (int i=0; i<bits.length(); i++) {
            if (bits.get(i)) {
                result |= 1<<(i%8);
            }
        }
        return result;
    }

    private int calendarToInt(Calendar expiresCalendar) {
        int dateInt = expiresCalendar.get(Calendar.YEAR) * 10000 +
               (expiresCalendar.get(Calendar.MONTH) + 1) * 100 +
               expiresCalendar.get(Calendar.DAY_OF_MONTH);

        return dateInt;
    }
    

    private Calendar intToCalendar(int yearMonthDay) {
        String ymd = "" + yearMonthDay;
        int year = Integer.parseInt(ymd.substring(0, 4));
        int month = Integer.parseInt(ymd.substring(4, 6));
        int day = Integer.parseInt(ymd.substring(6, 8));

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        return cal;
    }


    public Licence(byte[] bytes, String name) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        customerNumber = byteBuffer.getInt();
        expires = intToCalendar(byteBuffer.getInt());

        byte flags = byteBuffer.get();
        bits = fromByteArray(flags);

        storedNameHash = new byte[bytes.length - 9];

        byteBuffer.get(storedNameHash);

        this.name = name;
    }

    public static BitSet fromByteArray(byte byteX) {
        BitSet bits = new BitSet();
        for (int i=0; i< 8; i++) {
            if ((byteX&(1<<(i%8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }



    public void generateNameHash() throws Exception {
        this.storedNameHash = calculateNameHash(name);
    }

    public byte[] calculateNameHash(String name) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer = byteBuffer.putInt((name).toLowerCase().hashCode());  // 4 bytes
        return byteBuffer.array();
    }

    public byte[] getBytes() throws Exception {
        generateNameHash();

        ByteBuffer byteBuffer = ByteBuffer.allocate(storedNameHash.length + 9);
        byteBuffer = byteBuffer.putInt(customerNumber);  // 4 bytes
        byteBuffer = byteBuffer.putInt(calendarToInt(expires));  // 4 bytes
        byteBuffer.put(bitsToByte(bits));               // 1 byte
        byteBuffer.put(storedNameHash);
        return byteBuffer.array();
    }


    public int getCustomerNumber() {
        return customerNumber;
    }

    public String getName() {
        return name;
    }

    public byte[] getStoredNameHash() {
        return storedNameHash;
    }

    public Calendar getExpires() {
        return expires;
    }

    public void setName(String name) {
        this.name = name;
    }
}
