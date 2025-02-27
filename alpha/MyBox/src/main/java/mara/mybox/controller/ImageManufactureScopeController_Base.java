package mara.mybox.controller;

import java.awt.image.BufferedImage;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import mara.mybox.bufferedimage.PixelsOperation;
import mara.mybox.bufferedimage.PixelsOperationFactory;
import mara.mybox.db.table.TableColor;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.value.UserConfig;

/**
 * @Author Mara
 * @CreateDate 2021-8-13
 * @License Apache License Version 2.0
 */
public class ImageManufactureScopeController_Base extends ImageViewerController {

    protected TableColor tableColor;
    protected ImageManufactureController imageController;
    protected ImageManufactureScopesSavedController scopesSavedController;
    protected float opacity;
    protected BufferedImage outlineSource;
    protected ImagePopController imagePopController;

    @FXML
    protected ImageView scopeView, scopeTipsView;
    @FXML
    protected ToggleGroup scopeTypeGroup, matchGroup;
    @FXML
    protected TabPane tabPane;
    @FXML
    protected Tab areaTab, pointsTab, colorsTab, matchTab, pixTab, optionsTab, saveTab;
    @FXML
    protected VBox setBox, areaBox, rectangleBox, circleBox;
    @FXML
    protected ComboBox<String> scopeDistanceSelector, opacitySelector;
    @FXML
    protected ListView<Image> outlinesList;
    @FXML
    protected ColorSet colorSetController;
    @FXML
    protected ListView<Color> colorsList;
    @FXML
    protected ListView<String> pointsList;
    @FXML
    protected CheckBox areaExcludedCheck, colorExcludedCheck, scopeOutlineKeepRatioCheck, eightNeighborCheck,
            ignoreTransparentCheck, squareRootCheck, popImageCheck;
    @FXML
    protected TextField scopeNameInput, rectLeftTopXInput, rectLeftTopYInput, rightBottomXInput, rightBottomYInput,
            circleCenterXInput, circleCenterYInput, circleRadiusInput;
    @FXML
    protected Button saveScopeButton, deletePointsButton, clearPointsButton,
            scopeOutlineFileButton, scopeOutlineShrinkButton, scopeOutlineExpandButton,
            clearColorsButton, deleteColorsButton, saveColorsButton;
    @FXML
    protected RadioButton scopeAllRadio, scopeMattingRadio, scopeRectangleRadio, scopeCircleRadio,
            scopePolygonRadio, scopeColorRadio, scopeRectangleColorRadio, scopeCircleColorRadio,
            scopeEllipseColorRadio, scopePolygonColorRadio, scopeOutlineRadio, scopeEllipseRadio,
            colorRGBRadio, colorGreenRadio, colorRedRadio, colorBlueRadio,
            colorSaturationRadio, colorHueRadio, colorBrightnessRadio;
    @FXML
    protected Label scopeTips, scopePointsLabel, scopeColorsLabel, pointsSizeLabel, colorsSizeLabel, rectangleLabel;

    protected void indicateScope() {
        if (isSettingValues || imageView == null || !scopeView.isVisible()) {
            return;
        }
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {
                private Image scopedImage;

                @Override
                protected boolean handle() {
                    try {
                        PixelsOperation pixelsOperation = PixelsOperationFactory.create(imageView.getImage(),
                                scope, PixelsOperation.OperationType.ShowScope);
                        if (!ignoreTransparentCheck.isSelected()) {
                            pixelsOperation.setSkipTransparent(false);
                        }
                        scopedImage = pixelsOperation.operateFxImage();
                        if (task == null || isCancelled()) {
                            return false;
                        }
                        return scopedImage != null;
                    } catch (Exception e) {
                        MyBoxLog.error(e.toString());
                        return false;
                    }
                }

                @Override
                protected void whenSucceeded() {
                    if (scope == null) {
                        return;
                    }
                    scopeView.setImage(scopedImage);
                    scopeView.setFitWidth(imageView.getFitWidth());
                    scopeView.setFitHeight(imageView.getFitHeight());
                    scopeView.setLayoutX(imageView.getLayoutX());
                    scopeView.setLayoutY(imageView.getLayoutY());

                    popImage();
                }

            };
//            parentController.handling(task);
            task.setSelf(task);
            Thread thread = new Thread(task);
            thread.setDaemon(false);
            thread.start();
        }
    }

    protected void popImage() {
        if (scopeAllRadio.isSelected() || !popImageCheck.isSelected()) {
            return;
        }
        if (imagePopController != null && imagePopController.getMyWindow().isShowing()) {
            return;
        }
        imagePopController = ImagePopController.openView(imageController, imageController.imageView);
    }

    @Override
    protected void popImageMenu(double x, double y) {
        if (!UserConfig.getBoolean(baseName + "ContextMenu", true)
                || imageView == null || imageView.getImage() == null) {
            return;
        }
        MenuImageScopeController.open((ImageManufactureScopeController) this, x, y);
    }

    public void popMenu() {
        try {
            Point2D localToScreen = scrollPane.localToScreen(scrollPane.getWidth() - 80, 80);
            MenuImageScopeController.open((ImageManufactureScopeController) this, localToScreen.getX(), localToScreen.getY());
        } catch (Exception e) {
            MyBoxLog.debug(e.toString());
        }
    }

    @FXML
    @Override
    public boolean menuAction() {
        imageController.menuAction();
        return true;
    }

    @FXML
    @Override
    public boolean popAction() {
        imageController.popAction();
        return true;
    }

}
