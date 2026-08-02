package modoru.main.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class IOUtil {

    private IOUtil() {}

    public static void moveFilesRecursively(File source, File destination) {
        File[] files = source.listFiles();
        assert files != null;

        for (File file : files) {
            String fileName = file.getName();
            if(file.isDirectory()) {
                moveFilesRecursively(file, new File(destination, fileName + "/"));
                continue;
            }

            if(!destination.exists()) destination.mkdirs();

            File outFile = new File(destination, fileName);
            try (FileInputStream fis = new FileInputStream(file); FileOutputStream fos = new FileOutputStream(outFile)) {
                fis.transferTo(fos);
                fos.flush();
            }
            catch (IOException exception) {
                throw new RuntimeException(exception);
            }

            file.delete();
        }
    }

    public static void unzip(File destination, File zip) {
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zip.getPath()));
            ZipEntry entry = zis.getNextEntry();

            while(entry != null) {
                File output = fileFromEntry(destination, entry);
                if (entry.isDirectory()) {
                    if (!output.isDirectory() && !output.mkdirs())
                        throw new IOException("Failed to create directory " + output);
                }
                else {
                    if (output.exists())
                        throw new FileAlreadyExistsException(output.getAbsolutePath());

                    File parent = output.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    FileOutputStream fos = new FileOutputStream(output);
                    zis.transferTo(fos);
                    fos.flush();
                    fos.close();
                }

                entry = zis.getNextEntry();
            }
        }
        catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    private static File fileFromEntry(File destination, ZipEntry entry) throws IOException {
        File destinationFile = new File(destination, entry.getName());
        if (!destinationFile.getCanonicalPath().startsWith(destination.getCanonicalPath() + File.separator))
            throw new IOException("Entry is outside of the target dir: " + entry.getName());

        return destinationFile;
    }

}
