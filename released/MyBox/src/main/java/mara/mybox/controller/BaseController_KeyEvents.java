package mara.mybox.controller;

import javafx.event.EventTarget;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.NodeTools;
import mara.mybox.fxml.TextClipboardTools;
import mara.mybox.fxml.WebViewTools;
import mara.mybox.value.AppVariables;

/**
 * @Author Mara
 * @CreateDate 2021-7-27
 * @License Apache License Version 2.0
 */
public abstract class BaseController_KeyEvents extends BaseController_Actions {

    private KeyEvent keyEvent;

    public void monitorKeyEvents() {
        try {
            if (thisPane != null) {
                thisPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    keyEvent = event;
                    if (keyEventsFilter(event)) {
//                        MyBoxLog.debug("consume:" + this.getClass() + " text:" + event.getText() + " code:" + event.getCode());
                        event.consume();
                    }
                    keyEvent = null;
                });
            }
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    // return whether handled
    public boolean keyEventsFilter(KeyEvent event) {
//        MyBoxLog.debug("filter:" + this.getClass() + " text:" + event.getText() + " code:" + event.getCode()
//                + " source:" + event.getSource().getClass() + " target:" + event.getTarget().getClass());
        if (event.isControlDown()) {
            return controlAltFilter(event);

        } else if (event.isAltDown()) {
            return altFilter(event);

        } else if (event.getCode() != null) {
            return keyFilter(event);

        }
        return false;
    }

    public boolean altFilter(KeyEvent event) {
        if (!event.isAltDown() || event.getCode() == null) {
            return false;
        }
        switch (event.getCode()) {
            case HOME:
                return altHome();
            case END:
                return altEnd();
            case PAGE_UP:
                return altPageUp();
            case PAGE_DOWN:
                return altPageDown();
        }
        return controlAltFilter(event);
    }

    public boolean altPageUp() {
        if (previousButton != null && !previousButton.isDisabled() && previousButton.isVisible()) {
            previousAction();
            return true;
        } else if (pagePreviousButton != null && !pagePreviousButton.isDisabled() && pagePreviousButton.isVisible()) {
            pagePreviousAction();
            return true;
        }
        return false;
    }

    public boolean altPageDown() {
        if (nextButton != null && !nextButton.isDisabled() && nextButton.isVisible()) {
            nextAction();
        } else if (pageNextButton != null && !pageNextButton.isDisabled() && pageNextButton.isVisible()) {
            pageNextAction();
        }
        return false;
    }

    public boolean altHome() {
        if (firstButton != null && !firstButton.isDisabled() && firstButton.isVisible()) {
            firstAction();
            return true;
        } else if (pageFirstButton != null && !pageFirstButton.isDisabled() && pageFirstButton.isVisible()) {
            pageFirstAction();
            return true;
        }
        return false;
    }

    public boolean altEnd() {
        if (lastButton != null && !lastButton.isDisabled() && lastButton.isVisible()) {
            lastAction();
            return true;
        } else if (pageLastButton != null && !pageLastButton.isDisabled() && pageLastButton.isVisible()) {
            pageLastAction();
            return true;
        }
        return false;
    }

    public boolean controlAltFilter(KeyEvent event) {
        if (event.getCode() == null) {
            return false;
        }
        switch (event.getCode()) {
            case E:
                return controlAltE();

            case N:
                return controlAltN();

            case C:
                return controlAltC();

            case V:
                return controlAltV();

            case A:
                return controlAltA();

            case D:
                return controlAltD();

            case Z:
                return controlAltZ();

            case Y:
                return controlAltY();

            case O:
                return controlAltO();

            case X:
                return controlAltX();

            case R:
                return controlAltR();

            case S:
                return controlAltS();

            case F:
                return controlAltF();

            case H:
                return controlAltH();

            case T:
                return controlAltT();

            case G:
                return controlAltG();

            case B:
                return controlAltB();

            case I:
                return controlAltI();

            case P:
                return controlAltP();

            case W:
                return controlAltW();

            case M:
                return controlAltM();

            case J:
                return controlAltJ();

            case Q:
                return controlAltQ();

            case K:
                return controlAltK();

            case U:
                return controlAltU();

            case MINUS:
                setSceneFontSize(AppVariables.sceneFontSize - 1);
                return true;

            case EQUALS:
                setSceneFontSize(AppVariables.sceneFontSize + 1);
                return true;

            case DIGIT1:
                return controlAlt1();

            case DIGIT2:
                return controlAlt2();

            case DIGIT3:
                return controlAlt3();

            case DIGIT4:
                return controlAlt4();

        }
        return false;
    }

    // Shortcuts like PageDown/PageUp/Home/End/Ctrl-c/v/x/z/y/a should work for text editing preferentially
    public boolean defaultHandled() {
        if (keyEvent == null) {
            return false;
        }
        return NodeTools.isTextInput(keyEvent.getTarget()) != null;
    }

