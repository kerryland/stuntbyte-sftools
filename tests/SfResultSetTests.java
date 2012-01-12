import com.stuntbyte.salesforce.jdbc.SfResultSet;
import org.junit.Assert;
import org.junit.Test;

/**
 */
public class SfResultSetTests {

    @Test
    public void testEmptyResultSet() throws Exception {
        SfResultSet rs = new SfResultSet();
        Assert.assertFalse(rs.next());
        Assert.assertNotNull(rs.getMetaData());
    }

}
