# SQL DML

Here are the SQL DML commands we currently support:

## UPDATE

A normal SQL UPDATE statement, including support for expressions that refer to other columns:

```
UPDATE Lead
   SET firstName = firstname + ' ' + lastname,
       NumberOfEmployees=NumberOfEmployees*2*annualRevenue
 WHERE annualRevenue > 0
```

## INSERT

A normal SQL INSERT statement:
```
INSERT INTO Account(name, billingCity)
VALUES ('Mr Jones', 'Super City')
```

## DELETE

A normal SQL DELETE statement:

```
DELETE FROM account
WHERE billingCity = 'Super City'
```

## SELECT

SELECT behaves as per Salesforce [SOQL](http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_soql_select.htm), with the following enhancements:

*   Support for "SELECT *". This is almost priceless!
*   Support for column aliases via "as"

```
SELECT FirstName as fn, LastName as ln, CreatedBy.name as cn
FROM Lead
```

*   Support for "count(*)", as well as Salesforce-native "count()" and "count(id)".
*   Patches illegal SOQL to valid SQL, ie: "WHERE ID = null" to "WHERE 0 = 1".

## SELECT non-features

We haven't rewritten the Salesforce SQL engine, and still inherit the other Salesforce-supplied SOQL SELECT limitations, including no support for normal SQL joins.

We also don't support SOQL's nested SELECT functionality, because it doesn't really fit into the relational model. eg:

```
SELECT Account.Name, Type,
(SELECT b.LastName FROM Account.Contacts b)
FROM Account
```

## Data Type Literals

When using string literals in commands please adhere to the following formats:

| DataType | Format | Example |
|----------|--------|---------|
| DateTime | yyyy-MM-dd'T'HH:mm:ss.SSS'Z' | 2010-10-21T23:15:00.000Z |
| Date | yyyy-MM-dd | 2012-12-31 |
| MultiPickList | semi-colon-delimited values | Red;Blue;Green |

## General Limitations

UPDATE, DELETE and SELECT all depend on the SOQL WHERE clause, so they inherit the limitations provided by Salesforce. In particular you cannot write a WHERE clause that compares one column to another, eg:

```
SELECT * FROM Lead
WHERE LastActivityDate = ConvertedDate
```

# SQL DDL

Here are the SQL DDL commands we currently support:

## CREATE TABLE

We provide a way to create a Salesforce "custom object" via the JDBC driver:

```
CREATE TABLE <tableName> ( <columnName> <dataType> [null | not null ]
[ WITH <withClause> ] ,... );
```

For example:

```
create table superhero__c (
name string(80),
birthdate__c date,
cape_colour__c picklist('red', 'blue' default, 'green') sorted,
identity__c masterrecord references(secretidentity__c) with (relationshipName 'secretId'),
group__c reference references(superherogroup__c) with (relationshipName 'secretGroup'),
my_formula__c string(80) with (formula "name & ' ' & identity__r.Name"),
secret__c encryptedstring(50) with (maskChar asterisk, maskType all),
commitment__c percent(3,0) with (inlineHelpText "Here is some help!"));
```

### Data Types

We support the following datatypes via the CREATE and ALTER table commands.

#### Numeric datatypes

These data types support an optional "precision, scale" clause:

```
Currency[(<precision>, [<scale>])]
Percent[(<precision>, [<scale>])]
Double[(<precision>, [<scale>])]
Int[(<precision>)]
Decimal[(<precision>, [<scale>])]
```

#### String datatypes

These data types support an optional "length" clause:

```
String[(<length>)]
TextArea[(<length>)]
LongTextArea[(<length>)] with (visibleLines 3)
```

#### Picklist and Multipicklist datatype

[Multi]Picklist supports a list of valid values, a default indicator, and an optional "sorted" clause. Note that currently there is no support for "controlling field" in Picklist.

```
PickList(<value> [default] ... [sorted]
MultiPickList(<value> [default] ... [sorted]
```

#### Foreign key relationships

Foreign keys are defined via the "Reference" and "MasterRecord" data types:

```
Reference references (<referencedTable>) with (relationshipName <refName> [ , relationshipLabel '<labelName>'])
MasterRecord references (<referencedTable>) with (relationshipName <refName> [ , relationshipLabel '<labelName>'])
```

#### AutoNumber

The AutoNumber datatype, used with the built-in "Name" field, defaults to a format of {0000000000}, but this can be changed via the 'with' clause, eg:

```
AutoNumber with (displayFormat "{INV0000}")
```

#### Other datatypes

*   EncryptedString -- for example: encryptedstring(50) with (maskChar asterisk, maskType all),
*   Email
*   Phone
*   Url
*   base64 (synonym for LongTextArea)
*   Boolean
*   Byte
*   Date
*   DateTime
*   Summary

