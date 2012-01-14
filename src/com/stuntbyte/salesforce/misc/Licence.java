package com.stuntbyte.salesforce.misc;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Calendar;

/**
 */
public class Licence {

//    static byte USER_LICENCE = 0x01;
//
//    static byte ORG_LICENCE = 0x00; // Whatever it needs to be
//
//    static byte GOD_LICENCE = 0x03; // Don't care about username or organisation name


    // Licence type bits
    public static int JDBC_LICENCE_BIT = 0;
    public static int DEPLOYMENT_TOOL_LICENCE_BIT = 1;
    public static int PERSONAL_USER_LICENCE_BIT = 2; // on = personal. off = organisation
    public static int FREE_LIMITED_LICENCE_BIT = 3;

    private int customerNumber;
    private String name;
    private byte type = 0;
    private Calendar expires;
    private byte[] storedNameHash;


    public Licence(Integer customerNumber, String name, Calendar expiresCalendar) {
        this.customerNumber = customerNumber;
        this.name = name;
        this.expires = expiresCalendar;
    }

    public void setFeatures(BitSet bits) {
        type = bitsToByte(bits);
    }
    
    public boolean isFeatureAvailable(int licenceTypeBit) {
        BitSet bits = new BitSet(8);
        for (int i=0; i< 8; i++) {
            if ((type &(1<<(i%8))) > 0) {
                bits.set(i);
            }
        }
        return (bits.get(licenceTypeBit));
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

//    public byte getUserLicence() {
//
//    }
//
//
//    private void
    
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
        type = byteBuffer.get();
        storedNameHash = new byte[bytes.length - 9];

        byteBuffer.get(storedNameHash);

        this.name = name;
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
        byteBuffer.put(type);               // 1 byte
        byteBuffer.put(storedNameHash);
        return byteBuffer.array();
    }


    public int getCustomerNumber() {
        return customerNumber;
    }

    public String getName() {
        return name;
    }

//    public byte getType() {
//        return type;
//    }

    public byte[] getStoredNameHash() {
        return storedNameHash;
    }

    public Calendar getExpires() {
        return expires;
    }
}
