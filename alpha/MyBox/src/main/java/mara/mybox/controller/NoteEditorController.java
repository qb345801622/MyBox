package mara.mybox.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import mara.mybox.db.DerbyBase;
import mara.mybox.db.data.Note;
import mara.mybox.db.data.NoteTag;
import mara.mybox.db.data.Notebook;
import mara.mybox.db.data.Tag;
import mara.mybox.db.table.TableNote;
import mara.mybox.db.table.TableNoteTag;
import mara.mybox.db.table.TableNotebook;
import mara.mybox.db.table.TableTag;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.LocateTools;
import mara.mybox.tools.DateTools;
import mara.mybox.tools.HtmlReadTools;
import mara.mybox.value.HtmlStyles;
import static mara.mybox.value.Languages.message;
import mara.mybox.value.UserConfig;

/**
 * @Author Mara
 * @CreateDate 2021-8-11
 * @License Apache License Version 2.0
 */
public class NoteEditorController extends HtmlEditorController {

    protected NotesController notesController;
    protected ControlNotebookSelector notebooksController;
    protected TableNotebook tableNotebook;
    protected TableNote tableNote;
    protected TableTag tableTag;
    protected TableNoteTag tableNoteTag;
    protected Note currentNote;
    protected Notebook bookOfCurrentNote;

    @FXML
    protected Label notebookLabel;
    @FXML
    protected TextField idInput, titleInput, timeInput;
    @FXML
    protected Tab styleTab, tagsTab;
    @FXML
    protected TextArea styleInput;
    @FXML
    protected FlowPane noteTagsPane;
    @FXML
    protected Button okNoteTagsButton, addNoteTagButton;
    @FXML
    protected ControlListCheckBox tagsListController;

    public void setParameters(NotesController notesController) {
        try {
            this.notesController = notesController;
            this.baseName = notesController.baseName;
            notebooksController = notesController.notebooksController;
            tableNotebook = notesController.tableNotebook;
            tableNote = notesController.tableNote;
            tableTag = notesController.tableTag;
            tableNoteTag = notesController.tableNoteTag;
            saveButton = notesController.saveButton;
            currentNote = null;
            tagsListController.setParent(notesController);

            tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
                @Override
                public void changed(ObservableValue ov, Tab oldTab, Tab newTab) {
                    if (oldTab == styleTab) {
                        webEngine.getLoadWorker().cancel();
                        webEngine.loadContent(styleHtml(htmlInWebview()));
                        htmlEditor.setHtmlText(styleHtml(htmlByEditor()));
                    }
                }
            });

            titleInput.setText(message("Note"));
            styleInput.setText(UserConfig.getString(baseName + "Style", HtmlStyles.DefaultStyle));

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    protected void editNote(Note note) {
        if (!checkBeforeNextAction()) {
            return;
        }
        updateFileStatus(false);
        currentNote = note;
        setAddressChanged(true);
        loadNote();
    }

    protected void loadNote() {
        synchronized (this) {
            if (currentNote != null) {
                idInput.setText(currentNote.getNtid() + "");
                titleInput.setText(currentNote.getTitle());
                timeInput.setText(DateTools.datetimeToString(currentNote.getUpdateTime()));
                String html = currentNote.getHtml();
                String style = UserConfig.getString(baseName + "Style", HtmlStyles.DefaultStyle);
                if (style != null && !style.isBlank()) {
                    style = "<!DOCTYPE html><html><head><style type=\"text/css\">\n" + style + "\n</style></head></html>";
                } else {
                    style = "";
                }
                webEngine.getLoadWorker().cancel();
                webEngine.loadContent(style + html);
            } else {
                idInput.setText("");
                timeInput.setText("");
                titleInput.setText(message("Note"));
                webEngine.getLoadWorker().cancel();
                webEngine.loadContent("");
            }
            updateBookOfCurrentNote();
            refreshNoteTags();
        }
    }

    protected void updateBookOfCurrentNote() {
        synchronized (this) {
            SingletonTask noteTask = new SingletonTask<Void>() {
                private String chainName;

                @Override
                protected boolean handle() {
                    try ( Connection conn = DerbyBase.getConnection()) {
                        if (currentNote != null) {
                            if (bookOfCurrentNote == null || bookOfCurrentNote.getNbid() != currentNote.getNotebookid()) {
                                bookOfCurrentNote = tableNotebook.find(conn, currentNote.getNotebookid());
                            }
                        }
                        if (bookOfCurrentNote == null) {
                            bookOfCurrentNote = notebooksController.root(conn);
                        }
                        if (bookOfCurrentNote == null) {
                            return false;
                        }
                        chainName = notebooksController.chainName(conn, bookOfCurrentNote);
                    } catch (Exception e) {
                        error = e.toString();
                        return false;
                    }
                    return true;
                }

                @Override
                protected void whenSucceeded() {
                    notebookLabel.setText(chainName);
                }
            };
            noteTask.setSelf(noteTask);
            Thread thread = new Thread(noteTask);
            thread.setDaemon(false);
            thread.start();
        }
    }

