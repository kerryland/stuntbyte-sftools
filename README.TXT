
https://bitbucket.org/repo/mine
bhttps://sites.google.com/a/fidelma.com/main/

WHO:
----
Some guy in New Zealand who got fed up.


WHAT:
-----
"Farce IDE" is an alternative to Salesforce's "Force IDE". The difference is that
"Farce IDE" supports:

- Keyboard navigation through classes and methods.
- Decent error messages, and an IDE that takes you to the offending line
  of the correct class.
- A debug log file that you can search through.
- Confidence that you know when a file has been saved to the server.
  No more waiting for the "successful save flicker" before you run a test.


WHY:
----
"The Force.com IDE is a powerful client application for creating, modifying,
 testing and deploying Force.com applications"

(http://wiki.developerforce.com/index.php/Force.com_IDE)

Snigger.

The Force.com IDE is a stinking pile of poo that Salesforce should be
ashamed of.


WHEN:
-----
Farce.IDE was thrown together one Sunday in early February 2011.


HOW: (ie: INSTALLATION INSTRUCTIONS)
----
Via a combination of the VIM editor, CTAGS, and Salesforce's "Apex" API.


COMPILATION:

- Download jars:

1. Browse to http://code.google.com/p/sfdc-wsc/
    Click the Downloads tab.
    Click the wsc-22.jar link and save the file to lib directory.

    Download partner, metadata and apex wsdls

    java -classpath lib/wsc-22.jar com.sforce.ws.tools.wsdlc wsdl/partner.wsdl lib/partner.jar
    java -classpath lib/wsc-22.jar com.sforce.ws.tools.wsdlc wsdl/metadata.wsdl lib/metadata.jar
    java -classpath lib/wsc-22.jar com.sforce.ws.tools.wsdlc wsdl/apex.wsdl lib/apex.jar


2. http://mvel.codehaus.org/Downloading+MVEL into lib

3. Hibernate 

4. slf4j (for use with Hibernate)

- ... and jars that are just for testing

5. JUNIT: https://github.com/KentBeck/junit/downloads

6. H2: http://www.h2database.com/html/download.html

  


mvn install:install-file -Dfile=c:/apps/sql-workbench/drivers/sfdc-kjs-driver-2.jar -DgroupId=com.stuntbyte -DartifactId=farce-base -Dversion=1.1 -Dpackaging=jar -DgeneratePom=true
