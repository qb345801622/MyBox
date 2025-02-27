package mara.mybox.controller;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import mara.mybox.db.DerbyBase;
import mara.mybox.db.data.ColumnDefinition.ColumnType;
import mara.mybox.db.data.ConvolutionKernel;
import mara.mybox.db.data.DataDefinition.DataType;
import mara.mybox.db.data.Matrix;
import mara.mybox.db.data.MatrixCell;
import mara.mybox.db.table.BaseTable;
import mara.mybox.db.table.TableMatrixCell;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.NodeStyleTools;
import mara.mybox.tools.DoubleTools;
import mara.mybox.value.AppValues;
import mara.mybox.value.Languages;
import mara.mybox.value.UserConfig;

/**
 * @Author Mara
 * @CreateDate 2020-12-17
 * @License Apache License Version 2.0
 */
public class ControlMatrix extends ControlSheet {

    protected ControlMatricesList manager;
    protected TableMatrixCell tableMatrixCell;
    protected BaseTable tableMatrix;
    protected String matrixName;

    @FXML
    protected TextField nameInput, idInput;
    @FXML
    protected ComboBox<String> columnsNumberSelector, rowsNumberSelector;
    @FXML
    protected TextArea commentsArea;
    @FXML
    protected CheckBox autoNameCheck;
    @FXML
    protected Button save2Button;

    public ControlMatrix() {
        baseTitle = Languages.message("MatrixEdit");
        dataType = DataType.Matrix;
        colPrefix = "Column";
        defaultColumnType = ColumnType.Double;
        defaultColValue = "0";
        defaultColNotNull = true;
    }

    public void initManager(ControlMatricesList manager) {
        this.manager = manager;
        baseName = manager.baseName;
        tableMatrix = manager.tableDefinition;
        initDisplay(this);
    }

    public void initDisplay(ControlSheet parent) {
        try {
            colsNumber = UserConfig.getInt(baseName + "ColsNumber", 3);
            rowsNumber = UserConfig.getInt(baseName + "RowsNumber", 3);
            scale = (short) UserConfig.getInt(baseName + "Scale", 2);
            maxRandom = UserConfig.getInt(baseName + "MaxRandom", 1);
            nameInput.setText(rowsNumber + "x" + colsNumber);

            if (colsNumber <= 0) {
                colsNumber = 3;
                UserConfig.setInt(baseName + "ColsNumber", colsNumber);
            }
            if (rowsNumber <= 0) {
                rowsNumber = 3;
                UserConfig.setInt(baseName + "RowsNumber", rowsNumber);
            }
            if (scale < 0) {
                scale = 2;
                UserConfig.setInt(baseName + "Scale", scale);
            }
            makeSheet(new String[rowsNumber][colsNumber], false);

            autoNameCheck.setSelected(UserConfig.getBoolean(baseName + "AutoName", true));
            autoNameCheck.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                UserConfig.setBoolean(baseName + "AutoName", newValue);
            });

