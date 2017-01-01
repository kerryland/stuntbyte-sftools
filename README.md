# Welcome to the Stuntbyte Salesforce Tools

Here are a number of tools built to help Developers work more productively with the [salesforce.com](http://www.salesforce.com/) and [database.com](http://www.database.com/) platforms.

These tools started in February 2011 when I couldn't put up with the appalling "Force IDE" any longer, followed by the JDBC driver when I couldn't
put up with the appalling database query tools, and a deployment tool when I couldn't put up with the appalling deployment process.

Once I had these pieces in place I built data migration tools to allow the duplication of Salesforce instances. This tool is not as robust as the others because
I came up with another solution to the problem: Stop working with Salesforce.

You can read more about my experience of writing Salesforce code [here](https://docs.google.com/document/d/1piRkevGOfv1GFcqFb5fRFs4yUIFMK9a-mRW82EJzMQ8/edit?usp=sharing).
It is, I hope, out-of-date now, but it still makes me laugh :-)

More information about the tools here:

*   **[JDBC (Introduction)](jdbc/README.md)** - Use decent database query and update tools to work with Salesforce Databases.
    * [Configuration](jdbc/docs/jdbc-configuration.md)
    * [Supported SQL](jdbc/docs/sql.md)
*   **[Deploy](docs/deployment-tool.md)** - A deployment tool for Salesforce. It takes away much of the pain of Salesforce deployments.
*   **[IDE](docs/ide.md)** - A replacement for the "Force IDE". It lets developers use quality editors like Sublime Text 2 and VIM to compile, test, and navigate Salesforce Apex code.
*   **[Migration/Export/Grails](migration/README.md)** - When you have a JDBC driver and a deployment tool you can do fun things with Salesforce Instances.

https://groups.google.com/forum/#!forum/stuntbyte

Early in development of the JDBC driver I found two projects that I thought I could just bolt together, and magic would
happen. The reality was different, and I actually ended up only swiping small amounts of code, but they deserve to be highlighted.

I've grabbed:

* Lexical parsing code from Gregory Smith (gsmithfarmer@gmail.com)'s "SQLForce" code
via https://github.com/abhidotnet/sqlforce (repurposed as com.stuntbyte.salesforce.jdbc.sqlforce).
Released under the Eclipse Public Licence 1.0. You might like to look at http://www.capstorm.com for more of his tools.

* Salesforce Data type mapping from Keith Clarke (keith.clarke.claimvantage@gmail.com)'s "Force Metadata" code
via https://code.google.com/archive/p/force-metadata-jdbc-driver/ (repurposed as com.stuntbyte.salesforce.jdbc.metaforce).
Released under the "New BSD Licence" (aka BSD 3)

Random dev notes:
* ```mvn license:format``` will add MIT headers