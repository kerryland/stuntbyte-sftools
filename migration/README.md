# Migration and Export

These tools are not completely plug-and-play, and probably will require more hacking to get right. They have not
had the same attention as other components.

## Export
Given a JDBC driver, it's relatively easy to export your Salesforce data to another relationship database. Either for
reporting purposes, or perhaps permanently.

Take a look at the ['Exporter'] (src/main/java/com/stuntbyte/salesforce/database/migration/Exporter.java) class for more inspiration, although a purpose-built exporting tool like that in SQL-Workbench might be a better option.
This code currently exports to the H2 database, but that should be trivial to change.

## Migration
When you have a deployment tool *and* a JDBC driver it should be fairly easy to make clones of Salesforce environments.
This is especially useful when someone has been setting up a test environment and then wants to turn it into a
Production Environment. Take a look at the ['Migrator'] (src/main/java/com/stuntbyte/salesforce/database/migration/Migrator.java) class.

## Grails
Given a Hibernate profile, it should be fairly easy to generate a CRUD app using Grails. This was a little test-app
I wrote just to see if it worked. It did :-)