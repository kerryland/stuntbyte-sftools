package com.fidelma.salesforce.misc;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 2/04/11
 * Time: 6:45 AM
 * To change this template use File | Settings | File Templates.
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

        if (dataType == null) {
            return value;
        }
        if (dataType == Types.INTEGER) {
            return Integer.parseInt(value);
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