    @FXML
    protected void addNote() {
        editNote(null);
    }

    @FXML
    protected void copyNote() {
        if (!checkBeforeNextAction()) {
            return;
        }
        idInput.setText("");
        timeInput.setText("");
        titleInput.setText(titleInput.getText() + " - " + message("Copy"));
        currentNote = null;
        updateFileStatus(true);
    }

    @FXML
    protected void recoverNote() {
        loadNote();
    }

    /*
        html
     */
    @Override
    public String styleHtml(String html) {
        String style = UserConfig.getString(baseName + "Style", null);
        if (style != null && !style.isBlank()) {
            style = "<style type=\"text/css\">\n" + style + "\n</style>";
        } else {
            style = "";
        }
        return style + HtmlReadTools.body(html, false);
    }

    @Override
    public String htmlCodes(String html) {
        return HtmlReadTools.body(html, false);
    }

    @FXML
    @Override
    public void saveAction() {
        synchronized (this) {
            title = titleInput.getText();
            if (title == null || title.isBlank()) {
                popError(message("TitleNonEmpty"));
                return;
            }
            String html = currentHtml(true);
            notesController.rightPane.setDisable(true);
            SingletonTask saveTask = new SingletonTask<Void>() {
                private Note note;

                @Override
                protected boolean handle() {
                    try ( Connection conn = DerbyBase.getConnection()) {
                        note = new Note();
                        note.setTitle(title);
                        note.setHtml(HtmlReadTools.body(html, false));
                        note.setUpdateTime(new Date());
                        if (currentNote != null) {
                            note.setNtid(currentNote.getNtid());
                            note.setNotebook(currentNote.getNotebookid());
                            currentNote = tableNote.updateData(conn, note);
                        } else {
                            if (bookOfCurrentNote == null) {
                                bookOfCurrentNote = notebooksController.root(conn);
                            }
                            note.setNotebook(bookOfCurrentNote.getNbid());
                            currentNote = tableNote.insertData(conn, note);
                        }
                    } catch (Exception e) {
                        error = e.toString();
                        return false;
                    }
                    return currentNote != null;
                }

                @Override
                protected void whenSucceeded() {
                    popSaved();
                    idInput.setText(currentNote.getNtid() + "");
                    timeInput.setText(DateTools.datetimeToString(currentNote.getUpdateTime()));
                    updateBookOfCurrentNote();
                    if (notebooksController.selectedNode != null
                            && notebooksController.selectedNode.getNbid() == currentNote.getNotebookid()) {
                        notesController.refreshNotes();
                    }
                    notesController.refreshTimes();
                    updateFileStatus(false);
                }

                @Override
                protected void finalAction() {
                    notesController.rightPane.setDisable(false);
                }

            };
            saveTask.setSelf(saveTask);
            Thread thread = new Thread(saveTask);
            thread.setDaemon(false);
            thread.start();
        }
    }


