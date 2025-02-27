package mara.mybox.controller;

import java.util.ArrayList;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Window;
import mara.mybox.MyBox;
import mara.mybox.fxml.WindowTools;

import mara.mybox.value.Fxmls;

/**
 * @Author Mara
 * @CreateDate 2021-7-29
 * @License Apache License Version 2.0
 */
public abstract class MainMenuController_Window extends MainMenuController_Base {

    @FXML
    protected void showHome(ActionEvent event) {
        openStage(Fxmls.MyboxFxml);
    }

    @FXML
    protected void resetWindows(ActionEvent event) {
        WindowTools.resetWindows();
        refreshInterface();
    }

    @FXML
    protected void fullScreen(ActionEvent event) {
        parentController.getMyStage().setFullScreen(true);
    }

    @FXML
    protected void restart(ActionEvent event) {
        MyBox.restart();
    }

    @FXML
    protected void closeWindow(ActionEvent event) {
        parentController.closeStage();
    }

    @FXML
    protected void closeOtherWindows(ActionEvent event) {
        List<Window> windows = new ArrayList<>();
        windows.addAll(Window.getWindows());
        for (Window window : windows) {
            if (parentController != null) {
                if (!window.equals(parentController.getMyStage())) {
                    window.hide();
                }
            } else {
                if (!window.equals(myStage)) {
                    window.hide();
                }
            }
        }
    }

    @Override
    public BaseController refreshInterface() {
        parentController.refreshInterface();
        return super.refreshInterface();
    }

    @FXML
    @Override
    public BaseController refreshInterfaceAndFile() {
        parentController.refreshInterfaceAndFile();
        return super.refreshInterface();
    }

    @FXML
    @Override
    public BaseController reload() {
        return parentController.reload();
    }

    @FXML
    protected void exit(ActionEvent event) {
        WindowTools.appExit();
    }

}
