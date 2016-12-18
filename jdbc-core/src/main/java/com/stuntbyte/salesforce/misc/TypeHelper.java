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
package com.stuntbyte.salesforce.misc;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 */
public class TypeHelper {

    public static String timestampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static String dateFormat = "yyyy-MM-dd";

    public static String dataTypeToClassName(Integer dataType) throws SQLException {

        if (dataType == Types.LONGVARCHAR || dataType == Types.VARCHAR) {
            return String.class.getName();
        }

        if (dataType == Types.INTEGER) {
            return Integer.class.getName();
        }

        if (dataType == Types.NUMERIC || dataType == Types.DECIMAL) {
            return BigDecimal.class.getName();
        }

        if (dataType == Types.BOOLEAN) {
            return Boolean.class.getName();
        }

        if (dataType == Types.DOUBLE) {
            return Double.class.getName();
        }

        if (dataType == Types.DATE) {
            return java.sql.Date.class.getName();
        }

        if (dataType == Types.TIMESTAMP) {
            return Timestamp.class.getName();
        }

        if (dataType == Types.TIME) {
            return Time.class.getName();
        }

        if (dataType == Types.ARRAY) {
            return java.sql.Array.class.getName();
        }


        //TODO: OTHER, VARBINARY
        throw new SQLException("Don't know class for " + dataType);


    }

    // TODO: See TYPE_INFO_DATA for all the ones we need to cover
    public static Object dataTypeConvert(String value, Integer dataType) throws ParseException {

        if (value == null) {
            return value;
        }
        if (dataType == null) {
            return value;
        }
        if (dataType == Types.INTEGER) {
            return new BigDecimal(value).intValue();
//            return Integer.parseInt(value);

        }
        if (dataType == Types.DOUBLE) {
            return Double.parseDouble(value);
        }
        if (dataType == Types.BOOLEAN) {
            return Boolean.parseBoolean(value);
        }

        if (dataType == Types.DECIMAL) {
//            return new BigDecimal(value);
            return value;
        }

        if (dataType == Types.DATE) {
            SimpleDateFormat dateSdf = new SimpleDateFormat(dateFormat);
            dateSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return dateSdf.parse(value);
        }

        if (dataType == Types.TIMESTAMP) {
            SimpleDateFormat dateSdf = new SimpleDateFormat(timestampFormat);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(dateSdf.parse(value).getTime());
            return cal;
        }


        //TODO: TIME,  ARRAY, OTHER, VARBINARY

        return value;
    }

}