    /*
        Note tags
     */
    @FXML
    public void refreshNoteTags() {
        synchronized (this) {
            tagsListController.clear();
            noteTagsPane.setDisable(true);
            SingletonTask noteTagsTask = new SingletonTask<Void>() {
                private List<String> tagsString;
                private List<Integer> selected;

                @Override
                protected boolean handle() {
                    tagsString = new ArrayList<>();
                    selected = new ArrayList<>();
                    try ( Connection conn = DerbyBase.getConnection()) {
                        List<Tag> tags = tableTag.readAll(conn);
                        if (tags != null && !tags.isEmpty()) {
                            for (Tag tag : tags) {
                                tagsString.add(tag.getTag());
                            }
                            if (currentNote != null) {
                                List<Long> noteTagIDs = tableNoteTag.readTags(conn, currentNote.getNtid());
                                if (noteTagIDs != null && !noteTagIDs.isEmpty()) {
                                    for (int i = 0; i < tags.size(); i++) {
                                        Tag tag = tags.get(i);
                                        if (noteTagIDs.contains(tag.getTgid())) {
                                            selected.add(i);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        error = e.toString();
                        return false;
                    }
                    return true;
                }

                @Override
                protected void whenSucceeded() {
                    tagsListController.setValues(tagsString);
                    tagsListController.selectIndex(selected);
                }

                @Override
                protected void finalAction() {
                    noteTagsPane.setDisable(false);
                }

            };
            start(noteTagsTask, false);
        }
    }

    @FXML
    public void selectAllNoteTags() {
        tagsListController.selectAll();
    }

    @FXML
    public void selectNoneNoteTags() {
        tagsListController.selectNone();
    }

    @FXML
    public void addNoteTag() {
        notesController.addTag(true);
    }

    @FXML
    public void okNoteTags() {
        if (currentNote == null) {
            return;
        }
        synchronized (this) {
            notesController.rightPane.setDisable(true);
            SingletonTask noteTagsTask = new SingletonTask<Void>() {

                @Override
                protected boolean handle() {
                    List<String> selected = tagsListController.getSelectedValues();
                    long noteid = currentNote.getNtid();
                    try ( Connection conn = DerbyBase.getConnection();
                             PreparedStatement query = conn.prepareStatement(TableTag.QueryTag);
                             PreparedStatement delete = conn.prepareStatement(TableNoteTag.DeleteNoteTags)) {
                        delete.setLong(1, noteid);
                        delete.executeUpdate();
                        conn.commit();
                        if (selected != null) {
                            List<NoteTag> noteTags = new ArrayList<>();
                            for (String value : selected) {
                                Tag tag = tableTag.query(conn, query, value);
                                if (tag == null) {
                                    continue;
                                }
                                noteTags.add(new NoteTag(noteid, tag.getTgid()));
                            }
                            tableNoteTag.insertList(conn, noteTags);
                        }
                        conn.commit();
                    } catch (Exception e) {
                        MyBoxLog.error(e);
                    }
                    return true;
                }

                @Override
                protected void whenSucceeded() {
                    popSaved();
                }

                @Override
                protected void finalAction() {
                    notesController.rightPane.setDisable(false);
                }

            };
            noteTagsTask.setSelf(noteTagsTask);
            Thread thread = new Thread(noteTagsTask);
            thread.setDaemon(false);
            thread.start();
        }
    }

    protected void bookChanged(Notebook book) {
        if (book == null) {
            return;
        }
        if (bookOfCurrentNote != null && book.getNbid() == bookOfCurrentNote.getNbid()) {
            bookOfCurrentNote = book;
            updateBookOfCurrentNote();
        }
    }

    /*
        Note style
     */
    @FXML
    public void popDefaultStyle(MouseEvent mouseEvent) {
        try {
            if (popMenu != null && popMenu.isShowing()) {
                popMenu.hide();
            }
            popMenu = new ContextMenu();
            popMenu.setAutoHide(true);

            MenuItem menu;
            for (HtmlStyles.HtmlStyle style : HtmlStyles.HtmlStyle.values()) {
                menu = new MenuItem(message(style.name()));
                menu.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        styleInput.setText(HtmlStyles.styleValue(style));
                        UserConfig.setString(baseName + "Style", styleInput.getText());
                    }
                });
                popMenu.getItems().add(menu);
            }

            popMenu.getItems().add(new SeparatorMenuItem());
            menu = new MenuItem(message("PopupClose"));
            menu.setStyle("-fx-text-fill: #2e598a;");
            menu.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    popMenu.hide();
                }
            });
            popMenu.getItems().add(menu);

            LocateTools.locateCenter((Region) mouseEvent.getSource(), popMenu);
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @FXML
    public void setStyle() {
        UserConfig.setString(baseName + "Style", styleInput.getText());
    }

    @FXML
    public void clearStyle() {
        styleInput.clear();
    }

    @Override
    public boolean checkBeforeNextAction() {
        if (!fileChanged) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(getMyStage().getTitle());
            alert.setContentText(message("NoteChanged"));
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            ButtonType buttonSave = new ButtonType(message("Save"));
            ButtonType buttonNotSave = new ButtonType(message("NotSave"));
            ButtonType buttonCancel = new ButtonType(message("Cancel"));
            alert.getButtonTypes().setAll(buttonSave, buttonNotSave, buttonCancel);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(true);
            stage.toFront();

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == buttonSave) {
                saveAction();
                return false;
            } else {
                return result.get() == buttonNotSave;
            }
        }
    }

}
