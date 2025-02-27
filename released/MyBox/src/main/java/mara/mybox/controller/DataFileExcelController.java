package mara.mybox.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import mara.mybox.db.DerbyBase;
import mara.mybox.db.data.DataDefinition;
import mara.mybox.db.data.VisitHistory;
import mara.mybox.db.table.ColumnDefinition;
import mara.mybox.db.table.ColumnDefinition.ColumnType;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.PopTools;
import mara.mybox.fxml.WindowTools;
import mara.mybox.tools.DateTools;
import mara.mybox.tools.FileCopyTools;
import mara.mybox.tools.FileDeleteTools;
import mara.mybox.tools.FileNameTools;
import mara.mybox.tools.FileTools;
import mara.mybox.tools.MicrosoftDocumentTools;
import mara.mybox.tools.TextTools;
import mara.mybox.tools.TmpFileTools;
import mara.mybox.value.Fxmls;
import mara.mybox.value.Languages;
import mara.mybox.value.UserConfig;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @Author Mara
 * @CreateDate 2021-1-17
 * @License Apache License Version 2.0
 */
public class DataFileExcelController extends BaseDataFileController {

    protected String currentSheetName, targetSheetName;
    protected List<String> sheetNames;

    @FXML
    protected TitledPane sheetsPane;
    @FXML
    protected ComboBox<String> sheetSelector;
    @FXML
    protected CheckBox sourceWithNamesCheck, targetWithNamesCheck, currentOnlyCheck;
    @FXML
    protected Button okSheetButton, plusSheetButton, renameSheetButton, deleteSheetButton2;
    @FXML
    protected VBox sheetsBox;

    public DataFileExcelController() {
        baseTitle = Languages.message("EditExcel");
        TipsLabelKey = "DataFileExcelTips";
    }

    @Override
    public void setFileType() {
        setFileType(VisitHistory.FileType.Excel);
    }

    @Override
    public void initControls() {
        try {
            super.initControls();
            currentSheetName = null;

            sourceWithNamesCheck.setSelected(UserConfig.getBoolean(baseName + "SourceWithNames", true));
            sourceWithNamesCheck.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if (!isSettingValues) {
                    UserConfig.setBoolean(baseName + "SourceWithNames", newValue);
                }
            });

