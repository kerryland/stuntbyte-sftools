
import com.stuntbyte.salesforce.jdbc.metaforce.ColumnMap;
import com.stuntbyte.salesforce.jdbc.metaforce.ForceResultSet;
import org.junit.Assert;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class SfMetadataResultSetMetadataTests {

    @Test
    public void testEmptyResultSet() throws Exception {
        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();

        ColumnMap<String, Object> row = new ColumnMap<String, Object>();
        row.put("PKTABLE_CAT", "x");
        row.put("PKTABLE_SCHEM", "y");

        maps.add(row);
        ResultSet rs = new ForceResultSet(maps);
        ResultSetMetaData metaData = rs.getMetaData();

        Assert.assertEquals(2, metaData.getColumnCount());
        Assert.assertEquals("PKTABLE_CAT", metaData.getColumnName(1));
        Assert.assertEquals("PKTABLE_SCHEM", metaData.getColumnName(2));
    }

}
