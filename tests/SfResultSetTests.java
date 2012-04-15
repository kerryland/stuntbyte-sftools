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

        assertTrue(rs.isBeforeFirst());
        assertFalse(rs.isFirst());
        assertFalse(rs.isLast());
        assertFalse(rs.isAfterLast());

        assertFalse(rs.next());

        assertFalse(rs.isBeforeFirst());
        assertFalse(rs.isFirst());
        assertFalse(rs.isLast());
        assertTrue(rs.isAfterLast());

        assertNotNull(rs.getMetaData());
    }

}
