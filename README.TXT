I was inspired by two projects, and used small amounts of code from both:

Lexical parsing code from Gregory Smith (gsmithfarmer@gmail.com)'s "SQLForce" code
via https://github.com/abhidotnet/sqlforce (repurposed as com.stuntbyte.salesforce.jdbc.sqlforce).
Released under the Eclipse Public Licence 1.0. You might like to look at http://www.capstorm.com for more of his tools.

Salesforce Data type mapping from Keith Clarke (keith.clarke.claimvantage@gmail.com)'s "Force Metadata" code
via https://code.google.com/archive/p/force-metadata-jdbc-driver/ (repurposed as com.stuntbyte.salesforce.jdbc.metaforce).
Released under the "New BSD Licence" (aka BSD 3)

TODO:

Update to API 31 and fix ExporterTest
Migrate nanoc to Markdown

https://bitbucket.org/repo/mine
http://kerryland.myjetbrains.com/youtrack/issues#

https://sites.google.com/a/fidelma.com/main/

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

sudo apt-get install exuberant-ctags

edit ide.properties

1. set $STUNTBYTE_JAR
2. set $STUNTBYTE_PROJ (defines the project you want to work with)
3. vim -S vimrc        (TODO: Rename!)





Sublime:
--------

https://github.com/SublimeText/CTags

cd  ~/.config/sublime-text-2/Packages/
git clone https://github.com/SublimeText/CTags CTags

ctrl-alt ]
ctrl-alt [

http://blog.stuartherbert.com/php/2012/02/28/setting-up-sublime-text-2-for-php-development/
http://www.rockettheme.com/magazine/1319-using-sublime-text-2-for-development
http://docs.sublimetext.info/en/latest/reference/build_systems.html

{
	"cmd": [
      "java",
      "-cp",
      "/media/Local_Disk/work/kerry-dev/farce-ide/stuntbyte-sfdc-1.3.jar",
      "com.stuntbyte.salesforce.ide.SalesfarceIDE",
      "$file"],

	"file_regex": "^(...*?)>([0-9]*):?([0-9]*)(.*)",
    // "working_dir": "/media/Local_Disk/work/kerry-dev/farce-ide/editors"
	 "working_dir": "${project_path:${folder}}"
	// "selector": [ "source.cls", "source.trigger " ],

	// "working_Dir" : "$project_path"

}

[
{
"keys": ["ctrl+alt+x"],
            "command": "set_setting",
            "args":
            {
                "setting": "color_scheme",
                "value": "Packages/Color Scheme - Default/Amy.tmTheme"
            }

}
]


F7 compiles

{
	"build_systems":
	[
		{
			"name": "my build",
			"cmd": ["java", "-cp", "classes:/media/Local_Disk/work/kerry-dev/farce-ide/stuntbyte-sfdc-1.3.jar:.", "com.stuntbyte.salesforce.ide.SalesfarceIDE", "$file"],
			"file_regex": "^[ ]*File \"(...*?)\", line ([0-9]*)"
		}
	]

}


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




  


mvn install:install-file -Dfile=stuntbyte-sfdc-1.4.jar -DgroupId=com.stuntbyte -DartifactId=farce-base -Dversion=1.4 -Dpackaging=jar -DgeneratePom=true
