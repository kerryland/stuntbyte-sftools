# JDBC Configuration

In order to install the Stuntbyte JDBC driver into your tool or application you will need:

### **Driver Classname**

com.stuntbyte.salesforce.jdbc.SfDriver

### **JDBC URL**

Here is a sample JDBC URL. Make sure to use the correct domain for your instance. ie: "login.salesforce.com" for a Production or Developer instance, and "test.salesforce.com" for a Sandbox instance.

```
jdbc:sfdc:https://login.salesforce.com
```

The URL also takes an optional "datatypes" parameter that controls how the various data types are displayed to the user. For example:

```
jdbc:sfdc:https://login.salesforce.com?datatypes=sql92
```

The supported values for the "datatypes" parameter are:

*   **api** uses the datatypes as returned by the Salesforce API. This offers the richest range of data types, and is the default.
*   **ui** uses the datatypes as they appear in the Salesforce UI. Ideal for users who are more comfortable at that level.
*   **sql92** uses SQL92 datatypes. Useful for tools that can't handle Salesforce datatypes, like [Mogawai](http://mogwai.sourceforge.net/).

Note that "datatypes" can also be configured as a Property in the database connection, as well as part of the server URL.

### **JDBC Username**

This is the username you use to normally login to the desired Salesforce instance.

### **JDBC Password**

You should define the password as: ```<sfdc-password><sfdc-security-token>```, for example:

**S3CR3T!a2dQxG0Y3kqWiJQVEwnYtryr1Ja1**