            columnsNumberSelector.getItems().addAll(
                    Arrays.asList("3", "5", "1", "6", "2", "8", "4", "9", "10", "12", "15", "7")
            );
            columnsNumberSelector.setValue(colsNumber + "");
            columnsNumberSelector.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends String> ov, String oldValue, String newValue) -> {
                if (isSettingValues) {
                    return;
                }
                try {
                    int v = Integer.parseInt(newValue);
                    if (v > 0) {
                        colsNumber = v;
                        UserConfig.setInt(baseName + "ColsNumber", v);
                        columnsNumberSelector.getEditor().setStyle(null);
                        if (autoNameCheck.isSelected()) {
                            nameInput.setText(rowsNumber + "x" + colsNumber);
                        }
                        resizeSheet(rowsNumber, colsNumber);
                    } else {
                        columnsNumberSelector.getEditor().setStyle(NodeStyleTools.badStyle);
                    }
                } catch (Exception e) {
                    columnsNumberSelector.getEditor().setStyle(NodeStyleTools.badStyle);
                }
            });

            rowsNumberSelector.getItems().addAll(
                    Arrays.asList("3", "5", "1", "6", "2", "8", "4", "9", "10", "12", "15", "7")
            );
            rowsNumberSelector.setValue(rowsNumber + "");
            rowsNumberSelector.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends String> ov, String oldValue, String newValue) -> {
                if (isSettingValues) {
                    return;
                }
                try {
                    int v = Integer.parseInt(newValue);
                    if (v > 0) {
                        rowsNumber = v;
                        UserConfig.setInt(baseName + "RowsNumber", v);
                        rowsNumberSelector.getEditor().setStyle(null);
                        if (autoNameCheck.isSelected()) {
                            nameInput.setText(rowsNumber + "x" + colsNumber);
                        }
                        resizeSheet(rowsNumber, colsNumber);
                    } else {
                        rowsNumberSelector.getEditor().setStyle(NodeStyleTools.badStyle);
                    }
                } catch (Exception e) {
                    rowsNumberSelector.getEditor().setStyle(NodeStyleTools.badStyle);
                }
            });

            scaleSelector.getItems().addAll(
                    Arrays.asList("2", "1", "0", "3", "4", "5", "6", "7", "8", "10", "12", "15")
            );
            scaleSelector.setValue(scale + "");
            scaleSelector.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends String> ov, String oldValue, String newValue) -> {
                if (isSettingValues) {
                    return;
                }
                try {
                    int v = Integer.parseInt(newValue);
                    if (v >= 0 && v <= 15) {
                        scale = (short) v;
                        UserConfig.setInt(baseName + "Scale", v);
                        scaleSelector.getEditor().setStyle(null);
                        scaleMatrix();
                    } else {
                        scaleSelector.getEditor().setStyle(NodeStyleTools.badStyle);
                    }
                } catch (Exception e) {
                    scaleSelector.getEditor().setStyle(NodeStyleTools.badStyle);
                }
            });

            randomSelector.getItems().addAll(
                    Arrays.asList("1", "100", "10", "1000", "10000")
            );
            randomSelector.setValue(maxRandom + "");
            randomSelector.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends String> ov, String oldValue, String newValue) -> {
                if (isSettingValues) {
                    return;
                }
                try {
                    int v = Integer.parseInt(newValue);
                    if (v > 0) {
                        maxRandom = v;
                        UserConfig.setInt(baseName + "MaxRandom", v);
                        randomSelector.getEditor().setStyle(null);
                    } else {
                        randomSelector.getEditor().setStyle(NodeStyleTools.badStyle);
                    }
                } catch (Exception e) {
                    randomSelector.getEditor().setStyle(NodeStyleTools.badStyle);
                }
            });

            save2Button.disableProperty().bind(saveButton.disableProperty());

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @Override
    protected String titleName() {
        return nameInput.getText();
    }

    public void loadMatrix(Matrix matrix) {
        if (!checkBeforeNextAction()) {
            return;
        }
        synchronized (this) {
            columns = null;
            sheetInputs = null;
            colsNumber = matrix.getColsNumber();
            rowsNumber = matrix.getRowsNumber();
            scale = matrix.getScale();
            isSettingValues = true;
            nameInput.setText(matrix.getName());
            commentsArea.setText(matrix.getComments());
            columnsNumberSelector.setValue(matrix.getColsNumber() + "");
            rowsNumberSelector.setValue(matrix.getRowsNumber() + "");
            scaleSelector.setValue(matrix.getScale() + "");
            isSettingValues = false;
            if (matrix.getId() >= 0) {
                idInput.setText(matrix.getId() + "");
            } else {
                idInput.clear();
                loadMatrix(new double[rowsNumber][colsNumber]);
                return;
            }
            task = new SingletonTask<Void>() {
                private double[][] values;

                @Override
                protected boolean handle() {
                    try ( Connection conn = DerbyBase.getConnection()) {
                        values = new double[rowsNumber][colsNumber];
                        if (tableMatrixCell == null) {
                            tableMatrixCell = new TableMatrixCell();
                        }
                        List<MatrixCell> data = tableMatrixCell.query(conn,
                                "SELECT * FROM " + tableMatrixCell.getTableName() + " WHERE mcxid=" + matrix.getId());
                        for (MatrixCell cell : data) {
                            if (cell.getCol() < colsNumber && cell.getRow() < rowsNumber) {
                                values[cell.getRow()][cell.getCol()] = cell.getValue();
                            }
                        }
                    } catch (Exception e) {
                        error = e.toString();
                        MyBoxLog.console(error);
                        return false;
                    }
                    return true;
                }

                @Override
                protected void whenSucceeded() {
                    loadMatrix(values);
                }
            };
//            handling(task);
            task.setSelf(task);
            Thread thread = new Thread(task);
            thread.setDaemon(false);
            thread.start();
        }

    }

    protected void loadMatrix(double[][] matrix) {
        String[][] values = null;
        try {
            rowsNumber = matrix.length;
            colsNumber = matrix[0].length;
            values = new String[rowsNumber][colsNumber];
            for (int row = 0; row < rowsNumber; ++row) {
                for (int col = 0; col < colsNumber; ++col) {
                    try {
                        values[row][col] = DoubleTools.format(matrix[row][col], scale);
                    } catch (Exception e) {
                        values[row][col] = defaultColValue;
                    }
                }
            }
        } catch (Exception e) {
        }
        makeSheet(values, false);
    }

    protected double[][] matrix() {
        if (sheetInputs == null) {
            return null;
        }
        double[][] matrix = new double[sheetInputs.length][sheetInputs[0].length];
        for (int j = 0; j < rowsNumber; j++) {
            for (int i = 0; i < colsNumber; i++) {
                double d = dvalue(j, i);
                if (d != AppValues.InvalidDouble) {
                    matrix[j][i] = d;
                }
            }
        }
        return matrix;
    }

    protected double dvalue(int row, int col) {
        try {
            double d = Double.parseDouble(cellString(row, col));
            d = DoubleTools.scale(d, scale);
            return d;
        } catch (Exception e) {
            return AppValues.InvalidDouble;
        }
    }

    public void scaleMatrix() {
        String[][] values = null;
        try {
            values = new String[rowsNumber][colsNumber];
            for (int j = 0; j < rowsNumber; ++j) {
                for (int i = 0; i < colsNumber; ++i) {
                    double d = dvalue(j, i);
                    if (d != AppValues.InvalidDouble) {
                        values[j][i] = d + "";
                    } else {
                        values[j][i] = defaultColValue;
                    }
                }
            }
        } catch (Exception e) {
        }
        makeSheet(values);
    }

    @Override
    public void sheetChanged(boolean changed) {
        if (isSettingValues) {
            return;
        }
        if (sheetInputs != null) {
            rowsNumber = sheetInputs.length;
            colsNumber = sheetInputs[0].length;
        } else {
            rowsNumber = 0;
            colsNumber = 0;
        }
        isSettingValues = true;
        rowsNumberSelector.setValue(rowsNumber + "");
        columnsNumberSelector.setValue(colsNumber + "");
        isSettingValues = false;
        super.sheetChanged(changed);
    }

    @FXML
    @Override
    public void createAction() {
        try {
            if (!checkBeforeNextAction()) {
                return;
            }
            idInput.clear();
            manager.tableView.getSelectionModel().clearSelection();
            popInformation(Languages.message("NewMatrix"));

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @FXML
    @Override
    public void saveAction() {
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {

                private Matrix matrix;
                private long id = -1;

                @Override
                protected boolean handle() {
                    try ( Connection conn = DerbyBase.getConnection()) {
                        matrix = new Matrix();
                        matrix.setColsNumber(colsNumber);
                        matrix.setRowsNumber(rowsNumber);
                        matrix.setScale(scale);
                        matrix.setName(nameInput.getText().trim());
                        String comments = commentsArea.getText();
                        matrix.setComments(comments == null || comments.isBlank() ? null : comments.trim());
                        matrix.setModifyTime(new Date());
                        try {
                            id = Long.parseLong(idInput.getText());
                        } catch (Exception e) {
                        }
                        if (id < 0) {
                            if (tableMatrix.insertData(conn, matrix) == null) {
                                return false;
                            }
                            id = tableMatrix.getNewID();
                            if (id < 0) {
                                return false;
                            }
                            matrix.setId(id);
                        } else {
                            matrix.setId(id);
                            if (tableMatrix.updateData(conn, matrix) == null) {
                                return false;
                            }
                        }
                        if (tableMatrixCell == null) {
                            tableMatrixCell = new TableMatrixCell();
                        }
                        tableMatrixCell.update(conn, "DELETE FROM " + tableMatrixCell.getTableName() + " WHERE mcxid=" + id);
                        List<MatrixCell> data = new ArrayList<>();
                        for (int j = 0; j < rowsNumber; ++j) {
                            for (int i = 0; i < colsNumber; ++i) {
                                double d = 0d;
                                try {
                                    d = Double.parseDouble(cellString(j, i));
                                    d = DoubleTools.scale(d, scale);
                                } catch (Exception e) {
                                }
                                MatrixCell cell = MatrixCell.create()
                                        .setMcxid(id).setCol(i).setRow(j).setValue(d);
                                data.add(cell);
                            }
                        }
                        tableMatrixCell.insertList(conn, data);
                        return true;
                    } catch (Exception e) {
                        error = e.toString();
                        return false;
                    }
                }

                @Override
                protected void whenSucceeded() {
                    manager.loadTableData();
                    idInput.setText(id + "");
                    popSuccessful();
                    dataChanged(false);
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
    public void copyMatrixAction() {
//        dataChanged(false);
        createAction();
        nameInput.setText(nameInput.getText() + " " + Languages.message("Copy"));
//        saveAction();
    }

    public void dataChanged(boolean changed) {

    }

    public List<MenuItem> equalMoreMenu() {
        List<MenuItem> items = new ArrayList<>();
        try {
            MenuItem menu = new MenuItem(Languages.message("SetAllZero"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    for (int i = 0; i < colsNumber; ++i) {
                        sheetInputs[j][i].setText(0d + "");
                    }
                }
                isSettingValues = false;
                if (autoNameCheck.isSelected()) {
                    nameInput.setText(rowsNumber + "x" + colsNumber + " 0");
                }
                sheetChanged();
            });
            menu.setDisable(sheetInputs == null);
            items.add(menu);

            menu = new MenuItem(Languages.message("SetAllOne"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    for (int i = 0; i < colsNumber; ++i) {
                        sheetInputs[j][i].setText(1d + "");
                    }
                }
                isSettingValues = false;
                if (autoNameCheck.isSelected()) {
                    nameInput.setText(rowsNumber + "x" + colsNumber + " 1");
                }
                sheetChanged();
            });
            menu.setDisable(sheetInputs == null);
            items.add(menu);

            menu = new MenuItem(Languages.message("SetAllRandom"));
            menu.setOnAction((ActionEvent event) -> {
                Random r = new Random();
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    for (int i = 0; i < colsNumber; ++i) {
                        sheetInputs[j][i].setText(DoubleTools.format(DoubleTools.random(r, maxRandom), scale));
                    }
                }
                isSettingValues = false;
                if (autoNameCheck.isSelected()) {
                    nameInput.setText(rowsNumber + "x" + colsNumber + " " + Languages.message("Random"));
                }
                sheetChanged();
            });
            menu.setDisable(sheetInputs == null);
            items.add(menu);

            menu = new MenuItem(Languages.message("Normalization"));
            menu.setOnAction((ActionEvent event) -> {
                double sum = 0d;
                for (int j = 0; j < rowsNumber; ++j) {
                    for (int i = 0; i < colsNumber; ++i) {
                        double d = dvalue(j, i);
                        if (d == AppValues.InvalidDouble) {
                            sheetInputs[j][i].setStyle(NodeStyleTools.badStyle);
                            popError(Languages.message("InvalidData"));
                            return;
                        }
                        sum += d;
                    }
                }
                if (sum == 0) {
                    return;
                }
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    for (int i = 0; i < colsNumber; ++i) {
                        double d = dvalue(j, i);
                        sheetInputs[j][i].setText(DoubleTools.format(d / sum, scale));
                    }
                }
                isSettingValues = false;
                if (autoNameCheck.isSelected()) {
                    nameInput.setText(rowsNumber + "x" + colsNumber + " " + Languages.message("Normalization"));
                }
                sheetChanged();
            });
            menu.setDisable(sheetInputs == null);
            items.add(menu);

            menu = new MenuItem(Languages.message("GaussianDistribution"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                float[][] m = ConvolutionKernel.makeGaussMatrix(rowsNumber / 2);
                for (int j = 0; j < rowsNumber; ++j) {
                    for (int i = 0; i < colsNumber; ++i) {
                        sheetInputs[j][i].setText(DoubleTools.format(m[j][i], scale));
                    }
                }
                isSettingValues = false;
                if (autoNameCheck.isSelected()) {
                    nameInput.setText(rowsNumber + "x" + colsNumber + " " + Languages.message("GaussianDistribution"));
                }
                sheetChanged();
            });
            menu.setDisable(sheetInputs == null || colsNumber != rowsNumber || colsNumber < 3);
            items.add(menu);

            menu = new MenuItem(Languages.message("SetAsIdentifyMatrix"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    for (int i = 0; i < colsNumber; ++i) {
                        if (i == j) {
                            sheetInputs[j][i].setText(1d + "");
                        } else {
                            sheetInputs[j][i].setText(0d + "");
                        }
                    }
                }
                isSettingValues = false;
                if (autoNameCheck.isSelected()) {
                    nameInput.setText(rowsNumber + "x" + colsNumber + " " + Languages.message("IdentifyMatrix"));
                }
                sheetChanged();
            });
            menu.setDisable(sheetInputs == null || colsNumber != rowsNumber);
            items.add(menu);

            menu = new MenuItem(Languages.message("SetAsUpperTriangleMatrix"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    for (int i = 0; i < colsNumber; ++i) {
                        if (i < j) {
                            sheetInputs[j][i].setText(0d + "");
                        }
                    }
                }
                isSettingValues = false;
                if (autoNameCheck.isSelected()) {
                    nameInput.setText(rowsNumber + "x" + colsNumber + " " + Languages.message("UpperTriangle"));
                }
                sheetChanged();
            });
            menu.setDisable(sheetInputs == null || colsNumber != rowsNumber);
            items.add(menu);

            menu = new MenuItem(Languages.message("SetAsLowerTriangleMatrix"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    for (int i = 0; i < colsNumber; ++i) {
                        if (i > j) {
                            sheetInputs[j][i].setText(0d + "");
                        }
                    }
                }
                isSettingValues = false;
                if (autoNameCheck.isSelected()) {
                    nameInput.setText(rowsNumber + "x" + colsNumber + " " + Languages.message("LowerTriangle"));
                }
                sheetChanged();
            });
            menu.setDisable(sheetInputs == null || colsNumber != rowsNumber);
            items.add(menu);

            boolean rowsSelected = false;
            if (rowsCheck != null) {
                for (int j = 0; j < rowsCheck.length; ++j) {
                    if (rowsCheck[j].isSelected()) {
                        rowsSelected = true;
                        break;
                    }
                }
            }
            boolean colsSelected = false;
            if (colsCheck != null) {
                for (int j = 0; j < colsCheck.length; ++j) {
                    if (colsCheck[j].isSelected()) {
                        colsSelected = true;
                        break;
                    }
                }
            }

            items.add(new SeparatorMenuItem());

            menu = new MenuItem(Languages.message("SetSelectedRowsZero"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    if (!rowsCheck[j].isSelected()) {
                        continue;
                    }
                    for (int i = 0; i < colsNumber; ++i) {
                        sheetInputs[j][i].setText(0d + "");
                    }
                }
                isSettingValues = false;
                sheetChanged();
            });
            menu.setDisable(!rowsSelected);
            items.add(menu);

            menu = new MenuItem(Languages.message("SetSelectedRowsOne"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    if (!rowsCheck[j].isSelected()) {
                        continue;
                    }
                    for (int i = 0; i < colsNumber; ++i) {
                        sheetInputs[j][i].setText(1d + "");
                    }
                }
                isSettingValues = false;
                sheetChanged();
            });
            menu.setDisable(!rowsSelected);
            items.add(menu);

            menu = new MenuItem(Languages.message("SetSelectedRowsRandom"));
            menu.setOnAction((ActionEvent event) -> {
                Random r = new Random();
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    if (!rowsCheck[j].isSelected()) {
                        continue;
                    }
                    for (int i = 0; i < colsNumber; ++i) {
                        sheetInputs[j][i].setText(DoubleTools.format(DoubleTools.random(r, maxRandom), scale));
                    }
                }
                isSettingValues = false;
                sheetChanged();
            });
            menu.setDisable(!rowsSelected);
            items.add(menu);

            items.add(new SeparatorMenuItem());

            menu = new MenuItem(Languages.message("SetSelectedColsZero"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                for (int i = 0; i < colsNumber; ++i) {
                    if (!colsCheck[i].isSelected()) {
                        continue;
                    }
                    for (int j = 0; j < rowsNumber; ++j) {
                        sheetInputs[j][i].setText(0d + "");
                    }
                }
                isSettingValues = false;
                sheetChanged();
            });
            menu.setDisable(!colsSelected);
            items.add(menu);

            menu = new MenuItem(Languages.message("SetSelectedColsOne"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                for (int i = 0; i < colsNumber; ++i) {
                    if (!colsCheck[i].isSelected()) {
                        continue;
                    }
                    for (int j = 0; j < rowsNumber; ++j) {
                        sheetInputs[j][i].setText(1d + "");
                    }
                }
                isSettingValues = false;
                sheetChanged();
            });
            menu.setDisable(!colsSelected);
            items.add(menu);

            menu = new MenuItem(Languages.message("SetSelectedColsRandom"));
            menu.setOnAction((ActionEvent event) -> {
                Random r = new Random();
                isSettingValues = true;
                for (int i = 0; i < colsNumber; ++i) {
                    if (!colsCheck[i].isSelected()) {
                        continue;
                    }
                    for (int j = 0; j < rowsNumber; ++j) {
                        sheetInputs[j][i].setText(DoubleTools.format(DoubleTools.random(r, maxRandom), scale));
                    }
                }
                isSettingValues = false;
                sheetChanged();
            });
            menu.setDisable(!colsSelected);
            items.add(menu);

            items.add(new SeparatorMenuItem());

            menu = new MenuItem(Languages.message("SetSelectedRowsColsZero"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    if (!rowsCheck[j].isSelected()) {
                        continue;
                    }
                    for (int i = 0; i < colsNumber; ++i) {
                        if (!colsCheck[i].isSelected()) {
                            continue;
                        }
                        sheetInputs[j][i].setText(0d + "");
                    }
                }
                isSettingValues = false;
                sheetChanged();
            });
            menu.setDisable(!colsSelected || !rowsSelected);
            items.add(menu);

            menu = new MenuItem(Languages.message("SetSelectedRowsColsOne"));
            menu.setOnAction((ActionEvent event) -> {
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    if (!rowsCheck[j].isSelected()) {
                        continue;
                    }
                    for (int i = 0; i < colsNumber; ++i) {
                        if (!colsCheck[i].isSelected()) {
                            continue;
                        }
                        sheetInputs[j][i].setText(1d + "");
                    }
                }
                isSettingValues = false;
                sheetChanged();
            });
            menu.setDisable(!colsSelected || !rowsSelected);
            items.add(menu);

            menu = new MenuItem(Languages.message("SetSelectedRowsColsRandom"));
            menu.setOnAction((ActionEvent event) -> {
                Random r = new Random();
                isSettingValues = true;
                for (int j = 0; j < rowsNumber; ++j) {
                    if (!rowsCheck[j].isSelected()) {
                        continue;
                    }
                    for (int i = 0; i < colsNumber; ++i) {
                        if (!colsCheck[i].isSelected()) {
                            continue;
                        }
                        sheetInputs[j][i].setText(DoubleTools.format(DoubleTools.random(r, maxRandom), scale));
                    }
                }
                isSettingValues = false;
                sheetChanged();
            });
            menu.setDisable(!colsSelected || !rowsSelected);
            items.add(menu);

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
        return items;
    }

    @Override
    protected boolean saveColumns() {
        return true;
    }

    @Override
    public void copyCols(List<Integer> cols, boolean withNames, boolean toSystemClipboard) {
        copyRowsCols(rowsIndex(true), cols, withNames, toSystemClipboard);
    }

    @Override
    public void setCols(List<Integer> cols, String value) {
        setRowsCols(rowsIndex(true), cols, value);
    }

    @Override
    public void sort(int col, boolean asc) {
        sortRows(rowsIndex(true), col, asc);
    }

    @Override
    public void cleanPane() {
        try {
            manager = null;
            tableMatrixCell = null;
            tableMatrix = null;
        } catch (Exception e) {
        }
        super.cleanPane();
    }

    /*
        get/set
     */
    public int getColsNumber() {
        return colsNumber;
    }

    public void setColsNumber(int colsNumber) {
        this.colsNumber = colsNumber;
    }

    public int getRowsNumber() {
        return rowsNumber;
    }

    public void setRowsNumber(int rowsNumber) {
        this.rowsNumber = rowsNumber;
    }

}
