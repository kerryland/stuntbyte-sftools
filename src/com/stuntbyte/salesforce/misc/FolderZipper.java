package com.stuntbyte.salesforce.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FolderZipper {

    /**
     * Zip the srcFolder into the destFileZipFile. All the folder subtree of the src folder is added to the destZipFile
     * archive.
     * <p/>
     * TODO	 handle the usecase of srcFolder being en file.
     *
     * @param srcFolder   String, the path of the srcFolder
     * @param destZipFile String, the path of the destination zipFile. This file will be created or erased.
     */
    public void zipFolder(File srcFolder, String destZipFile) throws IOException {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;
        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);

        addFolderToZip("", srcFolder, zip);
        zip.flush();
        zip.close();
    }

    /**
     * add the srcFolder to the zip stream.
     *
     * @param folder    the directory to add
     * @param zip       ZipOutputStram, the stream to use to write the given file.
     */
    private void addFolderToZip(String root, File folder, ZipOutputStream zip) throws IOException {
        String fileList[] = folder.list();

        for (String fileName : fileList) {
            addToZip(root, new File(folder.getAbsolutePath() + "/" + fileName), zip);
        }
    }

    /**
     * Write the content of srcFile in a new ZipEntry, named path+srcFile, of the zip stream. The result
     * is that the srcFile will be in the path folder in the generated archive.
     *
     * @param path    String, the relatif path with the root archive.
     * @param source srcFile String, the absolute path of the file to add
     * @param zip     ZipOutputStram, the stream to use to write the given file.
     */
    private void addToZip(String path, File source, ZipOutputStream zip) throws IOException {
        String locn;

        if (path.equals("")) {
            locn = source.getName();
        } else {
            locn = path + "/" + source.getName();
        }

        if (source.isDirectory()) {
            addFolderToZip(locn, source, zip);
        } else {
//	 Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(source);
            zip.putNextEntry(new ZipEntry(locn));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
            in.close();
        }
    }

}
