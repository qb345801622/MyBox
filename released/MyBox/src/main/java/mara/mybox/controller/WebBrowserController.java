package mara.mybox.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import mara.mybox.db.data.VisitHistory;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.StyleTools;
import mara.mybox.fxml.WindowTools;
import mara.mybox.tools.FileNameTools;
import mara.mybox.tools.FileTools;
import mara.mybox.tools.HtmlReadTools;
import mara.mybox.value.AppVariables;
import mara.mybox.value.Fxmls;
import static mara.mybox.value.Languages.message;

/**
 * @Author Mara
 * @CreateDate 2018-7-31
 * @License Apache License Version 2.0
 */
public class WebBrowserController extends BaseController {

    protected Map<Tab, ControlWebView> tabControllers;
    protected Tab hisTab, favoriteTab;

    @FXML
    protected TabPane tabPane;
    @FXML
    protected Tab addTab;
    @FXML
    protected ImageView addIcon;

    public WebBrowserController() {
        baseTitle = message("WebBrowser");
    }

    @Override
    public void setFileType() {
        setFileType(VisitHistory.FileType.Html);
    }

    @Override
    public void initValues() {
        try {
            super.initValues();
            tabControllers = new HashMap();
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @Override
    public void setControlsStyle() {
        try {
            super.setControlsStyle();
            StyleTools.setIconTooltips(addIcon, "iconAdd.png", message("Add"));
        } catch (Exception e) {
            MyBoxLog.debug(e.toString());
        }
    }

    @Override
    public void afterSceneLoaded() {
        try {
            super.afterSceneLoaded();

            newTabAction();
        } catch (Exception e) {
            MyBoxLog.debug(e.toString());
        }
    }

    @FXML
    protected void newTabAction() {
        newTab(true);
    }

    protected ControlWebView newTab(boolean focus) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(WindowTools.class.getResource(
                    Fxmls.ControlWebViewFxml), AppVariables.currentBundle);
            Pane pane = fxmlLoader.load();
            Tab tab = new Tab();
            ImageView tabImage = new ImageView("img/MyBox.png");
            tabImage.setFitWidth(20);
            tabImage.setFitHeight(20);
            tab.setGraphic(tabImage);
            tab.setContent(pane);
            tabPane.getTabs().remove(addTab);
            tabPane.getTabs().addAll(tab, addTab);
            if (focus) {
                tabPane.getSelectionModel().select(tab);
            }
            refreshStyle(pane);

            ControlWebView controller = (ControlWebView) fxmlLoader.getController();
            controller.initTab(this, tab);
            if (tabControllers == null) {
                tabControllers = new HashMap();
            }
            tabControllers.put(tab, controller);
            tab.setOnClosed(new EventHandler<Event>() {
                @Override
                public void handle(Event event) {
                    tabControllers.remove(tab);
                }
            });
            return controller;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return null;
        }
    }

    public ControlWebView loadAddress(String address, boolean focus) {
        ControlWebView controller = newTab(focus);
        if (address != null) {
            controller.loadAddress(address);
        }
        return controller;
    }

    public ControlWebView loadContents(String contents, boolean focus) {
        ControlWebView controller = newTab(focus);
        if (contents != null) {
            controller.loadContents(contents);
        }
        return controller;
    }

    public ControlWebView loadFile(File file) {
        ControlWebView controller = newTab(true);
        controller.loadFile(file);
        return controller;
    }

    public void download(String address, String name) {
        if (address == null) {
            return;
        }
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            String dname;
            if (name != null && !name.isBlank()) {
                dname = name;
                String nameSuffix = FileNameTools.getFileSuffix(name);
                String addrSuffix = FileNameTools.getFileSuffix(address);
                if (addrSuffix != null && !addrSuffix.isBlank()) {
                    if (nameSuffix == null || nameSuffix.isBlank()
                            || !addrSuffix.equalsIgnoreCase(nameSuffix)) {
                        dname = name + "." + addrSuffix;
                    }
                }
            } else {
                dname = address;
            }
            int pos = dname.lastIndexOf("/");
            if (pos >= 0) {
                dname = (pos < dname.length() - 1) ? dname.substring(pos + 1) : "";
            }
            File dnFile = chooseSaveFile(VisitHistory.FileType.All, dname);
            if (dnFile == null) {
                return;
            }
            task = new SingletonTask<Void>() {

                @Override
                protected boolean handle() {
                    File tmpFile = HtmlReadTools.url2File(address);
                    return FileTools.rename(tmpFile, dnFile);
                }

                @Override
                protected void whenSucceeded() {
                    popSuccessful();
                    browseURI(dnFile.toURI());
                }

            };
            handling(task);
            task.setSelf(task);
            Thread thread = new Thread(task);
            thread.setDaemon(false);
            thread.start();
        }
    }

    @Override
    public void cleanPane() {
        try {
            tabControllers.clear();
            tabControllers = null;
        } catch (Exception e) {
        }
        super.cleanPane();
    }

    /*
        static methods
     */
    public static WebBrowserController oneOpen() {
        WebBrowserController controller = null;
        List<Window> windows = new ArrayList<>();
        windows.addAll(Window.getWindows());
        for (Window window : windows) {
            Object object = window.getUserData();
            if (object != null && object instanceof WebBrowserController) {
                try {
                    controller = (WebBrowserController) object;
                    controller.toFront();
                    break;
                } catch (Exception e) {
                }
            }
        }
        if (controller == null) {
            controller = (WebBrowserController) WindowTools.openStage(Fxmls.WebBrowserFxml);
        }
        return controller;
    }

    public static WebBrowserController oneOpen(File file) {
        WebBrowserController controller = oneOpen();
        if (controller != null && file != null) {
            controller.loadFile(file);
        }
        return controller;
    }

    public static WebBrowserController oneOpen(String address) {
        WebBrowserController controller = oneOpen();
        if (controller != null && address != null) {
            controller.loadAddress(address, true);
        }
        return controller;
    }

}
