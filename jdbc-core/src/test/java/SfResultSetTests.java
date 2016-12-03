import com.stuntbyte.salesforce.jdbc.SfResultSet;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class SfResultSetTests {

    @Test
    public void testEmptyResultSet() throws Exception {
        SfResultSet rs = new SfResultSet();

        Assert.assertTrue(rs.isBeforeFirst());
        Assert.assertFalse(rs.isFirst());
        Assert.assertFalse(rs.isLast());
        Assert.assertFalse(rs.isAfterLast());

        Assert.assertFalse(rs.next());

        Assert.assertFalse(rs.isBeforeFirst());
        Assert.assertFalse(rs.isFirst());
        Assert.assertFalse(rs.isLast());
        Assert.assertTrue(rs.isAfterLast());

        Assert.assertNotNull(rs.getMetaData());
    }

}
