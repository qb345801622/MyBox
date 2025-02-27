package mara.mybox.controller;

import java.io.File;
import java.util.Date;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import mara.mybox.data.FileInformation;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.NodeTools;
import mara.mybox.fxml.cell.TableFileSizeCell;
import mara.mybox.fxml.cell.TableTimeCell;
import mara.mybox.tools.DateTools;
import mara.mybox.fxml.SoundTools;
import mara.mybox.value.AppVariables;
import static mara.mybox.value.Languages.message;
import mara.mybox.value.Languages;

/**
 * @Author Mara
 * @CreateDate 2018-7-9
 * @Description
 * @License Apache License Version 2.0
 */
public class FilesFindController extends BaseBatchFileController {

    protected ObservableList<FileInformation> filesList;
    protected long totalMatched;
    protected String done;

    @FXML
    protected TableView<FileInformation> filesView;
    @FXML
    protected TableColumn<FileInformation, String> fileColumn, typeColumn;
    @FXML
    protected TableColumn<FileInformation, Long> sizeColumn, modifyTimeColumn, createTimeColumn;

    public FilesFindController() {
        baseTitle = Languages.message("FilesFind");
    }

    @Override
    public void initControls() {
        try {
            super.initControls();
            initFilesTab();

            tableController.listButton.setVisible(false);
            openTargetButton.setVisible(false);
            openCheck.setVisible(false);

            done = Languages.message("Done");

        } catch (Exception e) {
            MyBoxLog.debug(e.toString());
        }

    }

    private void initFilesTab() {
        try {
            filesList = FXCollections.observableArrayList();
            filesView.setItems(filesList);

            fileColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
            fileColumn.setPrefWidth(400);

            typeColumn.setCellValueFactory(new PropertyValueFactory<>("fileSuffix"));

            sizeColumn.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
            sizeColumn.setCellFactory(new TableFileSizeCell());

            modifyTimeColumn.setCellValueFactory(new PropertyValueFactory<>("modifyTime"));
            modifyTimeColumn.setCellFactory(new TableTimeCell());

            createTimeColumn.setCellValueFactory(new PropertyValueFactory<>("createTime"));
            createTimeColumn.setCellFactory(new TableTimeCell());

            filesView.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getClickCount() > 1) {
                        int index = filesView.getSelectionModel().getSelectedIndex();
                        if (index < 0 || index > filesList.size() - 1) {
                            return;
                        }
                        FileInformation info = filesList.get(index);
                        view(info.getFile());
                    }
                }
            });

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }

    }

    @Override
    public boolean makeMoreParameters() {
        filesList.clear();
        filesView.refresh();
        totalFilesHandled = totalMatched = 0;
        return super.makeMoreParameters();
    }

    @Override
    public void disableControls(boolean disable) {
        tableController.thisPane.setDisable(disable);
        batchTabPane.getSelectionModel().select(targetTab);
    }

    @Override
    public void countHandling(File file) {
        if (file == null || !file.isFile()) {
            return;
        }
        totalFilesHandled++;
        if (totalFilesHandled % 100 == 0) {
            updateStatusLabel(Languages.message("Checked") + ": " + totalFilesHandled);
        }
    }

    @Override
    public String handleFile(File file) {
        try {
            if (!match(file)) {
                return done;
            }
            totalMatched++;
            filesList.add(new FileInformation(file));
            return done;
        } catch (Exception e) {
            return done;
        }
    }

    @Override
    public String handleDirectory(File directory) {
        try {
            if (directory == null || !directory.isDirectory()) {
                return done;
            }
            File[] files = directory.listFiles();
            if (files == null) {
                return done;
            }
            for (File srcFile : files) {
                if (task == null || task.isCancelled()) {
                    return done;
                }
                if (srcFile.isFile()) {
                    countHandling(srcFile);
                    if (!match(srcFile)) {
                        continue;
                    }
                    totalMatched++;
                    filesList.add(new FileInformation(srcFile));
                } else if (srcFile.isDirectory()) {
                    handleDirectory(srcFile);
                }
            }
            return done;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return done;
        }
    }

    @Override
    public void showCost() {
        String s;
        if (paused) {
            s = Languages.message("Paused");
        } else {
            s = Languages.message(currentParameters.status);
        }
        s += ".  "
                + Languages.message("TotalCheckedFiles") + ": " + totalFilesHandled + "   "
                + Languages.message("TotalMatched") + ": " + totalMatched + ".   "
                + Languages.message("Cost") + ": " + DateTools.datetimeMsDuration(new Date(), processStartTime) + ". "
                + Languages.message("StartTime") + ": " + DateTools.datetimeToString(processStartTime) + ", "
                + Languages.message("EndTime") + ": " + DateTools.datetimeToString(new Date());
        statusLabel.setText(s);
    }

    @Override
    public void donePost() {
        showCost();
        if (miaoCheck.isSelected()) {
            SoundTools.miao3();
        }
    }

    @FXML
    @Override
    public void openTarget(ActionEvent event) {

    }

}
