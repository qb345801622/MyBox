package mara.mybox.controller;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Window;
import mara.mybox.data.CertificateEntry;
import mara.mybox.db.data.VisitHistory;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.NodeTools;
import mara.mybox.fxml.ControllerTools;
import mara.mybox.fxml.NodeStyleTools;
import mara.mybox.fxml.WindowTools;
import mara.mybox.fxml.RecentVisitMenu;
import mara.mybox.fxml.cell.TableTimeCell;
import mara.mybox.tools.NetworkTools;
import mara.mybox.tools.SecurityTools;
import mara.mybox.tools.SystemTools;
import static mara.mybox.value.Languages.message;

import mara.mybox.value.Fxmls;
import mara.mybox.value.Languages;

/**
 * @Author Mara
 * @CreateDate 2019-11-29
 * @License Apache License Version 2.0
 */
public class SecurityCertificatesController extends BaseController {

    protected ObservableList<CertificateEntry> tableData;

    @FXML
    protected TextField passwordInput;
    @FXML
    protected TableView<CertificateEntry> tableView;
    @FXML
    protected TableColumn<CertificateEntry, String> aliasColumn, timeColumn;
    @FXML
    protected TextArea certArea;
    @FXML
    protected Button htmlButton;
    @FXML
    protected ControlFileBackup backupController;

    public SecurityCertificatesController() {
        baseTitle = Languages.message("SecurityCertificates");
    }

    @Override
    public void setFileType() {
        setFileType(VisitHistory.FileType.All, VisitHistory.FileType.Html);
    }

    @Override
    public void initControls() {
        try {
            super.initControls();
            tableData = FXCollections.observableArrayList();

            aliasColumn.setCellValueFactory(new PropertyValueFactory<>("alias"));
            timeColumn.setCellValueFactory(new PropertyValueFactory<>("createTime"));
            timeColumn.setCellFactory(new TableTimeCell());

            tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            tableView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
                @Override
                public void changed(ObservableValue ov, Object t, Object t1) {
                    checkSelected();
                }
            });
            checkSelected();
            tableView.setItems(tableData);

            passwordInput.setText(SecurityTools.keystorePassword());
            htmlButton.setDisable(true);
            addButton.setDisable(true);
            recoverButton.setDisable(true);

