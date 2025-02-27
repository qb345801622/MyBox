package mara.mybox.controller;

import java.io.File;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.input.ContextMenuEvent;
import mara.mybox.db.data.VisitHistory.FileType;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.WindowTools;
import mara.mybox.tools.ByteFileTools;
import mara.mybox.tools.ByteTools;
import mara.mybox.tools.FileTools;
import mara.mybox.tools.TmpFileTools;
import mara.mybox.value.Fxmls;
import static mara.mybox.value.Languages.message;

/**
 * @Author Mara
 * @CreateDate 2021-8-8
 * @License Apache License Version 2.0
 */
public class BytesPopController extends TextPopController {

    public BytesPopController() {
        baseTitle = message("Bytes");
    }

    @Override
    public void setFileType() {
        setFileType(FileType.All);
    }

    @Override
    public void initControls() {
        try {
            super.initControls();

            textArea.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
                @Override
                public void handle(ContextMenuEvent event) {
                    MenuBytesEditController.open(myController, textArea, event);
                }
            });

        } catch (Exception e) {
            MyBoxLog.debug(e.toString());
        }
    }

    @FXML
    @Override
    public void editAction() {
        BytesEditorController controller = (BytesEditorController) WindowTools.openStage(Fxmls.BytesEditorFxml);
        controller.loadContents(textArea.getText());
        controller.toFront();
    }

    @FXML
    @Override
    public void saveAsAction() {
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            File file = chooseSaveFile();
            if (file == null) {
                return;
            }
            task = new SingletonTask<Void>() {
                @Override
                protected boolean handle() {
                    try {
                        File tmpFile = TmpFileTools.getTempFile();
                        tmpFile = ByteFileTools.writeFile(tmpFile, ByteTools.hexFormatToBytes(textArea.getText()));
                        return FileTools.rename(tmpFile, file);
                    } catch (Exception e) {
                        error = e.toString();
                        return false;
                    }
                }

                @Override
                protected void whenSucceeded() {
                    popSaved();
                    recordFileWritten(file);
                    if (openCheck.isSelected()) {
                        BytesEditorController controller = (BytesEditorController) WindowTools.openStage(Fxmls.BytesEditorFxml);
                        controller.sourceFileChanged(file);
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

    /*
        static methods
     */
    public static BytesPopController open(BaseController parent, String text) {
        try {
            if (text == null) {
                return null;
            }
            BytesPopController controller = (BytesPopController) WindowTools.openChildStage(parent.getMyWindow(), Fxmls.BytesPopFxml, false);
            controller.loadText(text);
            return controller;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return null;
        }
    }

}
