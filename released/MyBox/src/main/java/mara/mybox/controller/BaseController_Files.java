package mara.mybox.controller;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.RadioButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import mara.mybox.db.DerbyBase;
import mara.mybox.db.data.VisitHistory;
import mara.mybox.db.data.VisitHistoryTools;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.NodeStyleTools;
import mara.mybox.fxml.RecentVisitMenu;
import mara.mybox.tools.DateTools;
import mara.mybox.tools.FileNameTools;
import mara.mybox.value.AppVariables;
import static mara.mybox.value.AppVariables.MyBoxTempPath;
import mara.mybox.value.Languages;
import mara.mybox.value.UserConfig;

/**
 * @Author Mara
 * @CreateDate 2021-7-27
 * @License Apache License Version 2.0
 */
public abstract class BaseController_Files extends BaseController_Attributes {

    public boolean checkBeforeNextAction() {
        return true;
    }

    public void checkSourceFileInput() {
        String v = sourceFileInput.getText();
        if (v == null || v.isEmpty()) {
            sourceFileInput.setStyle(NodeStyleTools.badStyle);
            return;
        }
        final File file = new File(v);
        if (!file.exists()) {
            sourceFileInput.setStyle(NodeStyleTools.badStyle);
            return;
        }
        UserConfig.setString(baseName + "SourceFile", file.getAbsolutePath());
        sourceFileInput.setStyle(null);
        sourceFileChanged(file);
        if (parentController != null && parentController != this) {
            parentController.sourceFileChanged(file);
        }
        if (!file.isDirectory() && targetPrefixInput != null) {
            targetPrefixInput.setText(FileNameTools.getFilePrefix(file.getName()));
        }

    }

    public void checkTargetPathInput() {
        try {
            final File file = new File(targetPathInput.getText());
            if (!file.exists() || !file.isDirectory()) {
                targetPathInput.setStyle(NodeStyleTools.badStyle);
                return;
            }
            targetPath = file;
            targetPathInput.setStyle(null);
            UserConfig.setString(baseName + "TargetPath", file.getAbsolutePath());
        } catch (Exception e) {
        }
    }

    public void checkSourcetPathInput() {
        try {
            final File file = new File(sourcePathInput.getText());
            if (!file.exists() || !file.isDirectory()) {
                sourcePathInput.setStyle(NodeStyleTools.badStyle);
                return;
            }
            sourcePath = file;
            sourcePathInput.setStyle(null);
            UserConfig.setString(baseName + "SourcePath", file.getAbsolutePath());
        } catch (Exception e) {
        }
    }

    public void checkTargetFileInput() {
        try {
            String input = targetFileInput.getText();
            targetFile = new File(input);
            targetFileInput.setStyle(null);
            UserConfig.setString(baseName + "TargetFile", targetFile.getAbsolutePath());
        } catch (Exception e) {
            targetFile = null;
            targetFileInput.setStyle(NodeStyleTools.badStyle);
        }
    }

    public void checkTargetExistType() {
        if (isSettingValues) {
            return;
        }
        if (targetAppendInput != null) {
            targetAppendInput.setStyle(null);
        }

        RadioButton selected = (RadioButton) targetExistGroup.getSelectedToggle();
        if (selected.equals(targetReplaceRadio)) {
            targetExistType = BaseController.TargetExistType.Replace;

        } else if (selected.equals(targetRenameRadio)) {
            targetExistType = BaseController.TargetExistType.Rename;
            if (targetAppendInput != null) {
                if (targetAppendInput.getText() == null || targetAppendInput.getText().trim().isEmpty()) {
                    targetAppendInput.setStyle(NodeStyleTools.badStyle);
                } else {
                    UserConfig.setString(baseName + "TargetExistAppend", targetAppendInput.getText().trim());
                }
            }

        } else if (selected.equals(targetSkipRadio)) {
            targetExistType = BaseController.TargetExistType.Skip;
        }
        UserConfig.setString(baseName + "TargetExistType", selected.getText());
    }

    @FXML
    public void selectSourceFile() {
        try {
            if (!checkBeforeNextAction()) {
                return;
            }
            File file = mara.mybox.fxml.FxFileTools.selectFile(this);
            if (file == null) {
                return;
            }
            selectSourceFileDo(file);
        } catch (Exception e) {
//            MyBoxLog.error(e.toString());
        }
    }

