package mara.mybox.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import mara.mybox.db.data.Note;
import mara.mybox.db.data.Notebook;
import mara.mybox.db.table.TableNote;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.NodeStyleTools;
import mara.mybox.fxml.cell.TableDateCell;
import mara.mybox.value.Languages;

/**
 * @Author Mara
 * @CreateDate 2021-8-11
 * @License Apache License Version 2.0
 */
public class NotesController_Notes extends NotesController_Notebooks {

    @FXML
    protected TableColumn<Note, Long> ntidColumn;
    @FXML
    protected TableColumn<Note, String> titleColumn;
    @FXML
    protected TableColumn<Note, Date> timeColumn;
    @FXML
    protected Button refreshNotesButton, clearNotesButton, deleteNotesButton, moveDataNotesButton, copyNotesButton,
            addBookNoteButton, addNoteButton, queryTagsButton, deleteTagsButton, renameTagButton,
            refreshTimesButton, queryTimesButton, refreshTagsButton;
    @FXML
    protected CheckBox subCheck;
    @FXML
    protected Label conditionLabel;

    @Override
    protected void initColumns() {
        try {
            ntidColumn.setCellValueFactory(new PropertyValueFactory<>("ntid"));
            titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
            timeColumn.setCellValueFactory(new PropertyValueFactory<>("updateTime"));
            timeColumn.setCellFactory(new TableDateCell());

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @Override
    public void setControlsStyle() {
        try {
            super.setControlsStyle();

            NodeStyleTools.setTooltip(clearNotesButton, new Tooltip(Languages.message("ClearNotes")));
            NodeStyleTools.setTooltip(deleteNotesButton, new Tooltip(Languages.message("DeleteNotes")));
            NodeStyleTools.setTooltip(moveDataNotesButton, new Tooltip(Languages.message("MoveNotes")));
            NodeStyleTools.setTooltip(copyNotesButton, new Tooltip(Languages.message("CopyNotes")));

        } catch (Exception e) {
            MyBoxLog.debug(e.toString());
        }
    }

    @Override
    public void postLoadedTableData() {
        super.postLoadedTableData();
        makeConditionPane();
    }

    public void makeConditionPane() {
        notesConditionBox.getChildren().clear();
        if (notebooksController.selectedNode == null) {
            if (queryConditionsString != null) {
                conditionLabel.setText(queryConditionsString);
                notesConditionBox.getChildren().add(conditionLabel);
            }
            notesConditionBox.applyCss();
            return;
        }
        synchronized (this) {
            SingletonTask bookTask = new SingletonTask<Void>() {
                private List<Notebook> ancestor;

                @Override
                protected boolean handle() {
                    ancestor = tableNotebook.ancestor(notebooksController.selectedNode.getNbid());
                    return true;
                }

                @Override
                protected void whenSucceeded() {
                    List<Node> nodes = new ArrayList<>();
                    if (ancestor != null) {
                        for (Notebook book : ancestor) {
                            Hyperlink link = new Hyperlink(book.getName());
                            link.setWrapText(true);
                            link.setMinHeight(Region.USE_PREF_SIZE);
                            link.setOnAction(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent event) {
                                    loadBook(book);
                                }
                            });
                            nodes.add(link);
                            nodes.add(new Label(">"));
                        }
                    }
                    Label label = new Label(notebooksController.selectedNode.getName());
                    label.setWrapText(true);
                    label.setMinHeight(Region.USE_PREF_SIZE);
                    nodes.add(label);
                    namesPane.getChildren().setAll(nodes);
                    notesConditionBox.getChildren().setAll(namesPane, subCheck);
                    notesConditionBox.applyCss();
                }
            };
            bookTask.setSelf(bookTask);
            Thread thread = new Thread(bookTask);
            thread.setDaemon(false);
            thread.start();
        }

    }

    @Override
    public int readDataSize() {
        if (notebooksController.selectedNode != null && subCheck.isSelected()) {
            return TableNote.withSubSize(tableNotebook, notebooksController.selectedNode.getNbid());

        } else if (queryConditions != null) {
            return tableNote.conditionSize(queryConditions);

        } else {
            return 0;
        }
    }

    @Override
    public List<Note> readPageData() {
        if (notebooksController.selectedNode != null && subCheck.isSelected()) {
            return tableNote.withSub(tableNotebook, notebooksController.selectedNode.getNbid(), currentPageStart - 1, currentPageSize);

        } else if (queryConditions != null) {
            return tableNote.queryConditions(queryConditions, currentPageStart - 1, currentPageSize);

        } else {
            return null;
        }
    }

    @Override
    protected int clearData() {
        if (queryConditions != null) {
            return tableNote.deleteCondition(queryConditions);

        } else {
            return -1;
        }
    }

