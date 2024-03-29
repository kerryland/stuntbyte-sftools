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
package com.stuntbyte.salesforce.misc;

import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

/**
 */
public class FileUtil {

    public static String loadTextFile(File file) throws IOException {
        StringBuilder metaData = new StringBuilder();
        {
            LineNumberReader read = new LineNumberReader(new FileReader(file));
            String line = read.readLine();
            while (line != null) {
                metaData.append(line).append('\n');
                line = read.readLine();
            }
            read.close();
        }
        return metaData.toString().trim();
    }

    public static File createTempDirectory(String namePart) {
        File f = new File(System.getProperty("java.io.tmpdir"),
                "SFDC-" + namePart + "-" + System.currentTimeMillis());  // TODO: This must be unique
        f.mkdir();
        f.deleteOnExit();
        return f;
    }


    // http://www.salesforce.com/us/developer/docs/api_meta/Content/file_based.htm#component_folders_title
    public static String determineDirectoryName(String typeName) {
        if (typeName.equalsIgnoreCase("ActionOverride")) return "objects";
        if (typeName.equalsIgnoreCase("ApexClass")) return "classes";
        if (typeName.equalsIgnoreCase("ArticleType")) return "objects";
        if (typeName.equalsIgnoreCase("ApexComponent")) return "components";
        if (typeName.equalsIgnoreCase("ApexPage")) return "pages";
        if (typeName.equalsIgnoreCase("ApexTrigger")) return "triggers";
        if (typeName.equalsIgnoreCase("BusinessProcess")) return "objects";
        if (typeName.equalsIgnoreCase("CustomApplication")) return "applications";
        if (typeName.equalsIgnoreCase("CustomField")) return "objects";
        if (typeName.equalsIgnoreCase("CustomLabel")) return "labels";
        if (typeName.equalsIgnoreCase("CustomObject")) return "objects";
        if (typeName.equalsIgnoreCase("CustomObjectTranslation")) return "objectTranslations";
        if (typeName.equalsIgnoreCase("CustomPageWebLink")) return "weblinks";
        if (typeName.equalsIgnoreCase("CustomSite")) return "sites";
        if (typeName.equalsIgnoreCase("CustomTab")) return "tabs";
        if (typeName.equalsIgnoreCase("Dashboard")) return "dashboards";
        if (typeName.equalsIgnoreCase("DataCategoryGroup")) return "datacategorygroups";
        if (typeName.equalsIgnoreCase("Document")) return "document";
        if (typeName.equalsIgnoreCase("EmailTemplate")) return "email";
        if (typeName.equalsIgnoreCase("EntitlementTemplate")) return "entitlementTemplates";
        if (typeName.equalsIgnoreCase("FieldSet")) return "objects";
        if (typeName.equalsIgnoreCase("HomePageComponent")) return "homePageComponents";
        if (typeName.equalsIgnoreCase("HomePageLayout")) return "homePageLayouts";
        if (typeName.equalsIgnoreCase("Layout")) return "layouts";
        if (typeName.equalsIgnoreCase("Letterhead")) return "letterhead";
        if (typeName.equalsIgnoreCase("ListView")) return "objects";
        if (typeName.equalsIgnoreCase("NamedFilter")) return "objects";
        if (typeName.equalsIgnoreCase("PermissionSet")) return "permissionsets";
        if (typeName.equalsIgnoreCase("Portal")) return "portals";
        if (typeName.equalsIgnoreCase("Profile")) return "profiles";
        if (typeName.equalsIgnoreCase("RecordType")) return "objects";
        if (typeName.equalsIgnoreCase("RemoteSiteSetting")) return "remoteSiteSettings";
        if (typeName.equalsIgnoreCase("Report")) return "reports";
        if (typeName.equalsIgnoreCase("ReportType")) return "reportTypes";
        if (typeName.equalsIgnoreCase("Scontrol")) return "scontrols";
        if (typeName.equalsIgnoreCase("SharingReason")) return "objects";
        if (typeName.equalsIgnoreCase("SharingRecalculation")) return "objects";
        if (typeName.equalsIgnoreCase("StaticResource")) return "staticresources";
        if (typeName.equalsIgnoreCase("Translations")) return "translations";
        if (typeName.equalsIgnoreCase("ValidationRule")) return "objects";
        if (typeName.equalsIgnoreCase("Weblink")) return "objects";
        if (typeName.toUpperCase().startsWith("WORKFLOW")) return "workflows";

        System.err.println("WARNING: Unknown type " + typeName + " -- guessing at 'objects'");

        return "objects";
    }

    public static String determineFileSuffix(String typeName) throws Exception {
        String directoryName = determineDirectoryName(typeName);
        String suffix;

        if (directoryName.equals("classes")) {
            suffix = "cls";

        } else if (directoryName.equals("staticresources")) {
            suffix = "resource";

        } else if (directoryName.equals("remoteSiteSettings")) {
            suffix = "remoteSite";

        } else if (directoryName.equals("email") ||
                directoryName.equals("document") ||
                directoryName.equals("letterhead")) {
            suffix = directoryName; // These types have singular directory names

        } else {
            // These types have plural directory names
            // Chop off the "s" from the end of the directory name
            suffix = directoryName.substring(0, directoryName.length() - 1);
        }
        return suffix;
    }


    public static void delete(File file) throws IOException {

        if (file.isDirectory()) {

            //directory is empty, then delete it
            if (file.list().length == 0) {
                actuallyDelete(file);
//                System.out.println("Directory is deleted : "
//                        + file.getAbsolutePath());

            } else {

                //list all the directory contents
                String files[] = file.list();

                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp);

                    //recursive delete
                    delete(fileDelete);
                }

                //check the directory again, if empty then delete it
                if (file.list().length == 0) {
                    actuallyDelete(file);
//                    System.out.println("Directory is deleted : "
//                            + file.getAbsolutePath());
                }
            }

        } else {
            //if file, then delete it
            actuallyDelete(file);
//            System.out.println("File is deleted : " + file.getAbsolutePath());
        }
    }

    private static void actuallyDelete(File file) throws IOException {
        if (!file.delete()) {
            throw new IOException("Unable to delete " + file.getAbsolutePath());
        }
    }

    public static String docToXml(Document doc) throws TransformerException {
        StringWriter sw = new StringWriter();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(sw);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(source, result);
        return sw.toString();
    }
}
