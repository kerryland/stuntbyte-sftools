package com.stuntbyte.salesforce.misc;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;

/**
 */
public class LicenceTests {

    @Test
    public void testThis() throws Exception {
        Licence licence = new Licence(1000,  "Kerry Sainsbury", Licence.USER_LICENCE, Calendar.getInstance());
        Assert.assertEquals(1000, licence.getCustomerNumber());
        Assert.assertEquals("Kerry Sainsbury", licence.getName());
        Assert.assertEquals(Licence.USER_LICENCE, licence.getType());
        licence.generateNameHash();

        byte[] bytes = licence.getBytes();

        Licence second = new Licence(bytes, "Kerry Sainsbury");
        Assert.assertEquals(1000, second.getCustomerNumber());
        Assert.assertEquals("Kerry Sainsbury", second.getName());
        Assert.assertEquals(Licence.USER_LICENCE, second.getType());

        Assert.assertTrue(Arrays.equals(licence.getStoredNameHash(), second.getStoredNameHash()));
    }
}