    public boolean controlAltCTextInput() {
        if (keyEvent == null) {
            return false;
        }
        EventTarget target = keyEvent.getTarget();
        if (target != null) {
            if (target instanceof TextInputControl) {
                TextClipboardTools.copyToSystemClipboard(myController, (TextInputControl) target);
                return true;
            }
            if (target instanceof ComboBox) {
                ComboBox cb = (ComboBox) target;
                if (cb.isEditable()) {
                    TextClipboardTools.copyToSystemClipboard(myController, cb.getEditor());
                    return true;
                }
            }
            if (target instanceof WebView) {
                WebView webview = (WebView) target;
                TextClipboardTools.copyToSystemClipboard(myController, WebViewTools.selectedText(webview.getEngine()));
                return true;
            }
        }
        return false;
    }

    public boolean controlAltC() {
        if (controlAltCTextInput()) {
            return true;
        }
        if (copyButton != null) {
            if (!copyButton.isDisabled() && copyButton.isVisible()) {
                copyAction();
            }
            return true;
        } else if (copyToSystemClipboardButton != null) {
            if (!copyToSystemClipboardButton.isDisabled() && copyToSystemClipboardButton.isVisible()) {
                copyToSystemClipboard();
            }
            return true;
        } else if (copyToMyBoxClipboardButton != null) {
            if (!copyToMyBoxClipboardButton.isDisabled() && copyToMyBoxClipboardButton.isVisible()) {
                copyToMyBoxClipboard();
            }
            return true;
        }
        return false;

    }

