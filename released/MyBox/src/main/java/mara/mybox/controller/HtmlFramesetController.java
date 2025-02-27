package mara.mybox.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import mara.mybox.db.data.VisitHistory;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.tools.HtmlWriteTools;

import mara.mybox.value.AppVariables;
import static mara.mybox.value.Languages.message;
import mara.mybox.value.Languages;

/**
 * @Author Mara
 * @CreateDate 2020-10-27
 * @License Apache License Version 2.0
 */
public class HtmlFramesetController extends BaseBatchFileController {

    protected List<File> validFiles;

    @FXML
    protected ControlFileSelecter targetFileController;

    public HtmlFramesetController() {
        baseTitle = Languages.message("HtmlFrameset");
    }

    @Override
    public void initValues() {
        try {
            super.initValues();
            targetFileInput = targetFileController.fileInput;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @Override
    public void setFileType() {
        setFileType(VisitHistory.FileType.All, VisitHistory.FileType.Html);
    }

    @Override
    public void initControls() {
        try {
            targetFileController.label(Languages.message("TargetFile"))
                    .isDirectory(false).isSource(false).mustExist(false).permitNull(false)
                    .defaultValue("_" + Languages.message("Merge"))
                    .name(baseName + "TargetFile", false).type(VisitHistory.FileType.Html);

            super.initControls();

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @Override
    public boolean makeMoreParameters() {
        try {
            targetFile = targetFileController.file;
            if (targetFile == null) {
                return false;
            }
            validFiles = new ArrayList<>();
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return false;
        }
        return super.makeMoreParameters();
    }

    @Override
    public boolean matchType(File file) {
        return true;
    }

    @Override
    public String handleFile(File srcFile, File targetPath) {
        try {
            validFiles.add(srcFile);
            return Languages.message("Successful");
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return Languages.message("Failed");
        }
    }

    @Override
    public void afterHandleFiles() {
        if (HtmlWriteTools.generateFrameset(validFiles, targetFile)) {
            targetFileGenerated(targetFile);
        } else {
            updateLogs(Languages.message("Failed"), true, true);
        }
    }

    @Override
    public void viewTarget(File file) {
        if (file == null) {
            return;
        }
        browseURI(file.toURI());
    }

}
