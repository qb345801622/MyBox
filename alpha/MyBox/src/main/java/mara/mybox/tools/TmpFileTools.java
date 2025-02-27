package mara.mybox.tools;

import java.io.File;
import mara.mybox.value.AppVariables;

/**
 * @Author Mara
 * @CreateDate 2021-8-1
 * @License Apache License Version 2.0
 */
public class TmpFileTools {

    public static String getTempFileName() {
        return getTempFileName(AppVariables.MyBoxTempPath.getAbsolutePath());
    }

    public static String getTempFileName(String path) {
        return path + File.separator + DateTools.nowFileString() + "_" + IntTools.getRandomInt(100);
    }

    public static File getTempFile() {
        return getPathTempFile(AppVariables.MyBoxTempPath.getAbsolutePath());
    }

    public static File getTempFile(String suffix) {
        return getPathTempFile(AppVariables.MyBoxTempPath.getAbsolutePath(), suffix);
    }

    public static File getPathTempFile(String path) {
        File file = new File(getTempFileName(path));
        while (file.exists()) {
            file = new File(getTempFileName(path));
        }
        return file;
    }

    public static File getPathTempFile(String path, String suffix) {
        File file = new File(getTempFileName(path) + suffix);
        while (file.exists()) {
            file = new File(getTempFileName(path) + suffix);
        }
        return file;
    }

    public static File getTempDirectory() {
        return getPathTempDirectory(AppVariables.MyBoxTempPath.getAbsolutePath());
    }

    public static File getPathTempDirectory(String path) {
        File file = new File(getTempFileName(path) + File.separator);
        while (file.exists()) {
            file = new File(getTempFileName(path) + File.separator);
        }
        file.mkdirs();
        return file;
    }

}