### Use of 'with clause'

The CREATE command maps standard SQL values such as the the column name, data type, and field size to an internal Salesforce structure. The 'with' clause is really just a back-door to support unusual features associated with a column definition. The values listed in the 'with' clause are passed through to Salesforce unmodified, and values are case sensitive.

The "with" values are documented by Salesforce [here](http://www.salesforce.com/us/developer/docs/api_meta/Content/customfield.htm). If you wanted, for example, to define "inline help text" for a field you would use:

```
...with (inlineHelpText "Here is some help!")

```

Some data types, like LongTextArea, Reference and MasterRecord actually _require_ the use of the 'with' clause, as documented above. Failure to include the 'with' clause will cause Salesforce to generate an error that provides a pretty good clue about what you need to add.

For example:

```
create table wibble__c (description__c longTextArea(12000));
```

Will generate an error:

```
Error: wibble__c.description__c Must specify 'visibleLines' for
a CustomField of type LongTextArea on line 1 col 372
```

So add a "with" clause as appropriate, eg:

```
create table wibble__c(description__c longTextArea(12000)
with (visibleLines 3))
```

Formula fields are also handled via the "with" clause. For example:

```
my_formula__c string(80) with (formula "name & ' ' & identity__r.Name"),

```

### Known Issues

There are a few things that might make you raise an eyebrow.

#### Unsupported Custom Object Properties

There is currently no way to define the following properties of Custom Object.

*   pluralLabel
*   enableFeeds
*   deploymentStatus
*   sharingModel

This simply hasn't been implemented. If there is a demand, it will be :-)

#### Silly Error Messages

You may see silly error messages, because Salesforce thinks you are a web browser. Eg: if you try to use a picklist field as part of a formula you will get the following HTML code embedded in the error message:

Picklist fields are only supported in certain functions. <a href="javascript:openPopupFocusEscapePounds('/HelpAndTrainingDoor?loc=HOME&qs=BLANK?loc=help&target=tips_on_building_formulas.htm#picklists_and_msps&section=Customizing', 'Help', 1024, 768, 'width=1024,height=768,resizable=yes,toolbar=yes,status=yes,scrollbars=yes,menubar=yes,directories=no,location=yes,dependant=no', false, false);">Tell me more</a> on line 1 col 1458

#### Inaccurate Line Numbers

Lines and columns reported in error messages have no relationship to the CREATE TABLE script you wrote.

#### Relationship Names can't be reused

If you create a 'relationship' column with name given name, you won't be able to reuse the name, even if you drop the table or column. This is because Salesforce doesn't _really_ drop the data until later, just in case you change your mind, so you can't reuse the relationship name until the column is completely deleted.

## ALTER TABLE

You can add OR MODIFY a column with:

```
ALTER TABLE ADD <columnName> <dataTypeDefinition>
```

and remove columns with:

```
ALTER TABLE DROP COLUMN <columnName>
```

## DROP TABLE

You can remove a table from Salesforce with:

```
DROP TABLE [ IF EXISTS ]
```


# Grant Permissions

The JDBC driver can set profile permissions, although these currently cannot be embedded within the deployment script and must be run directly against the destination environment. It does save a lot of pointing and clicking.

But first, a disclaimer:

_WARNING: DO NOT RUN THESE COMMANDS AGAINST A PRODUCTION INSTANCE OF SALESFORCE IF YOU HAVE MANY APEX UNIT TESTS._

This is because each GRANT or REVOKE command is currently an independent "deployment" from Salesforce's perspective, so if run against a Production instance then ALL TESTS will be run for EACH command. If you have a lot of Apex unit tests this may end up taking a very, very, long time. One day this functionality will be rolled into the Deployment Tool, if there is sufficient demand.

Permissions can be granted, or revoked at an object level:

```
GRANT OBJECT [create],[update],[delete],[read]
 ON <object> TO [<Profile Name> | * ]

 REVOKE OBJECT [create],[update],[delete],[read]
 ON <object> FROM [<Profile Name> | * ]
```

or at field level:

```
GRANT FIELD [ VISIBLE], [EDITABLE]
 ON <object>.[<field>|*] TO [<Profile Name> | * ]

REVOKE FIELD VISIBLE ON <object>.[<field>|*] FROM [<Profile Name> | * ]
```

_WARNING: The GRANT and REVOKE commands are likely to fail with a profile of "*" as it will probably attempt to modify a readonly profile, and so fail._


# JDBC Limitations

Although the JDBC Driver doesn't offer every piece of JDBC functionality, it does support less-obvious features such as batch updates, and updateable result sets.

The major unsupported feature is currently scroll cursors.

Anything that isn't supported will generally throw a SQLFeatureNotSupportedException, so it should be obvious when you have pushed the current version too far.