    @Override
    protected List<MenuItem> makeTableContextMenu() {
        try {
            List<MenuItem> items = new ArrayList<>();
            MenuItem menu;

            menu = new MenuItem(Languages.message("DeleteNotes"));
            menu.setOnAction((ActionEvent menuItemEvent) -> {
                deleteNotes();
            });
            menu.setDisable(deleteNotesButton.isDisabled());
            items.add(menu);

            menu = new MenuItem(Languages.message("CopyNotes"));
            menu.setOnAction((ActionEvent menuItemEvent) -> {
                copyNotes();
            });
            menu.setDisable(deleteNotesButton.isDisabled());
            items.add(menu);

            menu = new MenuItem(Languages.message("MoveNotes"));
            menu.setOnAction((ActionEvent menuItemEvent) -> {
                moveNotes();
            });
            menu.setDisable(deleteNotesButton.isDisabled());
            items.add(menu);

            menu = new MenuItem(Languages.message("AddNote"));
            menu.setOnAction((ActionEvent menuItemEvent) -> {
                addBookNote();
            });
            items.add(menu);

            items.add(new SeparatorMenuItem());

            menu = new MenuItem(Languages.message("ClearNotes"));
            menu.setOnAction((ActionEvent menuItemEvent) -> {
                clearNotes();
            });
            items.add(menu);

            items.add(new SeparatorMenuItem());

            if (pageNextButton != null && pageNextButton.isVisible() && !pageNextButton.isDisabled()) {
                menu = new MenuItem(Languages.message("NextPage"));
                menu.setOnAction((ActionEvent menuItemEvent) -> {
                    pageNextAction();
                });
                items.add(menu);
            }

            if (pagePreviousButton != null && pagePreviousButton.isVisible() && !pagePreviousButton.isDisabled()) {
                menu = new MenuItem(Languages.message("PreviousPage"));
                menu.setOnAction((ActionEvent menuItemEvent) -> {
                    pagePreviousAction();
                });
                items.add(menu);
            }

            menu = new MenuItem(Languages.message("Refresh"));
            menu.setOnAction((ActionEvent menuItemEvent) -> {
                refreshAction();
            });
            items.add(menu);

            return items;

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return null;
        }
    }

    @Override
    public void itemClicked() {
        editAction(null);
    }

    @Override
    protected int checkSelected() {
        if (isSettingValues) {
            return -1;
        }
        int selection = super.checkSelected();
        deleteNotesButton.setDisable(selection == 0);
        copyNotesButton.setDisable(selection == 0);
        moveDataNotesButton.setDisable(selection == 0);
        selectedLabel.setText(Languages.message("Selected") + ": " + selection);
        return selection;
    }

    @FXML
    protected void refreshNotes() {
        refreshAction();
    }

    @FXML
    protected void addBookNote() {
        showRightPane();
        noteEditorController.bookOfCurrentNote = notebooksController.selectedNode;
        noteEditorController.editNote(null);
    }

    @FXML
    protected void copyNotes() {
        NotesCopyNotesController.oneOpen((NotesController) this);
    }

    @FXML
    protected void moveNotes() {
        NotesMoveNotesController.oneOpen((NotesController) this);
    }

    @FXML
    protected void clearNotes() {
        clearAction();
        refreshTimes();
    }

    @FXML
    protected void deleteNotes() {
        deleteAction();
        refreshTimes();
    }

    @Override
    protected int deleteData(List<Note> data) {
        int ret = super.deleteData(data);
        if (ret <= 0) {
            return ret;
        }
        if (noteEditorController.currentNote == null || data == null || data.isEmpty()) {
            return 0;
        }
        for (Note note : data) {
            if (note.getNtid() == noteEditorController.currentNote.getNtid()) {
                Platform.runLater(() -> {
                    noteEditorController.editNote(null);
                });
                break;
            }
        }
        return ret;
    }

    @FXML
    @Override
    public void editAction(ActionEvent event) {
        Note note = tableView.getSelectionModel().getSelectedItem();
        if (note != null) {
            noteEditorController.editNote(note);
        }
    }

    protected void loadBook(Notebook book) {
        clearQuery();
        notebooksController.selectedNode = book;
        if (book != null) {
            queryConditions = " notebook=" + book.getNbid();
            loadTableData();
        }
    }

    protected void bookChanged(Notebook book) {
        if (book == null) {
            return;
        }
        if (notebooksController.selectedNode != null && notebooksController.selectedNode.getNbid() == book.getNbid()) {
            notebooksController.selectedNode = book;
            makeConditionPane();
        }
        noteEditorController.bookChanged(book);
    }

}
