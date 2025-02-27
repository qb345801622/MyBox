package mara.mybox.controller;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import mara.mybox.data.StringTable;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.LocateTools;
import mara.mybox.fxml.TextClipboardTools;
import mara.mybox.fxml.WebViewTools;
import mara.mybox.fxml.WindowTools;
import mara.mybox.tools.FileTools;
import mara.mybox.tools.HtmlReadTools;
import mara.mybox.tools.HtmlWriteTools;
import mara.mybox.tools.UrlTools;
import mara.mybox.value.Fxmls;
import static mara.mybox.value.Languages.message;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @Author Mara
 * @CreateDate 2021-7-6
 * @License Apache License Version 2.0
 */
public class BaseWebViewController extends BaseWebViewController_Assist {

    public BaseWebViewController() {
    }

    public void setParent(BaseController parent) {
        if (parent == null) {
            return;
        }
        this.parentController = parent;
        this.baseName = parent.baseName;
        myController = this;
    }

    public void setParent(BaseController parent, WebView webView) {
        setParent(parent);
        this.webView = webView;
        HTMLEditor editor = WebViewTools.editor(webView);
        if (editor != null) {
            editor.setUserData(this);
        } else {
            webView.setUserData(this);
        }
        this.setFileType();
        initWebView();
    }

    @FXML
    public void zoomIn() {
        zoomScale += 0.1f;
        webView.setZoom(zoomScale);
    }

    @FXML
    public void zoomOut() {
        zoomScale -= 0.1f;
        webView.setZoom(zoomScale);
    }

    @FXML
    public void backAction() {
        webEngine.executeScript("window.history.back();");
    }

    @FXML
    public void forwardAction() {
        webEngine.executeScript("window.history.forward();");
    }

    @FXML
    public void refreshAction() {
        goAction();
    }

