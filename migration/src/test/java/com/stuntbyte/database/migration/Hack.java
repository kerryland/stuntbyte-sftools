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
package com.stuntbyte.database.migration;

import com.stuntbyte.salesforce.jdbc.SfConnection;

import java.sql.DriverManager;
import java.util.Properties;

/**
 */
public class Hack {
    public static void main(String[] args) throws Exception {

        Class.forName("com.stuntbyte.salesforce.jdbc.SfDriver");

        Properties info = new Properties();

//        String destUser = "fronde.admin@localist.co.nz.devkerry";
//        String destPwd = "jrP2U0TnCWok3CTtOfnhPC6UjYrOgQzI";


        info = new Properties();
//        info.put("user", destUser);
//        info.put("password", destPwd);

        info.put("user", "kerry.sainsbury@nzpost.co.nz.sandbox");
        info.put("password", "xJiKif3IeCLiZKNervuO3W3ozLxyQ6cm");

        // Get a connection to the database
        SfConnection destSalesforce = (SfConnection) DriverManager.getConnection(
                "jdbc:sfdc:https://test.salesforce.com"
                , info);

    }
}
