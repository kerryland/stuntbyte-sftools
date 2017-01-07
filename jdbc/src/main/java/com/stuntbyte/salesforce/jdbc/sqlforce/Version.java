package com.stuntbyte.salesforce.jdbc.sqlforce;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Get version numbers from the manifest
 */
public class Version {
    public static int majorVersion;
    public static int minorVersion;
    public static int sfdcVersion = 36; // Default if we can't access the manifest (eg: tests)

    static {
        InputStream stream = null;
        try {
            Class clazz = Version.class;
            String className = clazz.getSimpleName() + ".class";
            String classPath = clazz.getResource(className).toString();
            if (classPath.startsWith("jar")) {
                String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +
                        "/META-INF/MANIFEST.MF";
                stream = new URL(manifestPath).openStream();
                Manifest manifest = new Manifest(stream);
                Attributes attr = manifest.getMainAttributes();
                String version = attr.getValue("Implementation-Version");

                String[] versions = version.split("\\.");
                majorVersion = Integer.parseInt(versions[0]);
                minorVersion = Integer.parseInt(versions[1]);
                sfdcVersion = new BigDecimal(attr.getValue("Salesforce-api").split("\\.")[0]).intValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
