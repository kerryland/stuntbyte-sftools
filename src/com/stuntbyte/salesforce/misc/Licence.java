package com.stuntbyte.salesforce.misc;

import java.nio.ByteBuffer;
import java.util.Calendar;

/**
 */
public class Licence {

    static byte USER_LICENCE = 0x01;
    static byte ORG_LICENCE = 0x02;


    private int customerNumber;
    private String name;
    private byte type;
    private Calendar expires;
    private byte[] storedNameHash;

    public Licence(Integer customerNumber, String name, byte type, Calendar expiresCalendar) {
        this.customerNumber = customerNumber;
        this.name = name;
        this.type = type;
        this.expires = expiresCalendar;
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

    public byte getType() {
        return type;
    }

    public byte[] getStoredNameHash() {
        return storedNameHash;
    }

    public Calendar getExpires() {
        return expires;
    }
}