            targetWithNamesCheck.setSelected(UserConfig.getBoolean(baseName + "TargetWithNames", true));
            targetWithNamesCheck.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if (!isSettingValues) {
                    UserConfig.setBoolean(baseName + "TargetWithNames", newValue);
                }
            });

            currentOnlyCheck.setSelected(UserConfig.getBoolean(baseName + "CurrentOnly", false));
            currentOnlyCheck.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if (!isSettingValues) {
                    UserConfig.setBoolean(baseName + "CurrentOnly", newValue);
                }
            });

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @Override
    public void initCurrentPage() {
        currentSheetName = null;
        targetSheetName = null;
        super.initCurrentPage();
    }

    public void setFile(File file, boolean withName) {
        sourceFile = file;
        sourceWithNamesCheck.setSelected(withName);
        initCurrentPage();
        loadFile(true);
    }

    @Override
    public void initFile() {
        sheetNames = null;
        sheetsBox.setDisable(sourceFile == null);
        super.initFile();
    }

    @Override
    protected boolean readDataDefinition(boolean pickOptions) {
        try ( Connection conn = DerbyBase.getConnection();
                 Workbook wb = WorkbookFactory.create(sourceFile)) {
            int sheetsNumber = wb.getNumberOfSheets();
            sheetNames = new ArrayList<>();
            for (int i = 0; i < sheetsNumber; i++) {
                sheetNames.add(wb.getSheetName(i));
            }
            Sheet sheet;
            if (currentSheetName != null) {
                sheet = wb.getSheet(currentSheetName);
            } else {
                sheet = wb.getSheetAt(0);
                currentSheetName = sheet.getSheetName();
            }
            dataName = sourceFile.getAbsolutePath() + "-" + currentSheetName;

            dataDefinition = tableDataDefinition.read(conn, dataType, dataName);

            if (pickOptions || dataDefinition == null) {
                sourceWithNames = sourceWithNamesCheck.isSelected();
            } else {
                sourceWithNames = dataDefinition.isHasHeader();
            }
            boolean changed = pickOptions;
            if (sourceWithNames) {
                Iterator<Row> iterator = sheet.iterator();
                Row firstRow = null;
                if (iterator != null) {
                    while (iterator.hasNext()) {
                        firstRow = iterator.next();
                        if (firstRow != null) {
                            break;
                        }
                    }
                }
                if (firstRow == null) {
                    sourceWithNames = false;
                    changed = true;
                }
            }
            if (dataDefinition == null) {
                dataDefinition = DataDefinition.create().setDataName(dataName).setDataType(dataType)
                        .setHasHeader(sourceWithNames);
                tableDataDefinition.insertData(conn, dataDefinition);
                conn.commit();
            } else {
                if (changed) {
                    dataDefinition.setHasHeader(sourceWithNames);
                    tableDataDefinition.updateData(conn, dataDefinition);
                    conn.commit();
                }
                savedColumns = tableDataColumn.read(conn, dataDefinition.getDfid());
            }
        } catch (Exception e) {
            loadError = e.toString();
            return false;
        }
        return dataDefinition != null && dataDefinition.getDfid() >= 0;
    }

    @Override
    protected boolean readColumns() {
        columns = new ArrayList<>();
        if (!sourceWithNames) {
            return true;
        }
        try ( Workbook wb = WorkbookFactory.create(sourceFile)) {
            Sheet sheet;
            if (currentSheetName != null) {
                sheet = wb.getSheet(currentSheetName);
            } else {
                sheet = wb.getSheetAt(0);
                currentSheetName = sheet.getSheetName();
            }
            Iterator<Row> iterator = sheet.iterator();
            if (iterator == null) {
                sourceWithNames = false;
                return true;
            }
            Row firstRow = null;
            while (iterator.hasNext()) {
                firstRow = iterator.next();
                if (firstRow != null) {
                    break;
                }
            }
            if (firstRow == null) {
                sourceWithNames = false;
                return true;
            }
            for (int col = firstRow.getFirstCellNum(); col < firstRow.getLastCellNum(); col++) {
                String name = (Languages.message(colPrefix) + (col + 1));
                ColumnType type = ColumnType.String;
                Cell cell = firstRow.getCell(col);
                if (cell != null) {
                    switch (cell.getCellType()) {
                        case NUMERIC:
                            type = ColumnType.Double;
                            break;
                        case BOOLEAN:
                            type = ColumnType.Boolean;
                            break;
                    }
                    String v = MicrosoftDocumentTools.cellString(cell);
                    if (!v.isBlank()) {
                        name = v;
                    }
                }
                boolean found = false;
                if (savedColumns != null) {
                    for (ColumnDefinition def : savedColumns) {
                        if (def.getName().equals(name)) {
                            columns.add(def);
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    ColumnDefinition column = new ColumnDefinition(name, type);
                    columns.add(column);
                }
            }
            if (ColumnDefinition.valid(this, columns)) {
                tableDataColumn.save(dataDefinition.getDfid(), columns);
            }
        } catch (Exception e) {
            loadError = e.toString();
            return false;
        }
        return true;
    }

    @Override
    protected boolean readTotal() {
        try ( Workbook wb = WorkbookFactory.create(sourceFile)) {
            Sheet sheet;
            if (currentSheetName != null) {
                sheet = wb.getSheet(currentSheetName);
            } else {
                sheet = wb.getSheetAt(0);
                currentSheetName = sheet.getSheetName();
            }
            Iterator<Row> iterator = sheet.iterator();
            if (iterator != null) {
                while (backgroundTask != null && !backgroundTask.isCancelled() && iterator.hasNext()) {
                    iterator.next();
                    totalSize++;
                }
                if (backgroundTask == null || backgroundTask.isCancelled()) {
                    totalSize = 0;
                } else if (sourceWithNames && totalSize > 0) {
                    totalSize--;
                }
            }
        } catch (Exception e) {
            loadError = e.toString();
            return false;
        }
        return true;
    }

    @Override
    protected void afterFileLoaded() {
        sheetSelector.getItems().clear();
        if (sheetNames != null) {
            sheetSelector.getItems().setAll(sheetNames);
        }
        sheetSelector.setValue(currentSheetName);
        deleteSheetButton2.setDisable(sheetNames == null || sheetNames.size() <= 1);
        int current = sheetSelector.getSelectionModel().getSelectedIndex();
        nextButton.setDisable(current >= sheetSelector.getItems().size() - 1);
        previousButton.setDisable(current <= 0);
        sheetsPane.setExpanded(true);
        targetSheetName = currentSheetName;
        updateLabel();
    }

    @Override
    protected String titleName() {
        return (sourceFile == null ? "" : sourceFile.getAbsolutePath())
                + (currentSheetName == null ? "" : " - " + currentSheetName);
    }

    @Override
    protected String[][] readPageData() {
        if (currentPageStart < 1) {
            currentPageStart = 1;
        }
        long end = currentPageStart + pageSize;
        String[][] data = null;
        try ( Workbook wb = WorkbookFactory.create(sourceFile)) {
            Sheet sheet;
            if (currentSheetName != null) {
                sheet = wb.getSheet(currentSheetName);
            } else {
                sheet = wb.getSheetAt(0);
                currentSheetName = sheet.getSheetName();
            }
            List<List<String>> rows = new ArrayList<>();
            Iterator<Row> iterator = sheet.iterator();
            int rowIndex = 0, maxCol = 0;
            if (iterator != null && iterator.hasNext()) {
                if (sourceWithNames) {
                    while (iterator.hasNext() && (iterator.next() == null)) {
                    }
                }
                while (iterator.hasNext()) {
                    Row fileRow = iterator.next();
                    if (fileRow == null) {
                        continue;
                    }
                    ++rowIndex;
                    if (rowIndex < currentPageStart) {
                        continue;
                    }
                    if (rowIndex >= end) {
                        break;
                    }
                    List<String> row = new ArrayList<>();
                    for (int cellIndex = fileRow.getFirstCellNum(); cellIndex < fileRow.getLastCellNum(); cellIndex++) {
                        String v = MicrosoftDocumentTools.cellString(fileRow.getCell(cellIndex));
                        row.add(v);
                    }
                    rows.add(row);
                    if (maxCol < row.size()) {
                        maxCol = row.size();
                    }
                }
            }
            if (!rows.isEmpty() && maxCol > 0) {
                data = new String[rows.size()][maxCol];
                for (int row = 0; row < rows.size(); row++) {
                    List<String> rowData = rows.get(row);
                    for (int col = 0; col < rowData.size(); col++) {
                        data[row][col] = rowData.get(col);
                    }
                }
            }
        } catch (Exception e) {
            loadError = e.toString();
            MyBoxLog.console(loadError);
        }
        if (data == null) {
            currentPageEnd = currentPageStart;
        } else {
            currentPageEnd = currentPageStart + data.length;
        }
        return data;
    }

    @Override
    protected void updateLabel() {
        if (sourceFile == null) {
            loadedLabel.setText(Languages.message("FirstLineAsNames") + ": " + (sourceWithNames ? Languages.message("Yes") : Languages.message("No")));
        } else {
            loadedLabel.setText(Languages.message("File") + ": " + sourceFile.getAbsolutePath() + "\n"
                    + Languages.message("CurrentSheet") + ": " + (currentSheetName == null ? "" : currentSheetName + "\n")
                    + Languages.message("RowsNumber") + ": " + totalSize + "\n"
                    + (columns == null ? "" : Languages.message("ColumnsNumber") + ": " + columns.size() + "\n")
                    + Languages.message("FirstLineAsNames") + ": " + (sourceWithNames ? Languages.message("Yes") : Languages.message("No")) + "\n"
                    + Languages.message("Load") + ": " + DateTools.nowString());
        }
    }

    @FXML
    public void setSheet() {
        if (sourceFile == null || !checkBeforeNextAction()) {
            return;
        }
        initCurrentPage();
        currentSheetName = sheetSelector.getValue();
        int current = sheetSelector.getSelectionModel().getSelectedIndex();
        nextButton.setDisable(current >= sheetSelector.getItems().size() - 1);
        previousButton.setDisable(current <= 0);
        sheetSelector.getSelectionModel().select(current + 1);
        loadFile(false);
    }

    @FXML
    @Override
    public void saveAction() {
        if (sourceFile == null) {
            saveAsAction();
            return;
        }
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {
                String[][] data;

                @Override
                protected boolean handle() {
                    backup();
                    error = save(sourceFile, sourceWithNames);
                    return error == null;
                }

                @Override
                protected void whenSucceeded() {
                    popInformation(Languages.message("Saved"));
                    currentSheetName = targetSheetName;
                    dataChanged = false;
                    loadFile();
                }

            };
            handling(task);
            task.setSelf(task);
            Thread thread = new Thread(task);
            thread.setDaemon(false);
            thread.start();
        }
    }

    @FXML
    @Override
    public void saveAsAction() {
        String name = null;
        if (sourceFile != null) {
            name = FileNameTools.getFilePrefix(sourceFile.getName());
        }
        targetFile = chooseSaveFile(UserConfig.getPath(baseName + "TargetPath"),
                name, targetExtensionFilter);
        if (targetFile == null) {
            return;
        }
        recordFileWritten(targetFile);
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {
                String[][] data;

                @Override
                protected boolean handle() {
                    error = save(targetFile, targetWithNamesCheck.isSelected());
                    return error == null;
                }

                @Override
                protected void whenSucceeded() {
                    popInformation(Languages.message("Saved"));
                    if (sourceFile == null || saveAsType == SaveAsType.Load) {
                        dataChanged = false;
                        sourceFileChanged(targetFile);

                    } else if (saveAsType == SaveAsType.Open) {
                        DataFileExcelController controller = (DataFileExcelController) WindowTools.openStage(Fxmls.DataFileExcelFxml);
                        controller.sourceFileChanged(targetFile);
                    }

                }

            };
            handling(task);
            task.setSelf(task);
            Thread thread = new Thread(task);
            thread.setDaemon(false);
            thread.start();
        }
    }

    public String save(File file, boolean withName) {
        File tmpFile = TmpFileTools.getTempFile();
        if (withName && columns == null) {
            makeColumns(colsCheck.length);
        }
        List<String> otherSheetNames = new ArrayList<>();
        String theSheetName;
        if (sourceFile != null) {
            try ( Workbook sourceBook = WorkbookFactory.create(sourceFile)) {
                Sheet sourceSheet;
                if (currentSheetName != null) {
                    sourceSheet = sourceBook.getSheet(currentSheetName);
                } else {
                    sourceSheet = sourceBook.getSheetAt(0);
                    currentSheetName = sourceSheet.getSheetName();
                }
                if (targetSheetName == null) {
                    targetSheetName = currentSheetName;
                }
                theSheetName = targetSheetName;
                Workbook targetBook;
                Sheet targetSheet;
                File tmpDataFile = null;
                int sheetsNumber = sourceBook.getNumberOfSheets();
                if (sheetsNumber == 1
                        || (!file.equals(sourceFile) && currentOnlyCheck.isSelected())) {
                    targetBook = new XSSFWorkbook();
                    targetSheet = targetBook.createSheet(targetSheetName);
                } else {
                    tmpDataFile = TmpFileTools.getTempFile();
                    FileCopyTools.copyFile(sourceFile, tmpDataFile);
                    targetBook = WorkbookFactory.create(tmpDataFile);
                    if (!currentOnlyCheck.isSelected()) {
                        for (int i = 0; i < sheetsNumber; i++) {
                            otherSheetNames.add(targetBook.getSheetName(i));
                        }
                        otherSheetNames.remove(currentSheetName);
                    }
                    int index = targetBook.getSheetIndex(currentSheetName);
                    targetBook.removeSheetAt(index);
                    targetSheet = targetBook.createSheet(targetSheetName);
                    targetBook.setSheetOrder(targetSheetName, index);
                }
                int targetRowIndex = 0;
                if (withName) {
                    targetRowIndex = writeHeader(targetSheet, targetRowIndex);
                }
                Iterator<Row> iterator = sourceSheet.iterator();
                if (iterator != null && iterator.hasNext()) {
                    if (sourceWithNames) {
                        while (iterator.hasNext() && (iterator.next() == null)) {
                        }
                    }
                    int sourceRowIndex = 0;
                    while (iterator.hasNext()) {
                        Row sourceRow = iterator.next();
                        if (sourceRow == null) {
                            continue;
                        }
                        sourceRowIndex++;
                        if (sourceRowIndex < currentPageStart || sourceRowIndex >= currentPageEnd) {
                            Row targetRow = targetSheet.createRow(targetRowIndex++);
                            MicrosoftDocumentTools.copyRow(sourceRow, targetRow);
                        } else if (sourceRowIndex == currentPageStart) {
                            targetRowIndex = writePageData(targetSheet, targetRowIndex);
                        }
                    }
                } else {
                    writePageData(targetSheet, targetRowIndex);
                }
                try ( FileOutputStream fileOut = new FileOutputStream(tmpFile)) {
                    targetBook.write(fileOut);
                }
                targetBook.close();
                FileDeleteTools.delete(tmpDataFile);
            } catch (Exception e) {
                MyBoxLog.error(e);
                return e.toString();
            }

        } else {
            try ( Workbook targetBook = new XSSFWorkbook()) {
                if (targetSheetName != null) {
                    theSheetName = targetSheetName;
                } else {
                    theSheetName = Languages.message("Sheet") + "1";
                }
                Sheet targetSheet = targetBook.createSheet(theSheetName);
                int targetRowIndex = 0;
                if (withName) {
                    targetRowIndex = writeHeader(targetSheet, targetRowIndex);
                }
                writePageData(targetSheet, targetRowIndex);
                try ( FileOutputStream fileOut = new FileOutputStream(tmpFile)) {
                    targetBook.write(fileOut);
                }
            } catch (Exception e) {
                MyBoxLog.error(e);
                return e.toString();
            }
        }
        if (FileTools.rename(tmpFile, file)) {
            saveColumns(file, theSheetName, otherSheetNames, withName);
            return null;
        } else {
            return "Failed";
        }
    }

    protected int writeHeader(Sheet targetSheet, int targetRowIndex) {
        if (colsCheck == null) {
            return targetRowIndex;
        }
        int index = targetRowIndex;
        Row targetRow = targetSheet.createRow(index++);
        for (int col = 0; col < colsCheck.length; col++) {
            Cell targetCell = targetRow.createCell(col, CellType.STRING);
            targetCell.setCellValue(colsCheck[col].getText());
        }
        return index;
    }

    protected int writePageData(Sheet targetSheet, int targetRowIndex) {
        if (inputs == null) {
            return targetRowIndex;
        }
        int index = targetRowIndex;
        for (TextField[] row : inputs) {
            Row targetRow = targetSheet.createRow(index++);
            for (int col = 0; col < row.length; col++) {
                Cell targetCell = targetRow.createCell(col, CellType.STRING);
                targetCell.setCellValue(row[col].getText());
            }
        }
        return index;
    }

    protected void saveColumns(File file, String currentSheetName, List<String> otherSheetNames, boolean withName) {
        try ( Connection conn = DerbyBase.getConnection()) {
            if (currentSheetName != null) {
                String dname = file.getAbsolutePath() + "-" + currentSheetName;
                tableDataDefinition.clear(conn, dataType, dname);
                DataDefinition def = DataDefinition.create().setDataType(dataType).setDataName(dname)
                        .setHasHeader(withName);
                tableDataDefinition.insertData(conn, def);
                if (ColumnDefinition.valid(this, columns)) {
                    tableDataColumn.save(conn, def.getDfid(), columns);
                    conn.commit();
                }
            }
            if (otherSheetNames != null) {
                for (String name : otherSheetNames) {
                    String sourceDataName = sourceFile.getAbsolutePath() + "-" + name;
                    DataDefinition sourceDef = tableDataDefinition.read(conn, dataType, sourceDataName);
                    if (sourceDef == null) {
                        continue;
                    }
                    String targetDataName = file.getAbsolutePath() + "-" + name;
                    tableDataDefinition.clear(conn, dataType, targetDataName);
                    DataDefinition targetDef = DataDefinition.create().setDataType(dataType).setDataName(targetDataName)
                            .setHasHeader(sourceDef.isHasHeader());
                    tableDataDefinition.insertData(conn, targetDef);
                    List<ColumnDefinition> sourceColumns = tableDataColumn.read(conn, sourceDef.getDfid());
                    if (sourceColumns == null || sourceColumns.isEmpty()) {
                        continue;
                    }
                    for (ColumnDefinition column : sourceColumns) {
                        column.setDataid(targetDef.getDfid());
                    }
                    tableDataColumn.save(conn, targetDef.getDfid(), sourceColumns);
                    conn.commit();
                }
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
        }
    }

    @Override
    protected boolean saveColumns() {
        if (sourceFile == null) {
            return false;
        }
        try ( Connection conn = DerbyBase.getConnection()) {
            String dname = sourceFile.getAbsolutePath() + "-" + currentSheetName;
            tableDataDefinition.clear(conn, dataType, dname);
            DataDefinition def = DataDefinition.create().setDataType(dataType).setDataName(dname)
                    .setHasHeader(sourceWithNames);
            tableDataDefinition.insertData(conn, def);
            if (ColumnDefinition.valid(this, columns)) {
                tableDataColumn.save(conn, def.getDfid(), columns);
                conn.commit();
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
            return false;
        }
        return true;
    }

    @FXML
    protected void plusSheet() {
        if (sourceFile == null || sheetNames == null || !checkBeforeNextAction()) {
            return;
        }
        String newName = Languages.message("Sheet") + (sheetNames.size() + 1);
        while (sheetNames != null && sheetNames.contains(newName)) {
            newName += "m";
        }
        String value = askValue(Languages.message("Create"), Languages.message("SheetName"), newName);
        if (value == null || value.isBlank()) {
            popError(Languages.message("InvalidData"));
            return;
        }
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {

                @Override
                protected boolean handle() {
                    File tmpFile = TmpFileTools.getTempFile();
                    File tmpDataFile = TmpFileTools.getTempFile();
                    FileCopyTools.copyFile(sourceFile, tmpDataFile);
                    try ( Workbook targetBook = WorkbookFactory.create(tmpDataFile)) {
                        targetBook.createSheet(value);

                        try ( FileOutputStream fileOut = new FileOutputStream(tmpFile)) {
                            targetBook.write(fileOut);
                        }
                    } catch (Exception e) {
                        error = e.toString();
                        return false;
                    }
                    FileDeleteTools.delete(tmpDataFile);
                    if (tmpFile == null || !tmpFile.exists()) {
                        return false;
                    }
                    return FileTools.rename(tmpFile, sourceFile);
                }

                @Override
                protected void whenSucceeded() {
                    sheetSelector.setValue(value);
                    setSheet();
                }

            };
            handling(task);
            task.setSelf(task);
            Thread thread = new Thread(task);
            thread.setDaemon(false);
            thread.start();
        }
    }

    @FXML
    protected void renameSheet() {
        if (sourceFile == null || !checkBeforeNextAction()) {
            return;
        }
        String newName = currentSheetName + "m";
        while (sheetNames != null && sheetNames.contains(newName)) {
            newName += "m";
        }
        String value = askValue(Languages.message("CurrentName") + ": " + currentSheetName, Languages.message("NewName"), newName);
        if (value == null || value.isBlank() || value.equals(currentSheetName)
                || (sheetNames != null && sheetNames.contains(value))) {
            popError(Languages.message("InvalidData"));
            return;
        }
        targetSheetName = value;
        saveAction();
    }

    @FXML
    protected void deleteSheet() {
        if (sourceFile == null || sheetNames == null || sheetNames.size() <= 1) {
            return;
        }
        if (!PopTools.askSure(baseTitle, currentSheetName, Languages.message("SureDelete"))) {
            return;
        }
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {
                private int index;

                @Override
                protected boolean handle() {
                    File tmpFile = TmpFileTools.getTempFile();
                    File tmpDataFile = TmpFileTools.getTempFile();
                    FileCopyTools.copyFile(sourceFile, tmpDataFile);
                    try ( Workbook targetBook = WorkbookFactory.create(tmpDataFile)) {
                        index = targetBook.getSheetIndex(currentSheetName);
                        targetBook.removeSheetAt(index);

                        try ( FileOutputStream fileOut = new FileOutputStream(tmpFile)) {
                            targetBook.write(fileOut);
                        }
                    } catch (Exception e) {
                        error = e.toString();
                        return false;
                    }
                    FileDeleteTools.delete(tmpDataFile);
                    if (tmpFile == null || !tmpFile.exists()) {
                        return false;
                    }
                    return FileTools.rename(tmpFile, sourceFile);
                }

                @Override
                protected void whenSucceeded() {
                    if (sheetNames == null || index >= sheetNames.size() - 1) {
                        sheetSelector.getSelectionModel().select(0);
                    } else {
                        sheetSelector.getSelectionModel().select(index + 1);
                    }
                    dataChanged = false;
                    setSheet();
                }

            };
            handling(task);
            task.setSelf(task);
            Thread thread = new Thread(task);
            thread.setDaemon(false);
            thread.start();
        }
    }

    @FXML
    @Override
    public void nextAction() {
        int current = sheetSelector.getSelectionModel().getSelectedIndex();
        if (current >= sheetSelector.getItems().size() - 1) {
            popError(Languages.message("NoMore"));
            return;
        }
        sheetSelector.getSelectionModel().select(current + 1);
        setSheet();
    }

    @FXML
    @Override
    public void previousAction() {
        int current = sheetSelector.getSelectionModel().getSelectedIndex();
        if (current == 0) {
            popError(Languages.message("NoMore"));
            return;
        }
        sheetSelector.getSelectionModel().select(current - 1);
        setSheet();
    }

    @Override
    protected File setAllColValues(int col, String value) {
        if (sourceFile == null || col < 0 || value == null) {
            return null;
        }
        File tmpTargetFile = TmpFileTools.getTempFile();
        File tmpDataFile = TmpFileTools.getTempFile();
        FileCopyTools.copyFile(sourceFile, tmpDataFile);
        try ( Workbook sourceBook = WorkbookFactory.create(sourceFile);
                 Workbook targetBook = WorkbookFactory.create(tmpDataFile)) {
            Sheet sourceSheet;
            if (currentSheetName != null) {
                sourceSheet = sourceBook.getSheet(currentSheetName);
            } else {
                sourceSheet = sourceBook.getSheetAt(0);
                currentSheetName = sourceSheet.getSheetName();
            }
            int index = targetBook.getSheetIndex(currentSheetName);
            targetBook.removeSheetAt(index);
            Sheet targetSheet = targetBook.createSheet(currentSheetName);
            targetBook.setSheetOrder(currentSheetName, index);
            Iterator<Row> iterator = sourceSheet.iterator();
            if (iterator != null && iterator.hasNext()) {
                int targetRowIndex = 0;
                if (sourceWithNames) {
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    for (int i = 0; i < colsCheck.length; i++) {
                        Cell targetCell = targetRow.createCell(i);
                        targetCell.setCellValue(colsCheck[i].getText());
                    }
                    while (iterator.hasNext() && (iterator.next() == null)) {
                    }
                }
                while (iterator.hasNext()) {
                    Row sourceRow = iterator.next();
                    if (sourceRow == null) {
                        continue;
                    }
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    int targetIndex = col + sourceRow.getFirstCellNum();
                    for (int cellIndex = sourceRow.getFirstCellNum(); cellIndex < sourceRow.getLastCellNum(); cellIndex++) {
                        CellType type = CellType.STRING;
                        Cell sourceCell = sourceRow.getCell(cellIndex);
                        if (sourceCell != null) {
                            type = sourceCell.getCellType();
                        }
                        Cell targetCell = targetRow.createCell(cellIndex, type);
                        if (targetIndex == cellIndex) {
                            MicrosoftDocumentTools.setCell(targetCell, type, value);
                        } else {
                            MicrosoftDocumentTools.copyCell(sourceCell, targetCell, type);
                        }
                    }
                }
            }
            try ( FileOutputStream fileOut = new FileOutputStream(tmpTargetFile)) {
                targetBook.write(fileOut);
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
            return null;
        }
        FileDeleteTools.delete(tmpDataFile);
        return tmpTargetFile;

    }

    @Override
    protected StringBuilder copyAllColValues(int col) {
        if (sourceFile == null || col < 0) {
            return null;
        }
        StringBuilder s = null;
        try ( Workbook sourceBook = WorkbookFactory.create(sourceFile)) {
            Sheet sourceSheet;
            if (currentSheetName != null) {
                sourceSheet = sourceBook.getSheet(currentSheetName);
            } else {
                sourceSheet = sourceBook.getSheetAt(0);
                currentSheetName = sourceSheet.getSheetName();
            }
            Iterator<Row> iterator = sourceSheet.iterator();
            if (iterator != null && iterator.hasNext()) {
                if (sourceWithNames) {
                    while (iterator.hasNext() && (iterator.next() == null)) {
                    }
                }
                int sourceRowIndex = 0;
                while (iterator.hasNext()) {
                    Row sourceRow = iterator.next();
                    if (sourceRow == null) {
                        continue;
                    }
                    sourceRowIndex++;
                    if (sourceRowIndex < currentPageStart || sourceRowIndex >= currentPageEnd) {
                        String v = null;
                        int cellIndex = sourceRow.getFirstCellNum() + col;
                        if (cellIndex < sourceRow.getLastCellNum()) {
                            v = MicrosoftDocumentTools.cellString(sourceRow.getCell(cellIndex));
                        }
                        copiedCol.add(v == null ? "" : v);
                    } else if (sourceRowIndex == currentPageStart) {
                        copyPageCol(col);
                    }
                }
            } else {
                copyPageCol(col);
            }
            for (String v : copiedCol) {
                if (s == null) {
                    s = new StringBuilder();
                    s.append(v);
                } else {
                    s.append("\n").append(v);
                }
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
            return null;
        }
        return s;
    }

    @Override
    protected File pasteAllColValues(int col) {
        if (sourceFile == null || col < 0) {
            return null;
        }
        File tmpTargetFile = TmpFileTools.getTempFile();
        File tmpDataFile = TmpFileTools.getTempFile();
        FileCopyTools.copyFile(sourceFile, tmpDataFile);
        try ( Workbook sourceBook = WorkbookFactory.create(sourceFile);
                 Workbook targetBook = WorkbookFactory.create(tmpDataFile)) {
            Sheet sourceSheet;
            if (currentSheetName != null) {
                sourceSheet = sourceBook.getSheet(currentSheetName);
            } else {
                sourceSheet = sourceBook.getSheetAt(0);
                currentSheetName = sourceSheet.getSheetName();
            }
            int index = targetBook.getSheetIndex(currentSheetName);
            targetBook.removeSheetAt(index);
            Sheet targetSheet = targetBook.createSheet(currentSheetName);
            targetBook.setSheetOrder(currentSheetName, index);
            Iterator<Row> iterator = sourceSheet.iterator();
            if (iterator != null && iterator.hasNext()) {
                int targetRowIndex = 0;
                if (sourceWithNames) {
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    for (int i = 0; i < colsCheck.length; i++) {
                        Cell targetCell = targetRow.createCell(i);
                        targetCell.setCellValue(colsCheck[i].getText());
                    }
                    while (iterator.hasNext() && (iterator.next() == null)) {
                    }
                }
                int rowIndex = 0, copiedSize = copiedCol.size();
                while (iterator.hasNext()) {
                    Row sourceRow = iterator.next();
                    if (sourceRow == null) {
                        continue;
                    }
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    int targetIndex = col + sourceRow.getFirstCellNum();
                    for (int cellIndex = sourceRow.getFirstCellNum(); cellIndex < sourceRow.getLastCellNum(); cellIndex++) {
                        CellType type = CellType.STRING;
                        Cell sourceCell = sourceRow.getCell(cellIndex);
                        if (sourceCell != null) {
                            type = sourceCell.getCellType();
                        }
                        Cell targetCell = targetRow.createCell(cellIndex, type);
                        if ((rowIndex < copiedSize) && (targetIndex == cellIndex)) {
                            MicrosoftDocumentTools.setCell(targetCell, type, copiedCol.get(rowIndex));
                        } else {
                            MicrosoftDocumentTools.copyCell(sourceCell, targetCell, type);
                        }
                    }
                    rowIndex++;
                }
            }
            try ( FileOutputStream fileOut = new FileOutputStream(tmpTargetFile)) {
                targetBook.write(fileOut);
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
            return null;
        }
        FileDeleteTools.delete(tmpDataFile);
        return tmpTargetFile;
    }

    @Override
    protected File insertFileCol(int col, boolean left, int number) {
        if (sourceFile == null || col < 0 || number < 1) {
            return null;
        }
        File tmpFile = TmpFileTools.getTempFile();
        File tmpDataFile = TmpFileTools.getTempFile();
        FileCopyTools.copyFile(sourceFile, tmpDataFile);
        try ( Workbook sourceBook = WorkbookFactory.create(sourceFile);
                 Workbook targetBook = WorkbookFactory.create(tmpDataFile)) {
            Sheet sourceSheet;
            if (currentSheetName != null) {
                sourceSheet = sourceBook.getSheet(currentSheetName);
            } else {
                sourceSheet = sourceBook.getSheetAt(0);
                currentSheetName = sourceSheet.getSheetName();
            }
            int index = targetBook.getSheetIndex(currentSheetName);
            targetBook.removeSheetAt(index);
            Sheet targetSheet = targetBook.createSheet(currentSheetName);
            targetBook.setSheetOrder(currentSheetName, index);

            Iterator<Row> iterator = sourceSheet.iterator();
            if (iterator != null && iterator.hasNext()) {
                int targetRowIndex = 0;
                if (sourceWithNames) {
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    for (int i = 0; i < columns.size(); i++) {
                        Cell targetCell = targetRow.createCell(i);
                        targetCell.setCellValue(columns.get(i).getName());
                    }
                    while (iterator.hasNext() && (iterator.next() == null)) {
                    }
                }
                while (iterator.hasNext()) {
                    Row sourceRow = iterator.next();
                    if (sourceRow == null) {
                        continue;
                    }
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    int targetCol = 0;
                    Cell targetCell;
                    for (int cellIndex = sourceRow.getFirstCellNum(); cellIndex < sourceRow.getLastCellNum(); cellIndex++) {
                        if (col == cellIndex && left) {
                            for (int i = 0; i < number; i++) {
                                targetCell = targetRow.createCell(targetCol++, CellType.STRING);
                                MicrosoftDocumentTools.setCell(targetCell, CellType.STRING, defaultColValue);
                            }
                        }
                        CellType type = CellType.STRING;
                        Cell sourceCell = sourceRow.getCell(cellIndex);
                        if (sourceCell != null) {
                            type = sourceCell.getCellType();
                        }
                        targetCell = targetRow.createCell(targetCol++, type);
                        MicrosoftDocumentTools.copyCell(sourceCell, targetCell, type);
                        if (col == cellIndex && !left) {
                            for (int i = 0; i < number; i++) {
                                targetCell = targetRow.createCell(targetCol++, CellType.STRING);
                                MicrosoftDocumentTools.setCell(targetCell, CellType.STRING, defaultColValue);
                            }
                        }
                    }
                }
            }
            try ( FileOutputStream fileOut = new FileOutputStream(tmpFile)) {
                targetBook.write(fileOut);
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
            return null;
        }
        FileDeleteTools.delete(tmpDataFile);
        return tmpFile;
    }

    @Override
    protected File DeleteFileCol(int col) {
        if (sourceFile == null || col < 0) {
            return null;
        }
        File tmpFile = TmpFileTools.getTempFile();
        File tmpDataFile = TmpFileTools.getTempFile();
        FileCopyTools.copyFile(sourceFile, tmpDataFile);
        try ( Workbook sourceBook = WorkbookFactory.create(sourceFile);
                 Workbook targetBook = WorkbookFactory.create(tmpDataFile)) {
            Sheet sourceSheet;
            if (currentSheetName != null) {
                sourceSheet = sourceBook.getSheet(currentSheetName);
            } else {
                sourceSheet = sourceBook.getSheetAt(0);
                currentSheetName = sourceSheet.getSheetName();
            }

            columns.remove(col);
            saveColumns();

            int index = targetBook.getSheetIndex(currentSheetName);
            targetBook.removeSheetAt(index);
            Sheet targetSheet = targetBook.createSheet(currentSheetName);
            targetBook.setSheetOrder(currentSheetName, index);

            Iterator<Row> iterator = sourceSheet.iterator();
            if (iterator != null && iterator.hasNext()) {
                int targetRowIndex = 0;
                if (sourceWithNames) {
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    for (int i = 0; i < columns.size(); i++) {
                        Cell targetCell = targetRow.createCell(i);
                        targetCell.setCellValue(columns.get(i).getName());
                    }
                    while (iterator.hasNext() && (iterator.next() == null)) {
                    }
                }
                while (iterator.hasNext()) {
                    Row sourceRow = iterator.next();
                    if (sourceRow == null) {
                        continue;
                    }
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    int targetCol = 0;
                    Cell targetCell;
                    int targetIndex = col + sourceRow.getFirstCellNum();
                    for (int cellIndex = sourceRow.getFirstCellNum(); cellIndex < sourceRow.getLastCellNum(); cellIndex++) {
                        if (targetIndex == cellIndex) {
                            continue;
                        }
                        CellType type = CellType.STRING;
                        Cell sourceCell = sourceRow.getCell(cellIndex);
                        if (sourceCell != null) {
                            type = sourceCell.getCellType();
                        }
                        targetCell = targetRow.createCell(targetCol++, type);
                        MicrosoftDocumentTools.copyCell(sourceCell, targetCell, type);
                    }
                }
            }
            try ( FileOutputStream fileOut = new FileOutputStream(tmpFile)) {
                targetBook.write(fileOut);
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
            return null;
        }
        FileDeleteTools.delete(tmpDataFile);
        return tmpFile;
    }

    @Override
    protected File deleteFileAllCols() {
        if (sourceFile == null) {
            return null;
        }
        File tmpFile = TmpFileTools.getTempFile();
        File tmpDataFile = TmpFileTools.getTempFile();
        FileCopyTools.copyFile(sourceFile, tmpDataFile);
        try ( Workbook sourceBook = WorkbookFactory.create(sourceFile);
                 Workbook targetBook = WorkbookFactory.create(tmpDataFile)) {
            if (currentSheetName == null) {
                currentSheetName = sourceBook.getSheetName(0);
            }

            int index = targetBook.getSheetIndex(currentSheetName);
            targetBook.removeSheetAt(index);
            targetBook.createSheet(currentSheetName);
            targetBook.setSheetOrder(currentSheetName, index);

            try ( FileOutputStream fileOut = new FileOutputStream(tmpFile)) {
                targetBook.write(fileOut);
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
            return null;
        }
        FileDeleteTools.delete(tmpDataFile);
        return tmpFile;
    }

    @Override
    protected File orderFileCol(int col, boolean asc) {
        if (sourceFile == null || col < 0) {
            return null;
        }
        File tmpFile = TmpFileTools.getTempFile();
        File tmpDataFile = TmpFileTools.getTempFile();
        FileCopyTools.copyFile(sourceFile, tmpDataFile);
        try ( Workbook sourceBook = WorkbookFactory.create(sourceFile);
                 Workbook targetBook = WorkbookFactory.create(tmpDataFile)) {
            Sheet sourceSheet;
            if (currentSheetName != null) {
                sourceSheet = sourceBook.getSheet(currentSheetName);
            } else {
                sourceSheet = sourceBook.getSheetAt(0);
                currentSheetName = sourceSheet.getSheetName();
            }

            int index = targetBook.getSheetIndex(currentSheetName);
            targetBook.removeSheetAt(index);
            Sheet targetSheet = targetBook.createSheet(currentSheetName);
            targetBook.setSheetOrder(currentSheetName, index);

            List<Row> records = new ArrayList<>();
            Iterator<Row> iterator = sourceSheet.iterator();
            if (iterator != null && iterator.hasNext()) {
                if (sourceWithNames) {
                    while (iterator.hasNext() && (iterator.next() == null)) {
                    }
                }
                while (iterator.hasNext()) {
                    Row sourceRow = iterator.next();
                    if (sourceRow == null) {
                        continue;
                    }
                    records.add(sourceRow);
                }
            }
            if (records.isEmpty()) {
                return null;
            }
            Collections.sort(records, new Comparator<Row>() {
                @Override
                public int compare(Row row1, Row row2) {
                    ColumnDefinition column = columns.get(col);
                    int v = column.compare(MicrosoftDocumentTools.cellString(row1.getCell(col)), MicrosoftDocumentTools.cellString(row2.getCell(col)));
                    return asc ? v : -v;
                }
            });
            int targetRowIndex = 0;
            if (sourceWithNames) {
                Row targetRow = targetSheet.createRow(targetRowIndex++);
                for (int i = 0; i < columns.size(); i++) {
                    Cell targetCell = targetRow.createCell(i);
                    targetCell.setCellValue(columns.get(i).getName());
                }
            }
            for (Row sourceRow : records) {
                Row targetRow = targetSheet.createRow(targetRowIndex++);
                MicrosoftDocumentTools.copyRow(sourceRow, targetRow);
            }
            try ( FileOutputStream fileOut = new FileOutputStream(tmpFile)) {
                targetBook.write(fileOut);
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
            return null;
        }
        FileDeleteTools.delete(tmpDataFile);
        return tmpFile;
    }

    @Override
    protected StringBuilder copyAllSelectedCols() {
        if (sourceFile == null) {
            return null;
        }
        StringBuilder s = null;
        copiedLines = 0;
        try ( Workbook sourceBook = WorkbookFactory.create(sourceFile)) {
            Sheet sourceSheet;
            if (currentSheetName != null) {
                sourceSheet = sourceBook.getSheet(currentSheetName);
            } else {
                sourceSheet = sourceBook.getSheetAt(0);
                currentSheetName = sourceSheet.getSheetName();
            }
            String delimiterString = TextTools.delimiterText(sheetDisplayController.textDelimiter);
            Iterator<Row> iterator = sourceSheet.iterator();
            if (iterator != null && iterator.hasNext()) {
                if (sourceWithNames) {
                    while (iterator.hasNext() && (iterator.next() == null)) {
                    }
                }
                int sourceRowIndex = 0;
                while (iterator.hasNext()) {
                    Row sourceRow = iterator.next();
                    if (sourceRow == null) {
                        continue;
                    }
                    sourceRowIndex++;
                    if (sourceRowIndex < currentPageStart || sourceRowIndex >= currentPageEnd) {
                        String rowString = null;
                        for (int cellIndex = sourceRow.getFirstCellNum(); cellIndex < sourceRow.getLastCellNum(); cellIndex++) {
                            if (colsCheck[cellIndex - sourceRow.getFirstCellNum()].isSelected()) {
                                String cellString = MicrosoftDocumentTools.cellString(sourceRow.getCell(cellIndex));
                                cellString = cellString == null ? "" : cellString;
                                if (rowString == null) {
                                    rowString = cellString;
                                } else {
                                    rowString += delimiterString + cellString;
                                }
                            }
                        }
                        rowString = rowString == null ? "" : rowString;
                        if (s == null) {
                            s = new StringBuilder();
                            s.append(rowString);
                        } else {
                            s.append("\n").append(rowString);
                        }
                        copiedLines++;
                    } else if (sourceRowIndex == currentPageStart) {
                        s = copyPageCols(s, delimiterString);
                    }
                }
            } else {
                s = copyPageCols(s, delimiterString);
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
            return null;
        }
        return s;
    }

    @Override
    protected File deleteFileSelectedCols() {
        if (sourceFile == null) {
            return null;
        }
        File tmpFile = TmpFileTools.getTempFile();
        File tmpDataFile = TmpFileTools.getTempFile();
        FileCopyTools.copyFile(sourceFile, tmpDataFile);
        try ( Workbook sourceBook = WorkbookFactory.create(sourceFile);
                 Workbook targetBook = WorkbookFactory.create(tmpDataFile)) {
            Sheet sourceSheet;
            if (currentSheetName != null) {
                sourceSheet = sourceBook.getSheet(currentSheetName);
            } else {
                sourceSheet = sourceBook.getSheetAt(0);
                currentSheetName = sourceSheet.getSheetName();
            }

            int index = targetBook.getSheetIndex(currentSheetName);
            targetBook.removeSheetAt(index);
            Sheet targetSheet = targetBook.createSheet(currentSheetName);
            targetBook.setSheetOrder(currentSheetName, index);

            Iterator<Row> iterator = sourceSheet.iterator();
            if (iterator != null && iterator.hasNext()) {
                int targetRowIndex = 0;
                if (sourceWithNames) {
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    for (int col = 0; col < colsCheck.length; col++) {
                        if (colsCheck[col].isSelected()) {
                            continue;
                        }
                        Cell targetCell = targetRow.createCell(col);
                        targetCell.setCellValue(colsCheck[col].getText());
                    }
                    while (iterator.hasNext() && (iterator.next() == null)) {
                    }
                }
                while (iterator.hasNext()) {
                    Row sourceRow = iterator.next();
                    if (sourceRow == null) {
                        continue;
                    }
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    int targetCol = 0;
                    Cell targetCell;
                    for (int cellIndex = sourceRow.getFirstCellNum(); cellIndex < sourceRow.getLastCellNum(); cellIndex++) {
                        if (colsCheck[cellIndex - sourceRow.getFirstCellNum()].isSelected()) {
                            continue;
                        }
                        CellType type = CellType.STRING;
                        Cell sourceCell = sourceRow.getCell(cellIndex);
                        if (sourceCell != null) {
                            type = sourceCell.getCellType();
                        }
                        targetCell = targetRow.createCell(targetCol++, type);
                        MicrosoftDocumentTools.copyCell(sourceCell, targetCell, type);
                    }
                }
            }
            try ( FileOutputStream fileOut = new FileOutputStream(tmpFile)) {
                targetBook.write(fileOut);
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
            return null;
        }
        FileDeleteTools.delete(tmpDataFile);
        return tmpFile;
    }

    @Override
    protected File setAllSelectedCols(String value) {
        if (sourceFile == null) {
            return null;
        }
        File tmpFile = TmpFileTools.getTempFile();
        File tmpDataFile = TmpFileTools.getTempFile();
        FileCopyTools.copyFile(sourceFile, tmpDataFile);
        try ( Workbook sourceBook = WorkbookFactory.create(sourceFile);
                 Workbook targetBook = WorkbookFactory.create(tmpDataFile)) {
            Sheet sourceSheet;
            if (currentSheetName != null) {
                sourceSheet = sourceBook.getSheet(currentSheetName);
            } else {
                sourceSheet = sourceBook.getSheetAt(0);
                currentSheetName = sourceSheet.getSheetName();
            }

            int index = targetBook.getSheetIndex(currentSheetName);
            targetBook.removeSheetAt(index);
            Sheet targetSheet = targetBook.createSheet(currentSheetName);
            targetBook.setSheetOrder(currentSheetName, index);

            Iterator<Row> iterator = sourceSheet.iterator();
            if (iterator != null && iterator.hasNext()) {
                int targetRowIndex = 0;
                if (sourceWithNames) {
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    for (int col = 0; col < colsCheck.length; col++) {
                        Cell targetCell = targetRow.createCell(col);
                        targetCell.setCellValue(colsCheck[col].getText());
                    }
                    while (iterator.hasNext() && (iterator.next() == null)) {
                    }
                }
                while (iterator.hasNext()) {
                    Row sourceRow = iterator.next();
                    if (sourceRow == null) {
                        continue;
                    }
                    Row targetRow = targetSheet.createRow(targetRowIndex++);
                    for (int cellIndex = sourceRow.getFirstCellNum(); cellIndex < sourceRow.getLastCellNum(); cellIndex++) {
                        CellType type = CellType.STRING;
                        Cell sourceCell = sourceRow.getCell(cellIndex);
                        if (sourceCell != null) {
                            type = sourceCell.getCellType();
                        }
                        Cell targetCell = targetRow.createCell(cellIndex, type);
                        if (colsCheck[cellIndex - sourceRow.getFirstCellNum()].isSelected()) {
                            MicrosoftDocumentTools.setCell(targetCell, type, value);
                        } else {
                            MicrosoftDocumentTools.copyCell(sourceCell, targetCell, type);
                        }
                    }
                }
            }
            try ( FileOutputStream fileOut = new FileOutputStream(tmpFile)) {
                targetBook.write(fileOut);
            }
        } catch (Exception e) {
            MyBoxLog.error(e);
            return null;
        }
        FileDeleteTools.delete(tmpDataFile);
        return tmpFile;
    }

    public static DataFileExcelController oneOpen() {
        DataFileExcelController controller = null;
        List<Window> windows = new ArrayList<>();
        windows.addAll(Window.getWindows());
        for (Window window : windows) {
            Object object = window.getUserData();
            if (object != null && object instanceof DataFileExcelController) {
                try {
                    controller = (DataFileExcelController) object;
                    controller.toFront();
                    break;
                } catch (Exception e) {
                }
            }
        }
        if (controller == null) {
            controller = (DataFileExcelController) WindowTools.openStage(Fxmls.DataFileExcelFxml);
        }
        return controller;
    }

}
