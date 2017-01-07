package com.stuntbyte.salesforce.jdbc.metaforce;


import com.stuntbyte.salesforce.jdbc.SfConnection;
import com.stuntbyte.salesforce.misc.TestHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ResultSetFactoryTest {

    private static TestHelper testHelper = new TestHelper();

    @Test
    public void testGetTable() throws Exception {
        SfConnection conn = testHelper.getTestConnection();

        final ResultSetFactory rsf = conn.getMetaDataFactory();
        Table table = rsf.getTable("SF", "User");
        Column addressColumn = table.getColumn("address");
        Assert.assertEquals("address", addressColumn.getType());
    }
}
