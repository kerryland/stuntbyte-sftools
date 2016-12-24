# Welcome to the Stuntbyte Salesforce Tools

Here are a number of tools built to help Developers work more productively with the [salesforce.com](http://www.salesforce.com/) and [database.com](http://www.database.com/) platforms.

These tools started in February 2011 when I couldn't put up with the appalling "Force IDE" any longer, followed by the JDBC driver when I couldn't
put up with the appalling database query tools, and a deployment tool when I couldn't put up with the appalling deployment process.

Once I had these pieces in place I built data migration tools to allow the duplication of Salesforce instances. This tool is not as robust as the others because
I came up with another solution to the problem: Stop working with Salesforce.

You can read more about my experience of writing Salesforce code [here](https://docs.google.com/document/d/1piRkevGOfv1GFcqFb5fRFs4yUIFMK9a-mRW82EJzMQ8/edit?usp=sharing). It's, hopefully, out-of-date now, but it still makes me laugh :-)

*   **Stuntbyte JDBC** - A [JDBC](https://en.wikipedia.org/wiki/Java_Database_Connectivity) driver for Salesforce. It makes querying and updating Salesforce simple, and allows the use of best-of-breed database query tools. It also helps with data migration and reporting.
    * [Introduction](docs/jdbc-driver/index.md)
    * [Configuration](docs/jdbc-driver/jdbc-configuration.md)
    * [Supported SQL](docs/jdbc-driver/sql.md)
*   **[Stuntbyte Deploy](docs/deployment-tool.md)** - A deployment tool for Salesforce. It takes away much of the pain of Salesforce deployments.
*   **[Stuntbyte IDE](docs/ide.md)** - A replacement for the "Force IDE". It lets developers use quality editors like Sublime Text 2 and VIM to compile, test, and navigate Salesforce Apex code.


TODO: Exporter

TODO: Migrator
   (also need h2?)

TODO: Grails (does it work?)

TODO: Hibernate (com.stuntbyte.salesforce.jdbc.hibernate.SalesforceDialect)
     (also need slf4j)
https://groups.google.com/forum/#!forum/stuntbyte


I was inspired by two projects, and used small amounts of code from both:

Lexical parsing code from Gregory Smith (gsmithfarmer@gmail.com)'s "SQLForce" code
via https://github.com/abhidotnet/sqlforce (repurposed as com.stuntbyte.salesforce.jdbc.sqlforce).
Released under the Eclipse Public Licence 1.0. You might like to look at http://www.capstorm.com for more of his tools.

Salesforce Data type mapping from Keith Clarke (keith.clarke.claimvantage@gmail.com)'s "Force Metadata" code
via https://code.google.com/archive/p/force-metadata-jdbc-driver/ (repurposed as com.stuntbyte.salesforce.jdbc.metaforce).
Released under the "New BSD Licence" (aka BSD 3)


Alternatve JDBC Drivers:


Simba.
http://www.simba.com/product/salesforce-drivers-with-sql-connector/#purchase
Desktop: $199
Server: $999

cdata.
http://www.cdata.com/drivers/salesforce/order/jdbc/
Standard: $349 pa
Prof (server): $799 pa
Cloud: request quote