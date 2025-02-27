package mara.mybox.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.List;
import mara.mybox.data.TextEditInformation;
import mara.mybox.dev.MyBoxLog;

/**
 * @Author Mara
 * @CreateDate 2021-8-1
 * @License Apache License Version 2.0
 */
public class TextFileTools {

    public static String readTexts(File file) {
        return readTexts(file, charset(file));
    }

    public static String readTexts(File file, Charset charset) {
        StringBuilder s = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new FileReader(file, charset))) {
            String line = reader.readLine();
            if (line != null) {
                s.append(line);
                while ((line = reader.readLine()) != null) {
                    s.append(System.lineSeparator()).append(line);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return s.toString();
    }

    public static File writeFile(File file, String data) {
        return writeFile(file, data, Charset.forName("utf-8"));
    }

    public static File writeFile(File file, String data, Charset charset) {
        if (file == null || data == null) {
            return null;
        }
        file.getParentFile().mkdirs();
        Charset fileCharset = charset != null ? charset : Charset.forName("utf-8");
        try ( BufferedWriter out = new BufferedWriter(new FileWriter(file, fileCharset, false))) {
            out.write(data);
            out.flush();
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return null;
        }
        return file;
    }

    public static File writeFile(String data) {
        return writeFile(TmpFileTools.getTempFile(), data);
    }

    public static Charset charset(File file) {
        try {
            TextEditInformation info = new TextEditInformation(file);
            if (TextTools.checkCharset(info)) {
                return info.getCharset();
            } else {
                return Charset.forName("utf-8");
            }
        } catch (Exception e) {
            return Charset.forName("utf-8");
        }
    }

    public static boolean isUTF8(File file) {
        Charset charset = charset(file);
        return charset.equals(Charset.forName("utf-8"));
    }

    public static boolean mergeTextFiles(List<File> files, File targetFile) {
        if (files == null || files.isEmpty() || targetFile == null) {
            return false;
        }
        targetFile.getParentFile().mkdirs();
        String line;
        try (final FileWriter writer = new FileWriter(targetFile, Charset.forName("utf-8"))) {
            for (File file : files) {
                try (final BufferedReader reader = new BufferedReader(new FileReader(file, charset(file)))) {
                    while ((line = reader.readLine()) != null) {
                        writer.write(line + System.lineSeparator());
                    }
                }
            }
            writer.flush();
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return false;
        }
        return true;
    }

}