            backupController.setControls(this, baseName);

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    protected void checkSelected() {
        if (isSettingValues) {
            return;
        }
        CertificateEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            certArea.setText("");
            deleteButton.setDisable(true);
        } else {
            showRightPane();
            certArea.setText(selected.getCertificates());
            deleteButton.setDisable(false);
        }
    }

    @Override
    public void setControlsStyle() {
        try {
            super.setControlsStyle();
            NodeStyleTools.setTooltip(recoverButton, Languages.message("RecoverKeyStore"));
        } catch (Exception e) {
            MyBoxLog.debug(e.toString());
        }
    }

    @Override
    public void sourceFileChanged(final File file) {
        sourceFile = file;
        this.getMyStage().setTitle(baseTitle + " " + (sourceFile == null ? "" : sourceFile.getAbsolutePath()));
        loadAll(null);
    }

    @Override
    public RecentVisitMenu makeSourceFileRecentVisitMenu(MouseEvent event) {
        RecentVisitMenu menu = super.makeSourceFileRecentVisitMenu(event);
        List<String> examples = new ArrayList<>();
        examples.add(SecurityTools.keystore());
        return menu.setExamples(examples);
    }

    @FXML
    public void loadAll(String selectAlias) {
        tableView.getItems().clear();
        certArea.setText("");
        htmlButton.setDisable(true);
        addButton.setDisable(true);
        recoverButton.setDisable(true);
        backupController.loadBackups(null);
        if (sourceFile == null) {
            return;
        }
        recoverButton.setVisible(sourceFile.getAbsolutePath().equals(SecurityTools.myboxCacerts().getAbsolutePath()));
        try {
            synchronized (this) {
                if (task != null && !task.isQuit()) {
                    return;
                }
                task = new SingletonTask<Void>() {
                    private String texts;
                    private List<CertificateEntry> entires;
                    private CertificateEntry selectCert;

                    @Override
                    protected boolean handle() {
                        try {
                            texts = error = null;
                            entires = new ArrayList();
                            selectCert = null;
                            // https://docs.oracle.com/javase/10/docs/api/java/security/KeyStore.html
                            try {
                                char[] passphrase = passwordInput.getText().toCharArray();
                                KeyStore keyStore = KeyStore.getInstance(sourceFile, passphrase);
                                Enumeration<String> storeAliases = keyStore.aliases();
                                while (storeAliases.hasMoreElements()) {
                                    String alias = storeAliases.nextElement();
                                    if (!keyStore.isCertificateEntry(alias)) {
                                        continue;
                                    }
                                    try {
                                        Certificate[] chain = keyStore.getCertificateChain(alias);
                                        if (chain == null) {
                                            Certificate cert = keyStore.getCertificate(alias);
                                            if (cert != null) {
                                                chain = new Certificate[1];
                                                chain[0] = cert;
                                            }
                                        }
                                        CertificateEntry entry = CertificateEntry.create()
                                                .setAlias(alias)
                                                .setCreateTime(keyStore.getCreationDate(alias).getTime())
                                                .setCertificateChain(chain);
                                        entires.add(entry);
                                        if (selectAlias != null && alias.equals(selectAlias)) {
                                            selectCert = entry;
                                        }
                                    } catch (Exception e) {
                                        error = e.toString();
                                    }
                                }
                                if (selectCert == null) {
                                    StringBuilder s = new StringBuilder();
                                    s.append(sourceFile).append("\n\n");

                                    s.append("## ").append(Languages.message("Type")).append(": ").append(keyStore.getType()).append("   ").
                                            append(Languages.message("Size")).append(": ").append(keyStore.size()).
                                            append("\n\n");
                                    for (CertificateEntry entry : entires) {
                                        s.append("#### ").append(Languages.message("Alias")).append(": ").append(entry.getAlias()).append("\n");
                                        s.append("----------------------------\n");
                                        if (entry.getCertificateChain() != null) {
                                            for (Certificate cert : entry.getCertificateChain()) {
                                                s.append(cert).append("\n\n");
                                            }
                                        }
                                    }
                                    texts = s.toString();
                                }
                            } catch (Exception e) {
                                error = e.toString();
                            }
                        } catch (Exception e) {
                            error = e.toString();
                        }
                        return !entires.isEmpty();
                    }

                    @Override
                    protected void whenSucceeded() {
                        isSettingValues = true;
                        tableView.getItems().addAll(entires);
                        // https://stackoverflow.com/questions/36240142/sort-tableview-by-certain-column-javafx?r=SearchResults
                        tableView.getSortOrder().add(timeColumn);
                        timeColumn.setSortType(TableColumn.SortType.DESCENDING);
                        tableView.sort();
                        isSettingValues = false;
                        if (selectCert != null) {
                            tableView.scrollTo(selectCert);
                            tableView.getSelectionModel().select(selectCert);
                        } else {
                            showRightPane();
                            certArea.setText(texts);
                        }
                        htmlButton.setDisable(false);
                        addButton.setDisable(false);
                        recoverButton.setDisable(false);
                        bottomLabel.setText(Languages.message("Total") + ": " + tableData.size());
                        backupController.loadBackups(sourceFile);
                    }
                };
                handling(task);
                task.setSelf(task);
                Thread thread = new Thread(task);
                thread.setDaemon(false);
                thread.start();
            }

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }

    }

    @FXML
    public void htmlAction() {
        if (sourceFile == null) {
            return;
        }
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {
                private String result;

                @Override
                protected boolean handle() {
                    try {
                        result = error = null;
                        try {
                            char[] passphrase = passwordInput.getText().toCharArray();
                            KeyStore keyStore = KeyStore.getInstance(sourceFile, passphrase);
                            StringBuilder s = new StringBuilder();
                            s.append("<h1  class=\"center\">").append(sourceFile.getAbsolutePath()).append("</h1>\n");
                            s.append("<h2  class=\"center\">").
                                    append(Languages.message("Type")).append(": ").append(keyStore.getType()).append(" ").
                                    append(Languages.message("Size")).append(": ").append(keyStore.size()).
                                    append("</h2>\n");
                            s.append("<hr>\n");
                            Enumeration<String> aliases = keyStore.aliases();
                            while (aliases.hasMoreElements()) {
                                String alias = aliases.nextElement();
                                s.append("<h3  class=\"center\">").
                                        append(Languages.message("Alias")).append(": ").append(alias).
                                        append("</h3>\n");
                                Certificate[] chain = keyStore.getCertificateChain(alias);
                                if (chain != null) {
                                    for (Certificate cert : chain) {
                                        s.append("<pre>").append(cert).append("</pre>\n\n");
                                    }
                                } else {
                                    Certificate cert = keyStore.getCertificate(alias);
                                    if (cert != null) {
                                        s.append("<pre>").append(cert).append("</pre>\n");
                                    }
                                }
                            }
                            result = s.toString();
                        } catch (Exception e) {
                            error = e.toString();
                        }
                    } catch (Exception e) {
                        error = e.toString();
                    }
                    return true;
                }

                @Override
                protected void whenSucceeded() {
                    if (result != null) {
                        ControllerTools.openHtmlTable(null, result);
                    } else {
                        popError(error);
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

    @FXML
    @Override
    public void addAction(ActionEvent event) {
        if (sourceFile == null) {
            return;
        }
        try {
            SecurityCertificatesAddController controller
                    = (SecurityCertificatesAddController) openStage(Fxmls.SecurityCertificateAddFxml);
            controller.setCertController(this);
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @FXML
    @Override
    public void deleteAction() {
        if (sourceFile == null) {
            return;
        }
        List<CertificateEntry> selected = tableView.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            return;
        }
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {

                @Override
                protected boolean handle() {
                    error = null;
                    try {
                        if (backupController.isBack()) {
                            backupController.addBackup(sourceFile);
                        }
                        List<String> aliases = new ArrayList();
                        for (CertificateEntry cert : selected) {
                            aliases.add(cert.getAlias());
                        }
                        error = SecurityTools.uninstallCertificate(
                                sourceFile.getAbsolutePath(), passwordInput.getText(),
                                aliases);
                    } catch (Exception e) {
                        error = e.toString();
                    }
                    return true;
                }

                @Override
                protected void whenSucceeded() {
                    if (error == null) {
                        loadAll(null);
                        popSuccessful();
                    } else {
                        popError(error);
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

    @FXML
    public void recover() {
        if (sourceFile == null
                || !sourceFile.getAbsolutePath().equals(SecurityTools.myboxCacerts().getAbsolutePath())) {
            return;
        }
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {

                @Override
                protected boolean handle() {
                    SecurityTools.resetKeystore();
                    return true;
                }

                @Override
                protected void whenSucceeded() {
                    alertInformation(Languages.message("TakeEffectWhenReboot"));
                    loadAll(null);
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
    public void refreshAction() {
        loadAll(null);
    }


    /*
        get/set
     */
    public TextField getPasswordInput() {
        return passwordInput;
    }

    public void setPasswordInput(TextField passwordInput) {
        this.passwordInput = passwordInput;
    }

    /*
        static methods
     */
    public static SecurityCertificatesController oneOpen(File file) {
        SecurityCertificatesController controller = null;
        List<Window> windows = new ArrayList<>();
        windows.addAll(Window.getWindows());
        for (Window window : windows) {
            Object object = window.getUserData();
            if (object != null && object instanceof SecurityCertificatesController) {
                try {
                    controller = (SecurityCertificatesController) object;
                    controller.toFront();
                    break;
                } catch (Exception e) {
                }
            }
        }
        if (controller == null) {
            controller = (SecurityCertificatesController) WindowTools.openStage(Fxmls.SecurityCertificatesFxml);
        }
        if (controller != null) {
            if (file != null) {
                controller.sourceFileChanged(file);
            }
            controller.getMyStage().requestFocus();
        }
        return controller;
    }

}
