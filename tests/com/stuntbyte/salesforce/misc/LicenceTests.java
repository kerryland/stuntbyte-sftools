package com.stuntbyte.salesforce.misc;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;

/**
 */
public class LicenceTests {

    @Test
    public void testThis() throws Exception {
        BitSet bits = new BitSet(8);
        bits.set(Licence.JDBC_LICENCE_BIT);
        
        Licence licence = new Licence(1000,  "Kerry Sainsbury", Calendar.getInstance());
        licence.setFeatures(bits);
        Assert.assertEquals(1000, licence.getCustomerNumber());
        Assert.assertEquals("Kerry Sainsbury", licence.getName());
        Assert.assertTrue(licence.isFeatureAvailable(Licence.JDBC_LICENCE_BIT));
        Assert.assertFalse(licence.isFeatureAvailable(Licence.ORGANISATION_LICENCE_BIT));
        licence.generateNameHash();

        byte[] bytes = licence.getBytes();

        // Now reconstitute the licence from bytes
        Licence second = new Licence(bytes, "Kerry Sainsbury");
        Assert.assertEquals(1000, second.getCustomerNumber());
        Assert.assertEquals("Kerry Sainsbury", second.getName());
        Assert.assertTrue(second.isFeatureAvailable(Licence.JDBC_LICENCE_BIT));
        Assert.assertFalse(second.isFeatureAvailable(Licence.ORGANISATION_LICENCE_BIT));

        Assert.assertTrue(Arrays.equals(licence.getStoredNameHash(), second.getStoredNameHash()));
    }
}
