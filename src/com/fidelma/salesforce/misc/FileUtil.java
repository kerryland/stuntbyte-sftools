package com.fidelma.salesforce.misc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

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
                "SF-" + namePart + "-" + System.currentTimeMillis());  // TODO: This must be unique
        f.mkdir();
        f.deleteOnExit();
        return f;
    }
}