    public String currentHtml() {
        return WebViewTools.getHtml(webEngine);
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
            String html = currentHtml();
            if (html == null || html.isBlank()) {
                popError(message("NoData"));
                return;
            }
            task = new SingletonTask<Void>() {
                @Override
                protected boolean handle() {
                    File tmpFile = HtmlWriteTools.writeHtml(html);
                    return FileTools.rename(tmpFile, file);
                }

                @Override
                protected void whenSucceeded() {
                    popSaved();
                    recordFileWritten(file);
                    afterSaveAs(file);
                }
            };
            handling(task);
            task.setSelf(task);
            Thread thread = new Thread(task);
            thread.setDaemon(false);
            thread.start();
        }
    }

    protected void afterSaveAs(File file) {

    }

    @FXML
    @Override
    public void cancelAction() {
        webEngine.getLoadWorker().cancel();
    }

    @FXML
    public void popFunctionsMenu(MouseEvent mouseEvent) {
        try {
            String html = WebViewTools.getHtml(webEngine);
            doc = webEngine.getDocument();
            boolean isFrameset = framesDoc != null && framesDoc.size() > 0;

            List<MenuItem> items = new ArrayList<>();
            MenuItem menu;

            if (address != null && !address.isBlank()) {
                menu = new MenuItem(address);
                menu.setStyle("-fx-text-fill: #2e598a;");
                items.add(menu);
                items.add(new SeparatorMenuItem());
            }

            if (backwardButton == null) {
                int hisSize = (int) webEngine.executeScript("window.history.length;");

                menu = new MenuItem(message("ZoomIn"));
                menu.setOnAction((ActionEvent event) -> {
                    zoomIn();
                });
                items.add(menu);

                menu = new MenuItem(message("ZoomOut"));
                menu.setOnAction((ActionEvent event) -> {
                    zoomOut();
                });
                items.add(menu);

                menu = new MenuItem(message("Refresh"));
                menu.setOnAction((ActionEvent event) -> {
                    refreshAction();
                });
                items.add(menu);

                menu = new MenuItem(message("Cancel"));
                menu.setOnAction((ActionEvent event) -> {
                    cancelAction();
                });
                items.add(menu);

                menu = new MenuItem(message("Backward"));
                menu.setOnAction((ActionEvent event) -> {
                    backAction();
                });
                menu.setDisable(hisSize < 2);
                items.add(menu);

                menu = new MenuItem(message("Forward"));
                menu.setOnAction((ActionEvent event) -> {
                    forwardAction();
                });
                menu.setDisable(hisSize < 2);
                items.add(menu);
            }

            items.add(new SeparatorMenuItem());

            if (address != null && !address.isBlank()) {
                menu = new MenuItem(message("AddAsFavorite"));
                menu.setOnAction((ActionEvent event) -> {
                    WebFavoriteAddController controller = (WebFavoriteAddController) WindowTools.openStage(Fxmls.WebFavoriteAddFxml);
                    controller.setValues(webEngine.getTitle(), address);

                });
                items.add(menu);
            }

            menu = new MenuItem(message("WebFavorites"));
            menu.setOnAction((ActionEvent event) -> {
                WebFavoritesController.oneOpen();
            });
            items.add(menu);

            menu = new MenuItem(message("WebHistories"));
            menu.setOnAction((ActionEvent event) -> {
                WebHistoriesController.oneOpen();
            });
            items.add(menu);

            if (address != null && !address.isBlank()) {
                menu = new MenuItem(message("QueryNetworkAddress"));
                menu.setOnAction((ActionEvent event) -> {
                    NetworkQueryAddressController controller
                            = (NetworkQueryAddressController) WindowTools.openStage(Fxmls.NetworkQueryAddressFxml);
                    controller.queryUrl(address);
                });
                items.add(menu);
            }

            items.add(new SeparatorMenuItem());

            List<MenuItem> editItems = new ArrayList<>();

            if (!(parentController instanceof HtmlEditorController) && html != null && !html.isBlank()) {
                menu = new MenuItem(message("HtmlEditor"));
                menu.setOnAction((ActionEvent event) -> {
                    edit(html);
                });
                editItems.add(menu);
            }

            if (!(parentController instanceof HtmlSnapController) && html != null && !html.isBlank()) {
                menu = new MenuItem(message("HtmlSnap"));
                menu.setOnAction((ActionEvent event) -> {
                    HtmlSnapController controller = (HtmlSnapController) WindowTools.openStage(Fxmls.HtmlSnapFxml);
                    if (address != null && !address.isBlank()) {
                        controller.loadAddress(address);
                    } else {
                        controller.loadContents(html);
                    }
                });
                editItems.add(menu);
            }

            if (isFrameset) {
                NodeList frameList = webEngine.getDocument().getElementsByTagName("frame");
                if (frameList != null) {
                    List<MenuItem> frameItems = new ArrayList<>();
                    for (int i = 0; i < frameList.getLength(); i++) {
                        org.w3c.dom.Node node = frameList.item(i);
                        if (node == null) {
                            continue;
                        }
                        int index = i;
                        Element e = (Element) node;
                        String src = e.getAttribute("src");
                        String name = e.getAttribute("name");
                        String frame = message("Frame") + index;
                        if (name != null && !name.isBlank()) {
                            frame += " :   " + name;
                        } else if (src != null && !src.isBlank()) {
                            frame += " :   " + src;
                        }
                        menu = new MenuItem(frame);
                        menu.setOnAction((ActionEvent event) -> {
                            HtmlEditorController controller = (HtmlEditorController) WindowTools.openStage(Fxmls.HtmlEditorFxml);
                            if (src != null && !src.isBlank()) {
                                controller.loadAddress(UrlTools.fullAddress(address, src));
                            } else {
                                controller.loadContents(WebViewTools.getFrame(webEngine, index));
                            }

                        });
                        menu.setDisable(html == null || html.isBlank());
                        frameItems.add(menu);
                    }
                    if (!frameItems.isEmpty()) {
                        Menu frameMenu = new Menu(message("Frame"));
                        frameMenu.getItems().addAll(frameItems);
                        editItems.add(frameMenu);
                    }
                }
            }

            if (!editItems.isEmpty()) {
                editItems.add(new SeparatorMenuItem());
                items.addAll(editItems);
            }

            if (html != null && !html.isBlank()) {
                menu = new MenuItem(message("HtmlCodes"));
                menu.setOnAction((ActionEvent event) -> {
                    HtmlCodesPopController.open(myController, html);
                });
                items.add(menu);

                menu = new MenuItem(message("WebFind"));
                menu.setOnAction((ActionEvent event) -> {
                    find(html);
                });
                items.add(menu);

                menu = new MenuItem(message("WebElements"));
                menu.setOnAction((ActionEvent event) -> {
                    HtmlElementsController controller = (HtmlElementsController) WindowTools.openStage(Fxmls.HtmlElementsFxml);
                    if (address != null && !address.isBlank()) {
                        controller.loadAddress(address);
                    } else {
                        controller.loadContents(html);
                    }
                    controller.toFront();
                });
                items.add(menu);

                Menu elementsMenu = new Menu(message("Extract"));
                List<MenuItem> elementsItems = new ArrayList<>();

                menu = new MenuItem(message("Texts"));
                menu.setOnAction((ActionEvent event) -> {
                    texts(html);
                });
                menu.setDisable(isFrameset);
                elementsItems.add(menu);

                menu = new MenuItem(message("Links"));
                menu.setOnAction((ActionEvent event) -> {
                    links();
                });
                menu.setDisable(isFrameset || doc == null);
                elementsItems.add(menu);

                menu = new MenuItem(message("Images"));
                menu.setOnAction((ActionEvent event) -> {
                    images();
                });
                menu.setDisable(isFrameset || doc == null);
                elementsItems.add(menu);

                menu = new MenuItem(message("Headings"));
                menu.setOnAction((ActionEvent event) -> {
                    toc(html);
                });
                menu.setDisable(isFrameset);
                elementsItems.add(menu);

                elementsMenu.getItems().setAll(elementsItems);
                items.add(elementsMenu);

                items.add(new SeparatorMenuItem());
            }

            if (address != null && !address.isBlank()) {
                menu = new MenuItem(message("OpenLinkBySystem"));
                menu.setOnAction((ActionEvent event) -> {
                    browse(address);
                });
                items.add(menu);

                items.add(new SeparatorMenuItem());

                menu = new MenuItem(message("OpenLinkInNewTab"));
                menu.setOnAction((ActionEvent event) -> {
                    WebBrowserController c = WebBrowserController.oneOpen();
                    c.loadAddress(address, false);
                });
                items.add(menu);

                menu = new MenuItem(message("OpenLinkInNewTabSwitch"));
                menu.setOnAction((ActionEvent event) -> {
                    WebBrowserController c = WebBrowserController.oneOpen();
                    c.loadAddress(address, true);
                });
                items.add(menu);
            }

            menu = new MenuItem(message("PopupClose"));
            menu.setStyle("-fx-text-fill: #2e598a;");
            menu.setOnAction((ActionEvent menuItemEvent) -> {
                if (popMenu != null && popMenu.isShowing()) {
                    popMenu.hide();
                }
                popMenu = null;
            });
            items.add(menu);

            if (popMenu != null && popMenu.isShowing()) {
                popMenu.hide();
            }
            popMenu = new ContextMenu();
            popMenu.setAutoHide(true);
            popMenu.getItems().addAll(items);
            LocateTools.locateCenter((Region) mouseEvent.getSource(), popMenu);

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    public void find(String html) {
        HtmlFindController controller = (HtmlFindController) WindowTools.openStage(Fxmls.HtmlFindFxml);
        controller.loadContents(html);
        controller.setAddress(address);
        controller.toFront();
    }

    @FXML
    @Override
    public void findAction() {
        find(WebViewTools.getHtml(webEngine));
    }

    public HtmlEditorController edit(String html) {
        HtmlEditorController controller = (HtmlEditorController) WindowTools.openStage(Fxmls.HtmlEditorFxml);
        if (address != null && !address.isBlank()) {
            controller.loadAddress(address);
        } else if (html != null && !html.isBlank()) {
            controller.loadContents(html);
        }
        return controller;
    }

    @FXML
    public void editAction() {
        edit(WebViewTools.getHtml(webEngine));
    }

    protected void links() {
        doc = webEngine.getDocument();
        if (doc == null) {
            popError(message("NoData"));
            return;
        }
        try {
            NodeList aList = doc.getElementsByTagName("a");
            if (aList == null || aList.getLength() < 1) {
                popError(message("NoData"));
                return;
            }
            List<String> names = new ArrayList<>();
            names.addAll(Arrays.asList(message("Index"), message("Link"), message("Name"), message("Title"),
                    message("Address"), message("FullAddress")
            ));
            StringTable table = new StringTable(names);
            int index = 1;
            for (int i = 0; i < aList.getLength(); i++) {
                org.w3c.dom.Node node = aList.item(i);
                if (node == null) {
                    continue;
                }
                Element element = (Element) node;
                String href = element.getAttribute("href");
                if (href == null || href.isBlank()) {
                    continue;
                }
                String linkAddress = href;
                try {
                    URL url = new URL(new URL(element.getBaseURI()), href);
                    linkAddress = url.toString();
                } catch (Exception e) {
                }
                String name = element.getTextContent();
                String title = element.getAttribute("title");
                List<String> row = new ArrayList<>();
                row.addAll(Arrays.asList(
                        index + "",
                        "<a href=\"" + linkAddress + "\">" + (name == null ? title : name) + "</a>",
                        name == null ? "" : name,
                        title == null ? "" : title,
                        URLDecoder.decode(href, charset),
                        URLDecoder.decode(linkAddress, charset)
                ));
                table.add(row);
                index++;
            }
            table.editHtml();
        } catch (Exception e) {
            popError(e.toString());
        }
    }

    protected void images() {
        doc = webEngine.getDocument();
        if (doc == null) {
            popError(message("NoData"));
            return;
        }
        try {
            NodeList aList = doc.getElementsByTagName("img");
            if (aList == null || aList.getLength() < 1) {
                popError(message("NoData"));
                return;
            }
            List<String> names = new ArrayList<>();
            names.addAll(Arrays.asList(message("Index"), message("Link"), message("Name"), message("Title"),
                    message("Address"), message("FullAddress")
            ));
            StringTable table = new StringTable(names);
            int index = 1;
            for (int i = 0; i < aList.getLength(); i++) {
                org.w3c.dom.Node node = aList.item(i);
                if (node == null) {
                    continue;
                }
                Element element = (Element) node;
                String href = element.getAttribute("src");
                if (href == null || href.isBlank()) {
                    continue;
                }
                String linkAddress = href;
                try {
                    URL url = new URL(new URL(element.getBaseURI()), href);
                    linkAddress = url.toString();
                } catch (Exception e) {
                }
                String name = element.getAttribute("alt");
                List<String> row = new ArrayList<>();
                row.addAll(Arrays.asList(
                        index + "",
                        "<a href=\"" + linkAddress + "\">" + (name == null ? message("Link") : name) + "</a>",
                        "<img src=\"" + linkAddress + "\" " + (name == null ? "" : "alt=\"" + name + "\"") + " width=100/>",
                        name == null ? "" : name,
                        URLDecoder.decode(href, charset),
                        URLDecoder.decode(linkAddress, charset)
                ));
                table.add(row);
                index++;
            }
            table.editHtml();
        } catch (Exception e) {
            popError(e.toString());
        }
    }

    protected void toc(String html) {
        if (html == null) {
            popError(message("NoData"));
            return;
        }
        String toc = HtmlReadTools.toc(html, 8);
        if (toc == null || toc.isBlank()) {
            popError(message("NoData"));
            return;
        }
        TextEditorController c = (TextEditorController) WindowTools.openStage(Fxmls.TextEditorFxml);
        c.loadContents(toc);
        c.toFront();
    }

    protected void texts(String html) {
        if (html == null) {
            popError(message("NoData"));
            return;
        }
        String texts = HtmlWriteTools.htmlToText(html);
        if (texts == null || texts.isBlank()) {
            popError(message("NoData"));
            return;
        }
        TextEditorController c = (TextEditorController) WindowTools.openStage(Fxmls.TextEditorFxml);
        c.loadContents(texts);
        c.toFront();
    }

    @FXML
    @Override
    public boolean popAction() {
        HtmlPopController.html(parentController, webView);
        return true;
    }

    @Override
    public boolean controlAltO() {
        selectNoneAction();
        return true;
    }

    @FXML
    @Override
    public void selectNoneAction() {
        WebViewTools.selectNone(webView.getEngine());
    }

    @Override
    public boolean controlAltU() {
        selectAction();
        return true;
    }

    @FXML
    @Override
    public void selectAction() {
        WebViewTools.selectElement(webView, element);
    }

    @Override
    public boolean controlAltT() {
        copyTextToSystemClipboard();
        return true;
    }

    @FXML
    public void copyTextToSystemClipboard() {
        if (webView == null) {
            return;
        }
        String text = WebViewTools.selectedText(webView.getEngine());
        if (text == null || text.isEmpty()) {
            popError(message("SelectedNone"));
            return;
        }
        TextClipboardTools.copyToSystemClipboard(myController, text);
    }

    @Override
    public boolean controlAltH() {
        copyHtmlToSystemClipboard();
        return true;
    }

    @FXML
    public void copyHtmlToSystemClipboard() {
        String html = WebViewTools.selectedHtml(webView.getEngine());
        if (html == null || html.isEmpty()) {
            popError(message("SelectedNone"));
            return;
        }
        TextClipboardTools.copyToSystemClipboard(myController, html);
    }

    @Override
    public void cleanPane() {
        try {
            if (timer != null) {
                timer.cancel();
            }
            if (webEngine != null && webEngine.getLoadWorker() != null) {
                webEngine.getLoadWorker().cancel();
            }
            if (webView != null) {
                webView.setUserData(null);
            }
        } catch (Exception e) {
        }
        super.cleanPane();
    }

}
