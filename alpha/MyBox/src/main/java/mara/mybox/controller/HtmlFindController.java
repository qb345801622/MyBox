package mara.mybox.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import mara.mybox.data.FindReplaceString;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.NodeStyleTools;
import mara.mybox.fxml.PopTools;
import mara.mybox.fxml.WebViewTools;
import mara.mybox.tools.HtmlReadTools;
import mara.mybox.tools.HtmlWriteTools;
import static mara.mybox.value.Languages.message;
import mara.mybox.value.UserConfig;

/**
 * @Author Mara
 * @CreateDate 2021-5-5
 * @License Apache License Version 2.0
 */
public class HtmlFindController extends WebAddressController {

    protected static final String ItemPrefix = "MyBoxSearchLocation";
    protected int foundCount, foundItem;
    protected String loadedHtml, resultsHtml;
    protected LoadingController loading;

    @FXML
    protected ComboBox<String> findFontSelector, foundItemSelector;
    @FXML
    protected ControlStringSelector findInputController;
    @FXML
    protected ColorSet findColorController, findBgColorController, currentColorController, currentBgColorController;
    @FXML
    protected Label foundLabel;
    @FXML
    protected Button goItemButton, queryButton, exampleFindButton;
    @FXML
    protected CheckBox caseCheck, wrapCheck, regCheck;

    public HtmlFindController() {
        baseTitle = message("WebFind");
    }

