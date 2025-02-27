package mara.mybox.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.NodeStyleTools;
import mara.mybox.fxml.PopTools;
import mara.mybox.fxml.TextClipboardTools;
import mara.mybox.tools.StringTools;
import mara.mybox.value.Fxmls;
import static mara.mybox.value.Languages.message;

/**
 * @Author Mara
 * @CreateDate 2021-7-22
 * @License Apache License Version 2.0
 */
public class MenuTextEditController extends MenuTextBaseController {

    @FXML
    protected HBox fileBox;
    @FXML
    protected Button findButton, replaceButton;

    public MenuTextEditController() {
        baseTitle = message("Texts");
    }

    @Override
    public void setParameters(BaseController parent, Node node, double x, double y) {
        try {
            super.setParameters(parent, node, x, y);

            if (parent instanceof BaseFileEditorController) {
                BaseFileEditorController e = (BaseFileEditorController) parent;
                if (textInput == null || textInput != e.mainArea) {
                    fileBox.getChildren().remove(0, 2);
                }
            } else {
                fileBox.getChildren().remove(0, 2);
            }
            if (textInput != null) {
                textInput.textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue ov, String oldValue, String newValue) {
                        checkEditPane();
                    }
                });

                textInput.selectionProperty().addListener(new ChangeListener<IndexRange>() {
                    @Override
                    public void changed(ObservableValue ov, IndexRange oldValue, IndexRange newValue) {
                        checkEditPane();
                    }
                });
                checkEditPane();
            }

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    public void checkEditPane() {
        if (textInput == null) {
            return;
        }
        IndexRange range = textInput.getSelection();
        int selection = range != null ? range.getLength() : 0;
        String info = message("Length") + ": " + StringTools.format(textInput.getLength()) + "  ";
        if (selection > 0) {
            info += message("Selection") + ": " + StringTools.format(range.getStart() + 1)
                    + "-" + StringTools.format(range.getEnd()) + "(" + StringTools.format(selection) + ")";
        } else {
            info += message("Cursor") + ": " + StringTools.format(textInput.getAnchor() + 1) + " " + message("Selection") + ": 0";
        }
        bottomLabel.setText(info);
        if (undoButton != null) {
            undoButton.setDisable(!textInput.isEditable() || textInput.isDisable() || !textInput.isUndoable());
        }
        if (redoButton != null) {
            redoButton.setDisable(!textInput.isEditable() || textInput.isDisable() || !textInput.isRedoable());
        }
        boolean selectNone = selection < 1;
        if (cropButton != null) {
            cropButton.setDisable(!textInput.isEditable() || textInput.isDisable() || selectNone);
        }
        if (deleteButton != null) {
            deleteButton.setDisable(!textInput.isEditable() || textInput.isDisable() || selectNone);
        }
        if (clearButton != null) {
            clearButton.setDisable(!textInput.isEditable() || textInput.isDisable());
        }
        if (pasteContentInSystemClipboardButton != null) {
            pasteContentInSystemClipboardButton.setDisable(!textInput.isEditable() || textInput.isDisable()
                    || !TextClipboardTools.systemClipboardHasString());
        }
        boolean empty = textInput.getLength() < 1;
        if (selectAllButton != null) {
            selectAllButton.setDisable(empty);
        }
        if (selectNoneButton != null) {
            selectNoneButton.setDisable(empty);
        }
        if (editButton != null) {
            editButton.setDisable(empty);
        }
        if (copyToSystemClipboardButton != null) {
            copyToSystemClipboardButton.setDisable(empty);
        }
        if (copyToMyBoxClipboardButton != null) {
            copyToMyBoxClipboardButton.setDisable(empty);
        }
        if (saveAsButton != null) {
            saveAsButton.setDisable(empty);
        }
        if (findButton != null) {
            findButton.setDisable(empty);
        }
        if (replaceButton != null) {
            replaceButton.setDisable(empty || !textInput.isEditable() || textInput.isDisable());
        }
        if (TextClipboardTools.isMonitoringCopy()) {
            NodeStyleTools.setTooltip(copyToSystemClipboardButton, new Tooltip(message("CopyToClipboards") + "\nCTRL+c / ALT+c"));
        } else {
            NodeStyleTools.setTooltip(copyToSystemClipboardButton, new Tooltip(message("CopyToSystemClipboard") + "\nCTRL+c / ALT+c"));
        }
    }

    @FXML
    @Override
    public void copyToSystemClipboard() {
        if (textInput == null) {
            return;
        }
        TextClipboardTools.copyToSystemClipboard(myController, textInput);
        checkEditPane();
    }

    @FXML
    @Override
    public void copyToMyBoxClipboard() {
        if (textInput == null) {
            return;
        }
        TextClipboardTools.copyToMyBoxClipboard(myController, textInput);
        checkEditPane();
    }

    @FXML
    @Override
    public void pasteContentInSystemClipboard() {
        if (textInput == null) {
            return;
        }
        textInput.paste();
    }

    @FXML
    @Override
    public void cropAction() {
        if (textInput == null) {
            return;
        }
        textInput.cut();
    }

    @FXML
    @Override
    public void deleteAction() {
        if (textInput == null) {
            return;
        }
        textInput.deleteText(textInput.getSelection());
    }

    @FXML
    @Override
    public void clearAction() {
        if (textInput == null) {
            return;
        }
        textInput.clear();
    }

    @FXML
    @Override
    public void selectAllAction() {
        if (textInput == null) {
            return;
        }
        textInput.selectAll();
    }

    @FXML
    @Override
    public void selectNoneAction() {
        if (textInput == null) {
            return;
        }
        textInput.deselect();
    }

    @FXML
    @Override
    public void undoAction() {
        if (textInput == null) {
            return;
        }
        textInput.undo();
    }

    @FXML
    @Override
    public void redoAction() {
        if (textInput == null) {
            return;
        }
        textInput.redo();
    }

    @FXML
    @Override
    public void saveAction() {
        if (textInput == null || !(parentController instanceof BaseFileEditorController)) {
            return;
        }
        parentController.saveAction();
    }

    @FXML
    @Override
    public void recoverAction() {
        if (textInput == null || !(parentController instanceof BaseFileEditorController)) {
            return;
        }
        parentController.recoverAction();
    }

    @FXML
    @Override
    public boolean popAction() {
        if (textInput == null) {
            return false;
        }
        TextPopController.openInput(parentController, textInput);
        return true;
    }

    /*
        static methods
     */
    public static MenuTextEditController open(BaseController parent, Node node, double x, double y) {
        try {
            if (parent == null || node == null) {
                return null;
            }
            Popup popup = PopTools.popWindow(parent, Fxmls.MenuTextEditFxml, node, x, y);
            if (popup == null) {
                return null;
            }
            Object object = popup.getUserData();
            if (object == null && !(object instanceof MenuTextEditController)) {
                return null;
            }
            MenuTextEditController controller = (MenuTextEditController) object;
            controller.setParameters(parent, node, x, y);
            return controller;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return null;
        }
    }

    public static MenuTextEditController open(BaseController parent, Node node, MouseEvent event) {
        return open(parent, node, event.getScreenX() + 40, event.getScreenY() + 40);
    }

    public static MenuTextEditController open(BaseController parent, Node node, ContextMenuEvent event) {
        return open(parent, node, event.getScreenX() + 40, event.getScreenY() + 40);
    }
}
