package com.stuntbyte.ide;

import com.stuntbyte.salesforce.ide.SalesfarceIDE;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * I wrote SalesfarceIDE as a ugly hack and didn't
 * care enough to write any tests. BIG MISTAKE.
 * This is me trying to right that wrong...
 */
public class SalesfarceIDETests {

    private static String srcDirectory;
//    private static String debugFile;
//    private static String crcFile;


    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        Properties prop = new Properties();
//        prop.load(new FileReader("build.properties"));
//        prop.load(new FileReader("local.build.properties"));
        prop.load(new FileReader("ide.properties"));

        srcDirectory = prop.getProperty("src.dir");

//        new File(srcDirectory).mkdir();
        // Assert.assertTrue(new File(srcDirectory).mkdir());
//        debugFile = prop.getProperty("debug.file");
//        crcFile = prop.getProperty("crc.file");
    }


    @Test
    public void testClassEditing() throws Exception {

        // Create metadata for class
        String fileName = srcDirectory + "/classes/IdeTestClass.cls-meta.xml";
        String metadata =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<ApexClass xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n" +
                        "    <apiVersion>20.0</apiVersion>\n" +
                        "    <status>Active</status>\n" +
                        "</ApexClass>";

        writeCode(metadata, fileName);

        // Create a class
        fileName = srcDirectory + "/classes/IdeTestClass.cls";
        String simpleClass =
                "public class IdeTestClass {\n" +
                        "public void Hello() {\n" +
                        "}\n" +
                        "}";

        writeCode(simpleClass, fileName);


        // Upload the class
        System.out.println("Trying to upload");
        SalesfarceIDE.main(new String[] {"ide.properties", "tags", "-force", fileName});
        System.out.println("Trying to upload... done");

        // Edit a class
        simpleClass =
                "public class IdeTestClass {\n" +
                        "public void HelloAgain() {\n" +
                        "}\n" +
                        "}";
        writeCode(simpleClass, fileName);

        // Upload a class
        SalesfarceIDE.main(new String[]{"ide.properties", "tags", "-compile", fileName});

        // Delete the file locally
        Assert.assertTrue(new File(fileName).delete());

        // See if we can download it
        SalesfarceIDE.main(new String[] {"ide.properties", "tags", "-download", fileName});
        Assert.assertTrue(new File(fileName).exists());


        // Change a class -- and delete CRC file

        // Try to upload a class (should fail)

        // Force an upload of the class anyway

        // Change a class -- and delete CRC file

        // Try to download the class (should fail)

        // Force a download of the class

        // Do it all again for a page
    }

    @Test
    public void testExistingPageEditing() throws Exception {
        String fileName = srcDirectory + "/pages/ForgotPassword.page";
        testPage(fileName);
    }


    // WE EXPECT THIS TO FAIL AT THE MOMENT BECAUSE WE ARE NOT CREATING METADATA @Test
    // TODO: https://fidelma.repositoryhosting.com/trac/fidelma_farce-ide/ticket/55
    public void testNewPageEditing() throws Exception {
        // Create a class
        String fileName = srcDirectory + "/pages/IdeTestPage.page";
        String simplePage =
                "<apex:page></apex:page>";

        writeCode(simplePage, fileName);
        testPage(fileName);


    }

    private void testPage(String fileName) throws Exception {
        String simplePage;

        // First download it
        SalesfarceIDE.main(new String[] {"ide.properties", "tags", "-download", fileName});
        Assert.assertTrue(new File(fileName).exists());

        // Edit a class
        simplePage = "<apex:page>CHANGED</apex:page>";
        writeCode(simplePage, fileName);

        // Upload a class
        SalesfarceIDE.main(new String[] {"ide.properties", "tags",  "-compile", fileName});

        // Delete the file locally
        Assert.assertTrue(new File(fileName).delete());

        // See if we can download it after deleting it from the file system
        SalesfarceIDE.main(new String[] {"ide.properties", "tags", "-download", fileName});
        Assert.assertTrue(new File(fileName).exists());

        // TODO: Look at the content! File existing is a nice start, but really not enough


        // Change a class -- and delete CRC file

        // Try to upload a class (should fail)

        // Force an upload of the class anyway

        // Change a class -- and delete CRC file

        // Try to download the class (should fail)

        // Force a download of the class

        // Do it all again for a page
    }

    private String writeCode(String simpleClass, String fileName) throws IOException {
        File f = new File(fileName);
        f.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(f);
        fw.write(simpleClass);
        fw.close();
        return fileName;
    }
}
