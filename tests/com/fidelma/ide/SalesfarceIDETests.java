package com.fidelma.ide;

import com.fidelma.salesforce.ide.SalesfarceIDE;
import com.fidelma.salesforce.misc.TestHelper;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.datatransfer.FlavorEvent;
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
    private static String debugFile;
    private static String crcFile;


    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        Properties prop = new Properties();
        prop.load(new FileReader("build.properties"));
        prop.load(new FileReader("local.build.properties"));

        srcDirectory = prop.getProperty("src.dir");
        debugFile = prop.getProperty("debug.file");
        crcFile = prop.getProperty("crc.file");
    }


    @Test
    public void testClassEditing() throws Exception {
        // Create a class
        String fileName = srcDirectory + "/classes/IdeTestClass.cls";
        String simpleClass =
                "public class IdeTestClass {\n" +
                        "public void Hello() {\n" +
                        "}\n" +
                        "}";

        writeCode(simpleClass, fileName);

        // Upload the class
        SalesfarceIDE.main(new String[] {fileName});

        // Edit a class
        simpleClass =
                "public class IdeTestClass {\n" +
                        "public void HelloAgain() {\n" +
                        "}\n" +
                        "}";
        writeCode(simpleClass, fileName);

        // Upload a class
        SalesfarceIDE.main(new String[] {fileName});

        // Delete the file locally
        Assert.assertTrue(new File(fileName).delete());

        // See if we can download it
        SalesfarceIDE.main(new String[] {"-download", fileName});
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


    @Test
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

        // Upload the class
        SalesfarceIDE.main(new String[]{fileName});

        // Edit a class
        simplePage = "<apex:page>CHANGED</apex:page>";
        writeCode(simplePage, fileName);

        // Upload a class
        SalesfarceIDE.main(new String[] {fileName});

        // Delete the file locally
        Assert.assertTrue(new File(fileName).delete());

        // See if we can download it
        SalesfarceIDE.main(new String[] {"-download", fileName});
        Assert.assertTrue(new File(fileName).exists());


        // Change a class -- and delete CRC file

        // Try to upload a class (should fail)

        // Force an upload of the class anyway

        // Change a class -- and delete CRC file

        // Try to download the class (should fail)

        // Force a download of the class

        // Do it all again for a page
    }

    private String writeCode(String simpleClass, String fileName) throws IOException {
        FileWriter fw = new FileWriter(fileName);
        fw.write(simpleClass);
        fw.close();
        return fileName;
    }
}