    public void selectSourceFile(File file) {
        if (file == null || !file.exists()) {
            selectSourceFile();
            return;
        }
        if (!checkBeforeNextAction()) {
            return;
        }
        recordFileOpened(file);
        selectSourceFileDo(file);
    }

    public void selectSourceFileDo(File file) {
        if (sourceFileInput != null) {
            sourceFileInput.setText(file.getAbsolutePath());
        } else {
            sourceFileChanged(file);
        }
    }

    public void sourceFileChanged(final File file) {
        sourceFile = file;
    }

    public void recordFileOpened(String file) {
        if (file == null) {
            return;
        }
        recordFileOpened(new File(file));
    }

    public void recordFileOpened(File file) {
        if (file == null) {
            return;
        }
        recordFileOpened(file, SourcePathType, SourceFileType);
    }

    public void recordFileOpened(File file, int fileType) {
        recordFileOpened(file, fileType, fileType);
    }

    private void recordFileOpened(final File file, int pathType, int fileType) {
        if (file == null) {
            return;
        }
        try ( Connection conn = DerbyBase.getConnection()) {
            if (file.isDirectory()) {
                String path = file.getPath();
                UserConfig.setString(conn, baseName + "LastPath", path);
                UserConfig.setString(conn, baseName + "SourcePath", path);
                UserConfig.setString(conn, VisitHistoryTools.getPathKey(fileType), path);
                VisitHistoryTools.readPath(conn, pathType, path);
            } else {
                String path = file.getParent();
                String fname = file.getAbsolutePath();
                UserConfig.setString(conn, baseName + "LastPath", path);
                UserConfig.setString(conn, baseName + "SourceFile", fname);
                UserConfig.setString(conn, VisitHistoryTools.getPathKey(fileType), path);
                VisitHistoryTools.readPath(conn, pathType, path);
                VisitHistoryTools.readFile(conn, fileType, fname);
            }
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    public void recordFileWritten(String file) {
        recordFileWritten(new File(file));
    }

    public void recordFileWritten(final File file) {
        recordFileWritten(file, TargetPathType, TargetFileType);
    }

    public void recordFileWritten(final File file, int fileType) {
        recordFileWritten(file, fileType, fileType);
    }

    private void recordFileWritten(final File file, int TargetPathType, int TargetFileType) {
        if (file == null) {
            return;
        }
        try ( Connection conn = DerbyBase.getConnection()) {
            if (file.isDirectory()) {
                String path = file.getPath();
                UserConfig.setString(conn, baseName + "LastPath", path);
                UserConfig.setString(conn, baseName + "TargetPath", path);
                UserConfig.setString(conn, VisitHistoryTools.getPathKey(TargetPathType), path);
                VisitHistoryTools.writePath(conn, TargetPathType, path);
            } else {
                String path = file.getParent();
                String fname = file.getAbsolutePath();
                UserConfig.setString(conn, baseName + "LastPath", path);
                UserConfig.setString(conn, baseName + "TargetFile", fname);
                UserConfig.setString(conn, VisitHistoryTools.getPathKey(TargetPathType), path);
                VisitHistoryTools.writePath(conn, TargetPathType, path);
                VisitHistoryTools.writeFile(conn, TargetFileType, fname);
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
        }
    }

    public void recordFileAdded(String file) {
        recordFileOpened(new File(file));
    }

    public void recordFileAdded(final File file) {
        if (file == null) {
            return;
        }
        try ( Connection conn = DerbyBase.getConnection()) {
            if (file.isDirectory()) {
                String path = file.getPath();
                UserConfig.setString(conn, baseName + "SourcePath", path);
                UserConfig.setString(conn, baseName + "LastPath", path);
                UserConfig.setString(conn, VisitHistoryTools.getPathKey(SourcePathType), path);
                VisitHistoryTools.readPath(conn, SourcePathType, path);
            } else {
                String path = file.getParent();
                String fname = file.getAbsolutePath();
                UserConfig.setString(conn, baseName + "SourceFile", fname);
                UserConfig.setString(conn, baseName + "LastPath", path);
                UserConfig.setString(conn, VisitHistoryTools.getPathKey(SourcePathType), path);
                VisitHistoryTools.readPath(conn, SourcePathType, path);
                VisitHistoryTools.readFile(conn, AddFileType, fname);
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
        }
    }

    public void recordFileAdded(List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        try ( Connection conn = DerbyBase.getConnection()) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String path = file.getPath();
                    UserConfig.setString(conn, baseName + "SourcePath", path);
                    UserConfig.setString(conn, baseName + "LastPath", path);
                    UserConfig.setString(conn, VisitHistoryTools.getPathKey(SourcePathType), path);
                    VisitHistoryTools.readPath(conn, SourcePathType, path);
                } else {
                    String path = file.getParent();
                    String fname = file.getAbsolutePath();
                    UserConfig.setString(conn, baseName + "SourceFile", fname);
                    UserConfig.setString(conn, baseName + "LastPath", path);
                    UserConfig.setString(conn, VisitHistoryTools.getPathKey(SourcePathType), path);
                    VisitHistoryTools.readPath(conn, SourcePathType, path);
                    VisitHistoryTools.readFile(conn, AddFileType, fname);
                }
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
        }
    }

    @FXML
    public void selectTargetPath() {
        try {
            DirectoryChooser chooser = new DirectoryChooser();
            File path = UserConfig.getPath(baseName + "TargetPath");
            if (path != null) {
                chooser.setInitialDirectory(path);
            }
            File directory = chooser.showDialog(getMyStage());
            if (directory == null) {
                return;
            }
            selectTargetPath(directory);
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    public void selectTargetPath(File directory) {
        if (targetPathInput != null) {
            targetPathInput.setText(directory.getPath());
        }
        targetPathChanged();
        recordFileWritten(directory);
    }

    public void targetPathChanged() {

    }

    @FXML
    public void selectTargetFile() {
        File path = UserConfig.getPath(baseName + "TargetPath");
        selectTargetFileFromPath(path);
    }

    public void selectTargetFileFromPath(File path) {
        try {
            String name;
            if (sourceFile != null) {
                name = FileNameTools.getFilePrefix(sourceFile.getName());
            } else {
                name = DateTools.nowFileString();
            }
            final File file = chooseSaveFile(path, name, targetExtensionFilter);
            if (file == null) {
                return;
            }
            selectTargetFile(file);
        } catch (Exception e) {
//            MyBoxLog.error(e.toString());
        }
    }

    public void selectTargetFile(File file) {
        try {
            if (file == null) {
                return;
            }
            targetFile = file;
            if (targetFileInput != null) {
                targetFileInput.setText(targetFile.getAbsolutePath());
            }
            recordFileWritten(file);
        } catch (Exception e) {
//            MyBoxLog.error(e.toString());
        }
    }

    @FXML
    public void selectSourcePath() {
        try {
            DirectoryChooser chooser = new DirectoryChooser();
            File path = UserConfig.getPath(baseName + "SourcePath");
            if (path != null) {
                chooser.setInitialDirectory(path);
            }
            File directory = chooser.showDialog(getMyStage());
            if (directory == null) {
                return;
            }
            selectSourcePath(directory);
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    public void selectSourcePath(File directory) {
        if (sourcePathInput != null) {
            sourcePathInput.setText(directory.getPath());
        }
        recordFileOpened(directory);
    }

    public void openTarget(ActionEvent event) {

    }

    @FXML
    public void addFilesAction() {

    }

    public void addFile(File file) {

    }

    @FXML
    public void insertFilesAction() {

    }

    public void insertFile(File file) {

    }

    @FXML
    public void addDirectoryAction() {

    }

    public void addDirectory(File directory) {

    }

    @FXML
    public void insertDirectoryAction() {

    }

    public void insertDirectory(File directory) {

    }

    @FXML
    public void saveAsAction() {

    }

    @FXML
    public void popSourceFile(MouseEvent event) {
        if (AppVariables.fileRecentNumber <= 0) {
            return;
        }
        RecentVisitMenu menu = makeSourceFileRecentVisitMenu(event);
        if (menu != null) {
            menu.pop();
        }
    }

    public RecentVisitMenu makeSourceFileRecentVisitMenu(MouseEvent event) {
        RecentVisitMenu menu = new RecentVisitMenu(this, event) {

            @Override
            public void handleSelect() {
                selectSourceFile();
            }

            @Override
            public void handleFile(String fname) {
                File file = new File(fname);
                if (!file.exists()) {
                    selectSourceFile();
                    return;
                }
                selectSourceFile(file);
            }

        };
        return menu;
    }

    @FXML
    public void popFileAdd(MouseEvent event) {
        if (AppVariables.fileRecentNumber <= 0) {
            return;
        }
        new RecentVisitMenu(this, event) {
            @Override
            public List<VisitHistory> recentFiles() {
                return recentAddFiles();
            }

            @Override
            public List<VisitHistory> recentPaths() {
                int pathNumber = AppVariables.fileRecentNumber / 4 + 1;
                if (controller.getAddPathType() <= 0) {
                    controller.AddPathType = controller.SourcePathType;
                }
                return VisitHistoryTools.getRecentPath(controller.getAddPathType(), pathNumber);
            }

            @Override
            public void handleSelect() {
                addFilesAction();
            }

            @Override
            public void handleFile(String fname) {
                File file = new File(fname);
                if (!file.exists()) {
                    selectSourceFile();
                    return;
                }
                addFile(file);
            }

        }.pop();
    }

    @FXML
    public void popFileInsert(MouseEvent event) {
        if (AppVariables.fileRecentNumber <= 0) {
            return;
        }
        new RecentVisitMenu(this, event) {
            @Override
            public List<VisitHistory> recentFiles() {
                return recentAddFiles();
            }

            @Override
            public List<VisitHistory> recentPaths() {
                int pathNumber = AppVariables.fileRecentNumber / 4 + 1;
                if (controller.getAddPathType() <= 0) {
                    controller.AddPathType = controller.SourcePathType;
                }
                return VisitHistoryTools.getRecentPath(controller.getAddPathType(), pathNumber);
            }

            @Override
            public void handleSelect() {
                insertFilesAction();
            }

            @Override
            public void handleFile(String fname) {
                File file = new File(fname);
                if (!file.exists()) {
                    selectSourceFile();
                    return;
                }
                insertFile(file);
            }

        }.pop();
    }

    @FXML
    public void popDirectoryAdd(MouseEvent event) {
        if (AppVariables.fileRecentNumber <= 0) {
            return;
        }
        new RecentVisitMenu(this, event) {
            @Override
            public List<VisitHistory> recentFiles() {
                return null;
            }

            @Override
            public List<VisitHistory> recentPaths() {
                int pathNumber = AppVariables.fileRecentNumber / 4 + 1;
                if (controller.getAddPathType() <= 0) {
                    controller.AddPathType = controller.SourcePathType;
                }
                return VisitHistoryTools.getRecentPath(controller.getAddPathType(), pathNumber);
            }

            @Override
            public void handleSelect() {
                addDirectoryAction();
            }

            @Override
            public void handleFile(String fname) {

            }

            @Override
            public void handlePath(String fname) {
                File file = new File(fname);
                if (!file.exists()) {
                    handleSelect();
                    return;
                }
                addDirectory(file);
            }

        }.pop();
    }

    @FXML
    public void popDirectoryInsert(MouseEvent event) {
        if (AppVariables.fileRecentNumber <= 0) {
            return;
        }
        new RecentVisitMenu(this, event) {
            @Override
            public List<VisitHistory> recentFiles() {
                return null;
            }

            @Override
            public List<VisitHistory> recentPaths() {
                int pathNumber = AppVariables.fileRecentNumber / 4 + 1;
                if (controller.getAddPathType() <= 0) {
                    controller.AddPathType = controller.SourcePathType;
                }
                return VisitHistoryTools.getRecentPath(controller.getAddPathType(), pathNumber);
            }

            @Override
            public void handleSelect() {
                insertDirectoryAction();
            }

            @Override
            public void handleFile(String fname) {

            }

            @Override
            public void handlePath(String fname) {
                File file = new File(fname);
                if (!file.exists()) {
                    handleSelect();
                    return;
                }
                insertDirectory(file);
            }

        }.pop();
    }

    @FXML
    public void popSourcePath(MouseEvent event) {
        if (AppVariables.fileRecentNumber <= 0) {
            return;
        }
        new RecentVisitMenu(this, event) {
            @Override
            public List<VisitHistory> recentFiles() {
                return null;
            }

            @Override
            public List<VisitHistory> recentPaths() {
                return recentSourcePaths();
            }

            @Override
            public void handleSelect() {
                selectSourcePath();
            }

            @Override
            public void handleFile(String fname) {

            }

            @Override
            public void handlePath(String fname) {
                File file = new File(fname);
                if (!file.exists()) {
                    handleSelect();
                    return;
                }
                selectSourcePath(file);
            }

        }.pop();
    }

    @FXML
    public void popTargetPath(MouseEvent event) {
        if (AppVariables.fileRecentNumber <= 0) {
            return;
        }
        new RecentVisitMenu(this, event) {
            @Override
            public List<VisitHistory> recentFiles() {
                return null;
            }

            @Override
            public List<VisitHistory> recentPaths() {
                return recentTargetPaths();
            }

            @Override
            public void handleSelect() {
                selectTargetPath();
            }

            @Override
            public void handleFile(String fname) {

            }

            @Override
            public void handlePath(String fname) {
                File file = new File(fname);
                if (!file.exists()) {
                    handleSelect();
                    return;
                }
                selectTargetPath(file);
            }

        }.pop();
    }

    @FXML
    public void popTargetFile(MouseEvent event) {
        if (AppVariables.fileRecentNumber <= 0) {
            return;
        }
        new RecentVisitMenu(this, event) {
            @Override
            public List<VisitHistory> recentFiles() {
                return null;
            }

            @Override
            public List<VisitHistory> recentPaths() {
                return recentTargetPaths();
            }

            @Override
            public void handleSelect() {
                selectTargetFile();
            }

            @Override
            public void handleFile(String fname) {
            }

            @Override
            public void handlePath(String fname) {
                File file = new File(fname);
                if (!file.exists()) {
                    handleSelect();
                    return;
                }
                selectTargetFileFromPath(file);
            }

        }.pop();
    }

    @FXML
    public void popSaveAs(MouseEvent event) { //
        if (AppVariables.fileRecentNumber <= 0) {
            return;
        }
        new RecentVisitMenu(this, event) {
            @Override
            public List<VisitHistory> recentFiles() {
                return null;
            }

            @Override
            public List<VisitHistory> recentPaths() {
                return recentTargetPaths();
            }

            @Override
            public void handleSelect() {
                saveAsAction();
            }

            @Override
            public void handleFile(String fname) {

            }

            @Override
            public void handlePath(String fname) {
                handleTargetPath(fname);
            }

        }.pop();
    }

    public String defaultTargetName(String prefix) {
        String defaultName = prefix != null ? prefix : "";
        if (sourceFile != null) {
            defaultName += FileNameTools.prefixFilter(sourceFile) + "_";
        }
        defaultName += DateTools.nowFileString();
        return defaultName;
    }

    public File defaultTargetPath(int type) {
        File savedPath = VisitHistoryTools.getSavedPath(type);
        String defaultPathName = UserConfig.getString(baseName + "TargetPath", savedPath != null ? savedPath.getAbsolutePath() : null);
        if (defaultPathName != null) {
            return new File(defaultPathName);
        } else {
            return MyBoxTempPath;
        }
    }

    public List<FileChooser.ExtensionFilter> defaultFilter(int type) {
        return VisitHistoryTools.getExtensionFilter(type);
    }

    public File chooseSaveFile() {
        return chooseSaveFile(defaultTargetPath(TargetPathType), defaultTargetName(""), targetExtensionFilter);
    }

    public File chooseSaveFile(String defaultName) {
        return chooseSaveFile(defaultTargetPath(TargetPathType), defaultName, targetExtensionFilter);
    }

    public File chooseSaveFile(int type) {
        return chooseSaveFile(defaultTargetPath(type), defaultTargetName(""), defaultFilter(type));
    }

    public File chooseSaveFile(int type, String defaultName) {
        return chooseSaveFile(defaultTargetPath(type), defaultName, defaultFilter(type));
    }

    public File chooseSaveFile(File defaultPath, String defaultName, List<FileChooser.ExtensionFilter> filters) {
        return chooseSaveFile(null, defaultPath, defaultName, filters);
    }

    public File chooseSaveFile(String title, File defaultPath, String defaultName, List<FileChooser.ExtensionFilter> filters) {
        try {
            FileChooser fileChooser = new FileChooser();
            if (title != null) {
                fileChooser.setTitle(title);
            }
            if (defaultPath == null || !defaultPath.exists()) {
                defaultPath = MyBoxTempPath;
            }
            fileChooser.setInitialDirectory(defaultPath);
            String suffix = null, prefix = null;
            if (defaultName != null && !defaultName.isBlank()) {
                suffix = FileNameTools.getFileSuffix(defaultName);
                prefix = FileNameTools.getFilePrefix(defaultName);
            }
            if (prefix == null || prefix.isBlank()) {
                prefix = DateTools.nowFileString();
            }
            if (filters != null) {
                if (suffix == null || suffix.isBlank()) {
                    suffix = FileNameTools.getFileSuffix(filters.get(0).getExtensions().get(0));
                }
                fileChooser.getExtensionFilters().addAll(filters);
            }
            if ("*".equals(suffix)) {
                suffix = null;
            }
            String name = prefix;
            if (suffix != null && !suffix.isBlank()) {
                name += "." + suffix;
            }
            fileChooser.setInitialFileName(FileNameTools.filenameFilter(name));

            File file = fileChooser.showSaveDialog(getMyStage());
            if (file == null) {
                return null;
            }

            // https://stackoverflow.com/questions/20637865/javafx-2-2-get-selected-file-extension
            // This is a pretty annoying thing in JavaFX - they will automatically append the extension on Windows, but not on Linux or Mac.
            if (FileNameTools.getFileSuffix(file.getName()).isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                if (myStage != null) {
                    alert.setTitle(myStage.getTitle());
                }
                alert.setHeaderText(null);
                alert.setContentText(Languages.message("SureNoFileExtension"));
                ButtonType buttonSure = new ButtonType(Languages.message("Sure"));
                ButtonType buttonNo = new ButtonType(Languages.message("No"));
                ButtonType buttonCancel = new ButtonType(Languages.message("Cancel"));
                alert.getButtonTypes().setAll(buttonCancel, buttonNo, buttonSure);
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.setAlwaysOnTop(true);
                stage.toFront();
                stage.sizeToScene();
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() != buttonSure) {
                    if (result.get() == buttonNo) {
                        return chooseSaveFile(title, defaultPath, defaultName, filters);
                    } else {
                        return null;
                    }
                }
            }
            return file;

        } catch (Exception e) {
            return null;
        }

    }

    public File makeTargetFile(File sourceFile, File targetPath) {
        if (sourceFile.isDirectory()) {
            return makeTargetFile(sourceFile.getName(), "", targetPath);
        } else {
            return makeTargetFile(sourceFile.getName(), targetPath);
        }
    }

    public File makeTargetFile(String fileName, File targetPath) {
        try {
            if (fileName == null || targetPath == null) {
                return null;
            }
            String namePrefix = FileNameTools.namePrefix(fileName);
            String nameSuffix;
            if (targetFileSuffix != null) {
                nameSuffix = "." + targetFileSuffix;
            } else {
                nameSuffix = FileNameTools.getFileSuffix(fileName);
                if (nameSuffix != null && !nameSuffix.isEmpty()) {
                    nameSuffix = "." + nameSuffix;
                } else {
                    nameSuffix = "";
                }
            }
            return makeTargetFile(namePrefix, nameSuffix, targetPath);
        } catch (Exception e) {
            return null;
        }
    }

    public File makeTargetFile(String namePrefix, String nameSuffix, File targetPath) {
        try {
            String targetPrefix = targetPath.getAbsolutePath() + File.separator + FileNameTools.filenameFilter(namePrefix);
            String targetSuffix = FileNameTools.filenameFilter(nameSuffix);
            File target = new File(targetPrefix + targetSuffix);
            if (target.exists()) {
                if (targetExistType == TargetExistType.Skip) {
                    target = null;
                } else if (targetExistType == TargetExistType.Rename) {
                    if (targetAppendInput != null) {
                        targetNameAppend = targetAppendInput.getText().trim();
                    }
                    if (targetNameAppend == null || targetNameAppend.isEmpty()) {
                        targetNameAppend = "_m";
                    }
                    while (true) {
                        targetPrefix = targetPrefix + targetNameAppend;
                        target = new File(targetPrefix + targetSuffix);
                        if (!target.exists()) {
                            break;
                        }
                    }
                }
            }
            if (target != null) {
                target.getParentFile().mkdirs();
            }
            return target;
        } catch (Exception e) {
            MyBoxLog.debug(e.toString());
            return null;
        }
    }

}