    public boolean controlAltV() {
        if (defaultHandled()) {
            return false;
        }
        if (pasteButton != null) {
            if (!pasteButton.isDisabled() && pasteButton.isVisible()) {
                pasteAction();
            }
            return true;
        } else if (pasteContentInSystemClipboardButton != null) {
            if (!pasteContentInSystemClipboardButton.isDisabled() && pasteContentInSystemClipboardButton.isVisible()) {
                pasteContentInSystemClipboard();
            }
            return true;
        } else if (loadContentInSystemClipboardButton != null) {
            if (!loadContentInSystemClipboardButton.isDisabled() && loadContentInSystemClipboardButton.isVisible()) {
                loadContentInSystemClipboard();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltA() {
        if (defaultHandled()) {
            return false;
        }
        if (allButton != null) {
            if (!allButton.isDisabled() && allButton.isVisible()) {
                allAction();
            }
            return true;
        } else if (selectAllButton != null) {
            if (!selectAllButton.isDisabled() && selectAllButton.isVisible()) {
                selectAllAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltE() {
        if (startButton != null) {
            if (!startButton.isDisabled() && startButton.isVisible()) {
                startAction();
            }
            return true;
        } else if (okButton != null) {
            if (!okButton.isDisabled() && okButton.isVisible()) {
                okAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltN() {
        if (createButton != null) {
            if (!createButton.isDisabled() && createButton.isVisible()) {
                createAction();
            }
            return true;
        } else if (addButton != null) {
            if (!addButton.isDisabled() && addButton.isVisible()) {
                addAction(null);
            }
            return true;
        }
        return false;
    }

    public boolean controlAltS() {
        if (saveButton != null) {
            if (!saveButton.isDisabled() && saveButton.isVisible()) {
                saveAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltB() {
        if (saveAsButton != null) {
            if (!saveAsButton.isDisabled() && saveAsButton.isVisible()) {
                saveAsAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltI() {
        if (infoButton != null) {
            if (!infoButton.isDisabled() && infoButton.isVisible()) {
                infoAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltD() {
        if (defaultHandled()) {
            return false;
        }
        if (deleteButton != null) {
            if (!deleteButton.isDisabled() && deleteButton.isVisible()) {
                deleteAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltO() {
        if (selectNoneButton != null) {
            if (!selectNoneButton.isDisabled() && selectNoneButton.isVisible()) {
                selectNoneAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltX() {
        if (defaultHandled()) {
            return false;
        }
        if (cropButton != null) {
            if (!cropButton.isDisabled() && cropButton.isVisible()) {
                cropAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltG() {
        if (clearButton != null) {
            if (!clearButton.isDisabled() && clearButton.isVisible()) {
                clearAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltR() {
        if (recoverButton != null) {
            if (!recoverButton.isDisabled() && recoverButton.isVisible()) {
                recoverAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltZ() {
        if (defaultHandled()) {
            return false;
        }
        if (undoButton != null) {
            if (!undoButton.isDisabled() && undoButton.isVisible()) {
                undoAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltY() {
        if (defaultHandled()) {
            return false;
        }
        if (redoButton != null) {
            if (!redoButton.isDisabled() && redoButton.isVisible()) {
                redoAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltF() {
        findAction();
        return true;
    }

    public boolean controlAltH() {
        replaceAction();
        return true;
    }

    public boolean controlAltT() {
        return false;
    }

    public boolean controlAltP() {
        return popAction();
    }

    public boolean controlAltW() {
        if (cancelButton != null) {
            if (!cancelButton.isDisabled() && cancelButton.isVisible()) {
                cancelAction();
            }
            return true;
        } else if (withdrawButton != null) {
            if (!withdrawButton.isDisabled() && withdrawButton.isVisible()) {
                withdrawAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAltM() {
        EventTarget target = keyEvent.getTarget();
        if (target != null) {
            if (target instanceof TextInputControl) {
                TextClipboardPopController.open(myController, (TextInputControl) target);
                return true;
            }
            if (target instanceof ComboBox) {
                ComboBox cb = (ComboBox) target;
                if (cb.isEditable()) {
                    TextClipboardPopController.open(myController, cb);
                    return true;
                }
            }
        }
        myBoxClipBoard();
        return true;
    }

    public boolean controlAltJ() {
        systemClipBoard();
        return true;
    }

    public boolean controlAltQ() {
        return false;
    }

    public boolean controlAltK() {
        return false;
    }

    public boolean controlAltU() {
        if (selectButton != null) {
            if (!selectButton.isDisabled() && selectButton.isVisible()) {
                selectAction();
            }
            return true;
        }
        return false;
    }

    public boolean controlAlt1() {
        return false;
    }

    public boolean controlAlt2() {
        return false;
    }

    public boolean controlAlt3() {
        return false;
    }

    public boolean controlAlt4() {
        return false;
    }

    public boolean keyFilter(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == null) {
            return false;
        }
        switch (code) {
            case ENTER:
                return keyEnter();

            case DELETE:
                return keyDelete();

            case HOME:
                return keyHome();

            case END:
                return keyEnd();

            case PAGE_UP:
                return keyPageUp();

            case PAGE_DOWN:
                return keyPageDown();

            case F1:
                return keyF1();

            case F2:
                return keyF2();

            case F3:
                return keyF3();

            case F4:
                return keyF4();

            case F5:
                return keyF5();

            case F6:
                return keyF6();

            case F7:
                return keyF7();

            case F8:
                return keyF8();

            case F9:
                return keyF9();

            case F10:
                return keyF10();

            case F11:
                return keyF11();

            case F12:
                return keyF12();

            case ESCAPE:
                return keyESC();

        }
        if (NodeTools.textInputFocus(getMyScene()) == null) {
            return controlAltFilter(event);
        }
        return false;
    }

    public boolean keyEnter() {
        return false;
    }

    public boolean keyHome() {
        if (defaultHandled()) {
            return false;
        }
        return altHome();
    }

    public boolean keyEnd() {
        if (defaultHandled()) {
            return false;
        }
        return altEnd();
    }

    public boolean keyPageUp() {
        if (defaultHandled()) {
            return false;
        }
        return altPageUp();
    }

    public boolean keyPageDown() {
        if (defaultHandled()) {
            return false;
        }
        return altPageDown();
    }

    public boolean keyDelete() {
        return controlAltD();
    }

    public boolean keyF1() {
        if (startButton != null && !startButton.isDisabled() && startButton.isVisible()) {
            startAction();
            return true;
        } else if (okButton != null && !okButton.isDisabled() && okButton.isVisible()) {
            okAction();
            return true;
        } else if (setButton != null && !setButton.isDisabled() && setButton.isVisible()) {
            setAction();
            return true;
        } else if (playButton != null && !playButton.isDisabled() && playButton.isVisible()) {
            playAction();
            return true;
        } else if (goButton != null && !goButton.isDisabled() && goButton.isVisible()) {
            goAction();
            return true;
        }
        return false;
    }

    public boolean keyF2() {
        return controlAltS();
    }

    public boolean keyF3() {
        return controlAltR();
    }

    public boolean keyF4() {
        if (leftPaneControl != null) {
            controlLeftPane();
            return true;
        }
        return false;
    }

    public boolean keyF5() {
        if (rightPaneControl != null) {
            controlRightPane();
            return true;
        }
        return false;
    }

    public boolean keyF6() {
        closePopup();
        return false; // continue to close
    }

    public boolean keyF7() {
        closeStage();
        return true;

    }

    public boolean keyF8() {
        refreshInterfaceAndFile();
        return true;

    }

    public boolean keyF9() {
        return false;
    }

    public boolean keyF10() {
        return synchronizeAction();
    }

    public boolean keyF11() {
        return controlAltB();
    }

    public boolean keyF12() {
        return menuAction();
    }

    public boolean keyESC() {
        if (cancelButton != null && !cancelButton.isDisabled() && cancelButton.isVisible()) {
            cancelAction();
        } else if (withdrawButton != null && !withdrawButton.isDisabled() && withdrawButton.isVisible()) {
            withdrawAction();
        }
        closePopup();
//                else if (stopButton != null && !stopButton.isDisabled()) {
//                    stopAction();
//                }
        return false;  // continue to close
    }

}