    @Override
    public void initControls() {
        try {
            super.initControls();

            findInputController.init(this, baseName + "Find", "find", 20);

            findColorController.init(this, baseName + "FindColor", Color.YELLOW);
            findBgColorController.init(this, baseName + "FindBgColor", Color.BLACK);
            currentColorController.init(this, baseName + "CurrentColor", Color.RED);
            currentBgColorController.init(this, baseName + "CurrentBgColor", Color.BLACK);

            List<String> fonts = new ArrayList();
            fonts.addAll(Arrays.asList("1em", "1.2em", "1.5em", "24px", "28px"));
            String saved = UserConfig.getString(baseName + "Font", "1.2em");
            if (!fonts.contains(saved)) {
                fonts.add(0, saved);
            }
            findFontSelector.getItems().addAll(fonts);
            findFontSelector.getSelectionModel().select(saved);

            caseCheck.setSelected(UserConfig.getBoolean(baseName + "CaseInsensitive", false));
            caseCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    UserConfig.setBoolean(baseName + "CaseInsensitive", caseCheck.isSelected());
                }
            });

            regCheck.setSelected(UserConfig.getBoolean(baseName + "RegularExpression", false));
            regCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    UserConfig.setBoolean(baseName + "RegularExpression", regCheck.isSelected());
                }
            });

            wrapCheck.setSelected(UserConfig.getBoolean(baseName + "WrapAround", false));
            wrapCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    UserConfig.setBoolean(baseName + "WrapAround", wrapCheck.isSelected());
                }
            });

            goItemButton.setDisable(true);
            firstButton.setDisable(true);
            previousButton.setDisable(true);
            nextButton.setDisable(true);
            lastButton.setDisable(true);

            exampleFindButton.disableProperty().bind(regCheck.selectedProperty().not());

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }

    }

    @Override
    public void setControlsStyle() {
        try {
            super.setControlsStyle();
            NodeStyleTools.setTooltip(queryButton, new Tooltip(message("Query") + "\nF1"));
        } catch (Exception e) {
            MyBoxLog.debug(e.toString());
        }
    }

    public void find(String html) {
        loadedHtml = html;
        foundCount = 0;
        loadContents(html);
    }

    @FXML
    @Override
    public void goAction() {
        foundCount = 0;
        loadedHtml = null;
        super.goAction();
    }

    @Override
    public void afterPageLoaded() {
        try {
            super.afterPageLoaded();

            if (loadedHtml == null) {
                loadedHtml = WebViewTools.getHtml(webEngine);
            } else {
                popInformation(message("Found") + ": " + foundCount);

            }

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @FXML
    protected void queryAction() {
        synchronized (this) {
            if (task != null) {
                task.cancel();
                task = null;
            }
            if (loadedHtml == null) {
                loadedHtml = WebViewTools.getHtml(webEngine);
            }
            if (loadedHtml == null || loadedHtml.isBlank()) {
                popError(message("NoData"));
                return;
            }
            String string = findInputController.value();
            if (string == null || string.isBlank()) {
                parentController.popError(message("InvalidData"));
                return;
            }
            findInputController.refreshList();

            foundCount = 0;
            foundLabel.setText("");
            foundItemSelector.getItems().clear();
            goItemButton.setDisable(true);
            firstButton.setDisable(true);
            previousButton.setDisable(true);
            nextButton.setDisable(true);
            lastButton.setDisable(true);
            task = new SingletonTask<Void>() {

                private StringBuilder results;

                @Override
                protected boolean handle() {
                    try {
                        String findString = HtmlWriteTools.stringToHtml(string);

                        String font = findFontSelector.getValue();
                        UserConfig.setString(baseName + "Font", font);

                        FindReplaceString textsChecker = FindReplaceString.create()
                                .setOperation(FindReplaceString.Operation.FindNext)
                                .setIsRegex(false).setCaseInsensitive(true).setMultiline(true);

                        FindReplaceString finder = FindReplaceString.create()
                                .setOperation(FindReplaceString.Operation.FindNext).setFindString(findString)
                                .setIsRegex(regCheck.isSelected()).setCaseInsensitive(caseCheck.isSelected()).setMultiline(true);
                        String inputString = HtmlReadTools.body(loadedHtml, false);
                        String replaceSuffix = " style=\"" + itemsStyle() + "\" >" + findString + "</span>";

                        results = new StringBuilder();
                        String texts;
                        while (!inputString.isBlank()) {
                            textsChecker.setInputString(inputString).setFindString(">").setAnchor(0).run();
                            if (textsChecker.getStringRange() == null) {
                                break;
                            }
                            results.append(inputString.substring(0, textsChecker.getLastEnd()));
                            inputString = inputString.substring(textsChecker.getLastEnd());
                            textsChecker.setInputString(inputString).setFindString("<").setAnchor(0).run();
                            if (textsChecker.getStringRange() == null) {
                                texts = inputString;
                                inputString = "";
                            } else {
                                if (textsChecker.getLastStart() > 0) {
                                    texts = inputString.substring(0, textsChecker.getLastStart());
                                } else {
                                    texts = "";
                                }
                                inputString = inputString.substring(textsChecker.getLastStart());
                            }
                            if (texts.isEmpty()) {
                                continue;
                            }
                            StringBuilder r = new StringBuilder();
                            while (!texts.isBlank()) {
                                finder.setInputString(texts).setAnchor(0).run();
                                if (finder.getStringRange() == null) {
                                    break;
                                }
                                String replaceString = "<span id=\"" + ItemPrefix + (++foundCount) + "\" " + replaceSuffix;
                                if (finder.getLastStart() > 0) {
                                    r.append(texts.substring(0, finder.getLastStart()));
                                }
                                r.append(replaceString);
                                texts = texts.substring(finder.getLastEnd());
                                Platform.runLater(() -> {
                                    loading.setInfo(message("Found") + ": " + foundCount);
                                });
                            }
                            r.append(texts);
                            results.append(r.toString());
                        }
                        results.append(inputString);
                        return true;
                    } catch (Exception e) {
                        error = e.toString();
                        return false;
                    }
                }

                @Override
                protected void whenSucceeded() {
                    String info = message("Found") + ": " + foundCount;
                    foundLabel.setText(info);

                    foundItem = 0;
                    if (foundCount > 0) {
                        List<String> numbers = new ArrayList<>();
                        for (int i = 1; i <= foundCount; i++) {
                            numbers.add(i + "");
                        }
                        foundItemSelector.getItems().setAll(numbers);
                        foundItemSelector.getSelectionModel().select(0);
                        goItemButton.setDisable(false);
                        firstButton.setDisable(false);
                        previousButton.setDisable(false);
                        nextButton.setDisable(false);
                        lastButton.setDisable(false);
                    }
                    webEngine.getLoadWorker().cancel();
                    webEngine.loadContent(results.toString());
                }

                @Override
                protected void finalAction() {
                    loading = null;
                }

            };
            task.setSelf(task);
            loading = handling(task);
            Thread thread = new Thread(task);
            thread.setDaemon(false);
            thread.start();
        }
    }

    protected String itemsStyle() {
        return "color:" + findColorController.rgb()
                + "; background: " + findBgColorController.rgb()
                + "; font-size:" + findFontSelector.getValue() + ";";
    }

    protected String currentStyle() {
        return "color:" + currentColorController.rgb()
                + "; background: " + currentBgColorController.rgb()
                + "; font-size:" + findFontSelector.getValue() + ";";
    }

    protected void setStyle(int id, String style) {
        if (id <= 0 || id > foundCount) {
            return;
        }
        webEngine.executeScript("document.getElementById('" + ItemPrefix + id + "').setAttribute('style', '" + style + "');");
    }

    protected void scrollTo(int id) {
        if (id <= 0 || id > foundCount) {
            return;
        }
        webEngine.executeScript("document.getElementById('" + ItemPrefix + id + "').scrollIntoView();");
    }

    // 1-based
    protected void goItem(int index) {
        setStyle(foundItem, itemsStyle());
        foundItem = index;
        if (foundItem < 1) {
            foundItem = wrapCheck.isSelected() ? foundCount : 1;
        }
        if (foundItem > foundCount) {
            foundItem = wrapCheck.isSelected() ? 1 : foundCount;
        }
        foundItemSelector.getSelectionModel().select(foundItem + "");
        scrollTo(foundItem);
        setStyle(foundItem, currentStyle());
    }

    @FXML
    protected void goItem() {
        String item = foundItemSelector.getValue();
        if (item == null || item.isBlank()) {
            return;
        }
        goItem(Integer.parseInt(item));
    }

    @FXML
    @Override
    public void firstAction() {
        goItem(1);
    }

    @FXML
    @Override
    public void previousAction() {
        goItem(foundItem - 1);
    }

    @FXML
    @Override
    public void nextAction() {
        goItem(foundItem + 1);
    }

    @FXML
    @Override
    public void lastAction() {
        goItem(foundCount);
    }

    @FXML
    public void popFindExample(MouseEvent mouseEvent) {
        PopTools.popRegexExample(this, findInputController.selector.getEditor(), mouseEvent);
    }

    @Override
    public boolean keyF1() {
        queryAction();
        return true;
    }

}
