/**
 * The MIT License
 * Copyright © 2011-2017 Kerry Sainsbury
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
