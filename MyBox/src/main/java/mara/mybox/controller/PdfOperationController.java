/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mara.mybox.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author mara
 */
public class PdfOperationController extends BaseController {

    protected PdfBaseController parentController;

    @FXML
    protected Button startButton;
    @FXML
    protected Button pauseButton;
    @FXML
    protected ProgressBar progressBar;
    @FXML
    protected Label progressValue;
    @FXML
    protected Button openTargetButton;
    @FXML
    protected TextField statusLabel;
    @FXML
    protected ProgressBar fileProgressBar;
    @FXML
    protected Label fileProgressValue;

    @Override
    protected void initializeNext() {

    }

    @FXML
    protected void startProcess(ActionEvent event) {
        if (parentController != null) {
            parentController.startProcess(event);
        }
    }

    @FXML
    protected void pauseProcess(ActionEvent event) {
        if (parentController != null) {
            parentController.startProcess(event);
        }
    }

    @FXML
    protected void openTarget(ActionEvent event) {
        if (parentController != null) {
            parentController.openTarget(event);
        }
    }

    public Button getStartButton() {
        return startButton;
    }

    public void setStartButton(Button startButton) {
        this.startButton = startButton;
    }

    public Button getPauseButton() {
        return pauseButton;
    }

    public void setPauseButton(Button pauseButton) {
        this.pauseButton = pauseButton;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public Label getProgressValue() {
        return progressValue;
    }

    public void setProgressValue(Label progressValue) {
        this.progressValue = progressValue;
    }

    public Button getOpenTargetButton() {
        return openTargetButton;
    }

    public void setOpenTargetButton(Button openTargetButton) {
        this.openTargetButton = openTargetButton;
    }

    public TextField getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(TextField statusLabel) {
        this.statusLabel = statusLabel;
    }

    public PdfBaseController getParentController() {
        return parentController;
    }

    public void setParentController(PdfBaseController parentController) {
        this.parentController = parentController;
    }

}
