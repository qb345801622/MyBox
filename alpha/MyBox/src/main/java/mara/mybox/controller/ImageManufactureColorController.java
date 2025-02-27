package mara.mybox.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import mara.mybox.bufferedimage.AlphaTools;
import mara.mybox.bufferedimage.ColorConvertTools;
import mara.mybox.bufferedimage.ImageScope;
import mara.mybox.bufferedimage.PixelsOperation;
import mara.mybox.bufferedimage.PixelsOperation.ColorActionType;
import mara.mybox.bufferedimage.PixelsOperation.OperationType;
import mara.mybox.bufferedimage.PixelsOperationFactory;
import mara.mybox.bufferedimage.ScaleTools;
import mara.mybox.controller.ImageManufactureController_Image.ImageOperation;
import mara.mybox.data.DoublePoint;
import mara.mybox.data.DoubleRectangle;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fximage.ImageViewTools;
import mara.mybox.fxml.NodeStyleTools;
import mara.mybox.fxml.ValidationTools;
import mara.mybox.fxml.WindowTools;
import mara.mybox.imagefile.ImageFileWriters;
import mara.mybox.value.AppVariables;
import mara.mybox.value.FileExtensions;
import mara.mybox.value.Fxmls;
import mara.mybox.value.Languages;
import mara.mybox.value.UserConfig;

/**
 * @Author Mara
 * @CreateDate 2019-9-1
 * @Description
 * @License Apache License Version 2.0
 */
public class ImageManufactureColorController extends ImageManufactureOperationController {

    protected int colorValue, colorDistance;
    private OperationType colorOperationType;
    private ColorActionType colorActionType;

    @FXML
    protected VBox setBox, replaceBox;
    @FXML
    protected HBox valueBox, alphaBox, valueColorBox;
    @FXML
    protected FlowPane opBox, originalColorPane, newColorPane;
    @FXML
    protected ToggleGroup colorGroup, distanceGroup;
    @FXML
    protected RadioButton colorReplaceRadio, colorColorRadio, colorRGBRadio,
            colorBrightnessRadio, colorHueRadio, colorSaturationRadio,
            colorRedRadio, colorGreenRadio, colorBlueRadio, colorOpacityRadio,
            colorYellowRadio, colorCyanRadio, colorMagentaRadio,
            distanceColorRadio, distanceHueRadio;
    @FXML
    protected ComboBox<String> valueSelector, distanceSelector;
    @FXML
    protected Label colorLabel, colorUnit, commentsLabel;
    @FXML
    protected Button colorIncreaseButton, colorDecreaseButton, colorFilterButton,
            colorInvertButton, demoButton, scopeButton;
    @FXML
    protected CheckBox preAlphaCheck, distanceExcludeCheck, squareRootCheck;
    @FXML
    protected ImageView preAlphaTipsView, distanceTipsView;
    @FXML
    protected ColorSet originalColorSetController, newColorSetController, valueColorSetController;

    @Override
    public void initPane() {
        try {
            colorGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                @Override
                public void changed(ObservableValue ov, Toggle oldValue, Toggle newValue) {
                    checkColorType();
                }
            });

            setButton.disableProperty().bind(valueSelector.getEditor().styleProperty().isEqualTo(NodeStyleTools.badStyle));
            colorIncreaseButton.disableProperty().bind(setButton.disableProperty());
            colorDecreaseButton.disableProperty().bind(setButton.disableProperty());
            colorInvertButton.disableProperty().bind(setButton.disableProperty());
            colorFilterButton.disableProperty().bind(setButton.disableProperty());

            if (imageController.imageInformation != null
                    && FileExtensions.NoAlphaImages.contains(imageController.imageInformation.getImageFormat())) {
                preAlphaCheck.setSelected(true);
                preAlphaCheck.setDisable(true);
            } else {
                preAlphaCheck.setSelected(false);
                preAlphaCheck.setDisable(false);
            }

            valueColorSetController.init(this, baseName + "ValueColor", Color.TRANSPARENT);
            originalColorSetController.init(this, baseName + "OriginalColor", Color.WHITE);
            newColorSetController.init(this, baseName + "NewColor", Color.TRANSPARENT);

            colorDistance = UserConfig.getInt(baseName + "ColorDistance", 20);
            colorDistance = colorDistance <= 0 ? 20 : colorDistance;
            distanceSelector.setValue(colorDistance + "");
            distanceSelector.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue ov, String oldValue, String newValue) {
                    checkDistance();
                }
            });

            squareRootCheck.setSelected(UserConfig.getBoolean(baseName + "ColorDistanceSquare", false));
            squareRootCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean oldv, Boolean newv) {
                    checkDistance();
                }
            });
            squareRootCheck.disableProperty().bind(distanceColorRadio.selectedProperty().not());

            distanceGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                @Override
                public void changed(ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) {
                    checkDistance();
                }
            });

            checkDistance();
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }

    }

    protected void checkDistance() {
        if (isSettingValues) {
            return;
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                int max = 255, step = 10;
                if (distanceColorRadio.isSelected()) {
                    if (squareRootCheck.isSelected()) {
                        max = 255 * 255;
                        step = 100;
                    }
                } else {
                    max = 360;
                }
                NodeStyleTools.setTooltip(distanceSelector, new Tooltip("0 ~ " + max));
                String value = distanceSelector.getValue();
                List<String> vList = new ArrayList<>();
                for (int i = 0; i <= max; i += step) {
                    vList.add(i + "");
                }
                isSettingValues = true;
                distanceSelector.getItems().clear();
                distanceSelector.getItems().addAll(vList);
                distanceSelector.setValue(value);
                isSettingValues = false;
                try {
                    int v = Integer.valueOf(value);
                    if (v == 0
                            && ((Color) originalColorSetController.rect.getFill()).equals(((Color) newColorSetController.rect.getFill()))) {
                        popError(Languages.message("OriginalNewSameColor"));
                        return;
                    }
                    if (v >= 0 && v <= max) {
                        colorDistance = v;
                        UserConfig.setInt(baseName + "ColorDistance", colorDistance);
                        distanceSelector.getEditor().setStyle(null);
                    } else {
                        distanceSelector.getEditor().setStyle(NodeStyleTools.badStyle);
                    }
                } catch (Exception e) {
                    distanceSelector.getEditor().setStyle(NodeStyleTools.badStyle);
                }
            }
        });
    }

    @Override
    protected void paneExpanded() {
        imageController.showRightPane();
        checkColorType();
    }

    private void checkColorType() {
        try {
            imageController.resetImagePane();
            setBox.getChildren().clear();
            opBox.getChildren().clear();
            valueBox.getChildren().clear();
            valueSelector.getItems().clear();
            ValidationTools.setEditorNormal(valueSelector);
            okButton.disableProperty().unbind();
            commentsLabel.setText("");
            colorUnit.setText("0-255");

            if (colorGroup.getSelectedToggle() == null) {
                return;
            }
            if (colorReplaceRadio.isSelected()) {
                imageController.imageTab();
                okButton.disableProperty().bind(distanceSelector.getEditor().styleProperty().isEqualTo(NodeStyleTools.badStyle));
                colorOperationType = OperationType.ReplaceColor;
                setBox.getChildren().addAll(replaceBox);
                commentsLabel.setText(Languages.message("ManufactureWholeImage"));

            } else {
                imageController.scopeTab();
                okButton.setDisable(true);
                commentsLabel.setText(Languages.message("DefineScopeAndManufacture"));
                opBox.getChildren().add(scopeButton);

                if (colorColorRadio.isSelected()) {
                    colorOperationType = OperationType.Color;
                    setBox.getChildren().addAll(valueBox, alphaBox, opBox);
                    valueBox.getChildren().addAll(valueColorBox);
                    opBox.getChildren().addAll(setButton);

                } else if (colorRGBRadio.isSelected()) {
                    colorOperationType = OperationType.RGB;
                    makeValuesBox(0, 255);
                    setBox.getChildren().addAll(valueBox, opBox);
                    valueBox.getChildren().addAll(colorLabel, valueSelector, colorUnit);
                    opBox.getChildren().addAll(colorIncreaseButton, colorDecreaseButton, colorInvertButton);

                } else if (colorBrightnessRadio.isSelected()) {
                    colorOperationType = OperationType.Brightness;
                    makeValuesBox(0, 100);
                    colorUnit.setText("0-100");
                    setBox.getChildren().addAll(valueBox, opBox);
                    valueBox.getChildren().addAll(colorLabel, valueSelector, colorUnit);
                    opBox.getChildren().addAll(setButton, colorIncreaseButton, colorDecreaseButton);

                } else if (colorSaturationRadio.isSelected()) {
                    colorOperationType = OperationType.Saturation;
                    makeValuesBox(0, 100);
                    colorUnit.setText("0-100");
                    setBox.getChildren().addAll(valueBox, opBox);
                    valueBox.getChildren().addAll(colorLabel, valueSelector, colorUnit);
                    opBox.getChildren().addAll(setButton, colorIncreaseButton, colorDecreaseButton);

                } else if (colorHueRadio.isSelected()) {
                    colorOperationType = OperationType.Hue;
                    makeValuesBox(0, 360);
                    colorUnit.setText("0-360");
                    setBox.getChildren().addAll(valueBox, opBox);
                    valueBox.getChildren().addAll(colorLabel, valueSelector, colorUnit);
                    opBox.getChildren().addAll(setButton, colorIncreaseButton, colorDecreaseButton);

                } else if (colorRedRadio.isSelected()) {
                    colorOperationType = OperationType.Red;
                    makeValuesBox(0, 255);
                    setBox.getChildren().addAll(valueBox, opBox);
                    valueBox.getChildren().addAll(colorLabel, valueSelector, colorUnit);
                    opBox.getChildren().addAll(setButton, colorIncreaseButton, colorDecreaseButton, colorFilterButton, colorInvertButton);

                } else if (colorGreenRadio.isSelected()) {
                    colorOperationType = OperationType.Green;
                    makeValuesBox(0, 255);
                    setBox.getChildren().addAll(valueBox, opBox);
                    valueBox.getChildren().addAll(colorLabel, valueSelector, colorUnit);
                    opBox.getChildren().addAll(setButton, colorIncreaseButton, colorDecreaseButton, colorFilterButton, colorInvertButton);

                } else if (colorBlueRadio.isSelected()) {
                    colorOperationType = OperationType.Blue;
                    makeValuesBox(0, 255);
                    setBox.getChildren().addAll(valueBox, opBox);
                    valueBox.getChildren().addAll(colorLabel, valueSelector, colorUnit);
                    opBox.getChildren().addAll(setButton, colorIncreaseButton, colorDecreaseButton, colorFilterButton, colorInvertButton);

                } else if (colorYellowRadio.isSelected()) {
                    colorOperationType = OperationType.Yellow;
                    makeValuesBox(0, 255);
                    setBox.getChildren().addAll(valueBox, opBox);
                    valueBox.getChildren().addAll(colorLabel, valueSelector, colorUnit);
                    opBox.getChildren().addAll(setButton, colorIncreaseButton, colorDecreaseButton, colorFilterButton, colorInvertButton);

                } else if (colorCyanRadio.isSelected()) {
                    colorOperationType = OperationType.Cyan;
                    makeValuesBox(0, 255);
                    setBox.getChildren().addAll(valueBox, opBox);
                    valueBox.getChildren().addAll(colorLabel, valueSelector, colorUnit);
                    opBox.getChildren().addAll(setButton, colorIncreaseButton, colorDecreaseButton, colorFilterButton, colorInvertButton);

                } else if (colorMagentaRadio.isSelected()) {
                    colorOperationType = OperationType.Magenta;
                    makeValuesBox(0, 255);
                    setBox.getChildren().addAll(valueBox, opBox);
                    valueBox.getChildren().addAll(colorLabel, valueSelector, colorUnit);
                    opBox.getChildren().addAll(setButton, colorIncreaseButton, colorDecreaseButton, colorFilterButton, colorInvertButton);

                } else if (colorOpacityRadio.isSelected()) {
                    colorOperationType = OperationType.Opacity;
                    makeValuesBox(0, 255);
                    setBox.getChildren().addAll(valueBox, alphaBox, opBox);
                    valueBox.getChildren().addAll(colorLabel, valueSelector, colorUnit);
                    opBox.getChildren().addAll(setButton, colorIncreaseButton, colorDecreaseButton);

                }

                setBox.getChildren().addAll(demoButton);
            }

            refreshStyle(setBox);

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    private void makeValuesBox(final int min, final int max) {
        try {
            List<String> valueList = new ArrayList<>();
            int step = (max - min) / 10;
            for (int v = min; v < max; v += step) {
                valueList.add(v + "");
            }
            valueList.add(max + "");
            valueSelector.getItems().addAll(valueList);
            valueSelector.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue ov, String oldValue, String newValue) {
                    try {
                        int v = Integer.valueOf(newValue);
                        if (v >= min && v <= max) {
                            colorValue = v;
                            ValidationTools.setEditorNormal(valueSelector);
                        } else {
                            ValidationTools.setEditorBadStyle(valueSelector);
                        }
                    } catch (Exception e) {
                        ValidationTools.setEditorBadStyle(valueSelector);
                    }
                }
            });
            valueSelector.getSelectionModel().select(valueList.size() / 2);

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @Override
    public void imageClicked(MouseEvent event, DoublePoint p) {
        if (imageController.isPickingColor) {
            Color color = ImageViewTools.imagePixel(p, imageView);
            if (color != null) {
                originalColorSetController.setColor(color);
                valueColorSetController.setColor(color);
            }
        }
    }

    @FXML
    public void increaseAction() {
        colorActionType = ColorActionType.Increase;
        applyChange();
    }

    @FXML
    public void decreaseAction() {
        colorActionType = ColorActionType.Decrease;
        applyChange();
    }

    @FXML
    @Override
    public void setAction() {
        colorActionType = ColorActionType.Set;
        applyChange();
    }

    @FXML
    @Override
    public void okAction() {
        colorActionType = ColorActionType.Set;
        applyChange();
    }

    @FXML
    public void filterAction() {
        colorActionType = ColorActionType.Filter;
        applyChange();
    }

    @FXML
    public void invertAction() {
        colorActionType = ColorActionType.Invert;
        applyChange();
    }

    private void applyChange() {
        if (imageController == null || colorOperationType == null || colorActionType == null) {
            return;
        }
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {

                private Image newImage;

                @Override
                protected boolean handle() {
                    PixelsOperation pixelsOperation;
                    if (colorOperationType == OperationType.ReplaceColor) {
                        java.awt.Color originalColor = ColorConvertTools.converColor((Color) originalColorSetController.rect.getFill());
                        java.awt.Color newColor = ColorConvertTools.converColor((Color) newColorSetController.rect.getFill());
                        ImageScope scope = new ImageScope(imageView.getImage());
                        scope.setScopeType(ImageScope.ScopeType.Color);
                        scope.setColorScopeType(ImageScope.ColorScopeType.Color);
                        List<java.awt.Color> colors = new ArrayList();
                        colors.add(originalColor);
                        scope.setColors(colors);
                        if (distanceColorRadio.isSelected()) {
                            scope.setColorScopeType(ImageScope.ColorScopeType.Color);
                            if (squareRootCheck.isSelected()) {
                                scope.setColorDistanceSquare(colorDistance);
                            } else {
                                scope.setColorDistance(colorDistance);
                            }
                        } else {
                            scope.setColorScopeType(ImageScope.ColorScopeType.Hue);
                            scope.setHsbDistance(colorDistance / 360.0f);
                        }
                        scope.setColorExcluded(distanceExcludeCheck.isSelected());
                        pixelsOperation = PixelsOperationFactory.create(imageView.getImage(),
                                scope, OperationType.ReplaceColor, ColorActionType.Set);
                        pixelsOperation.setColorPara1(originalColor);
                        pixelsOperation.setColorPara2(newColor);
                        if (originalColor.getRGB() == 0 || !scopeController.ignoreTransparentCheck.isSelected()) {
                            pixelsOperation.setSkipTransparent(false);
                        }

                    } else {
                        if (colorOperationType == OperationType.Opacity && preAlphaCheck.isSelected()) {
                            colorOperationType = OperationType.PreOpacity;
                        }
                        pixelsOperation = PixelsOperationFactory.create(imageView.getImage(),
                                scopeController.scope, colorOperationType, colorActionType);
                        pixelsOperation.setSkipTransparent(scopeController.ignoreTransparentCheck.isSelected());
                        switch (colorOperationType) {
                            case Color:
                                pixelsOperation.setColorPara1(ColorConvertTools.converColor((Color) valueColorSetController.rect.getFill()));
                                break;
                            case RGB:
                                pixelsOperation.setIntPara1(colorValue);
                                break;
                            case Hue:
                                pixelsOperation.setFloatPara1(colorValue / 360.0f);
                                break;
                            case Brightness:
                            case Saturation:
                                pixelsOperation.setFloatPara1(colorValue / 100.0f);
                                break;
                            case Red:
                            case Green:
                            case Blue:
                            case Yellow:
                            case Cyan:
                            case Magenta:
                            case Opacity:
                            case PreOpacity:
                                pixelsOperation.setIntPara1(colorValue);
                                break;
                        }
                    }
                    newImage = pixelsOperation.operateFxImage();
                    return true;
                }

                @Override
                protected void whenSucceeded() {
                    imageController.popSuccessful();
                    imageController.updateImage(ImageOperation.Color,
                            colorOperationType.name(), colorActionType.name(), newImage, cost);
                }
            };
            imageController.handling(task);
            task.setSelf(task);
            Thread thread = new Thread(task);
            thread.setDaemon(false);
            thread.start();
        }
    }

    @FXML
    protected void demo() {
        if (imageView.getImage() == null) {
            return;
        }
        imageController.popInformation(Languages.message("WaitAndHandling"));
        demoButton.setDisable(true);
        Task demoTask = new Task<Void>() {
            private List<String> files;

            @Override
            protected Void call() {

                try {
                    files = new ArrayList<>();
                    BufferedImage image = SwingFXUtils.fromFXImage(imageView.getImage(), null);
                    image = ScaleTools.scaleImageLess(image, 1000000);

                    PixelsOperation pixelsOperation;
                    BufferedImage bufferedImage;
                    String tmpFile;

                    BufferedImage outlineSource = SwingFXUtils.fromFXImage(new Image("img/About.png"), null);
                    ImageScope scope = new ImageScope(SwingFXUtils.toFXImage(image, null));
                    scope.setScopeType(ImageScope.ScopeType.Outline);
                    if (sourceFile != null) {
                        scope.setFile(sourceFile.getAbsolutePath());
                    }
                    scope.setRectangle(new DoubleRectangle(0, 0, image.getWidth(), image.getHeight()));
                    BufferedImage[] outline = AlphaTools.outline(outlineSource,
                            scope.getRectangle(), image.getWidth(), image.getHeight(),
                            false, ColorConvertTools.converColor(Color.WHITE), false);
                    scope.setOutlineSource(outlineSource);
                    scope.setOutline(outline[1]);

                    pixelsOperation = PixelsOperationFactory.create(image,
                            scope, OperationType.Color, ColorActionType.Set);
                    pixelsOperation.setColorPara1(ColorConvertTools.converColor(Color.LIGHTPINK));
                    bufferedImage = pixelsOperation.operate();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("Color") + "_" + Languages.message("Set") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    pixelsOperation = PixelsOperationFactory.create(image,
                            scope, OperationType.Brightness, ColorActionType.Increase);
                    pixelsOperation.setFloatPara1(0.5f);
                    bufferedImage = pixelsOperation.operate();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("Brightness") + "_" + Languages.message("Increase") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    pixelsOperation = PixelsOperationFactory.create(image,
                            scope, OperationType.Hue, ColorActionType.Decrease);
                    pixelsOperation.setFloatPara1(0.3f);
                    bufferedImage = pixelsOperation.operate();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("Hue") + "_" + Languages.message("Decrease") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    pixelsOperation = PixelsOperationFactory.create(image,
                            scope, OperationType.Saturation, ColorActionType.Increase);
                    pixelsOperation.setFloatPara1(0.5f);
                    bufferedImage = pixelsOperation.operate();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("Saturation") + "_" + Languages.message("Increase") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    pixelsOperation = PixelsOperationFactory.create(image,
                            scope, OperationType.Opacity, ColorActionType.Decrease);
                    pixelsOperation.setIntPara1(128);
                    bufferedImage = pixelsOperation.operate();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("Opacity") + "_" + Languages.message("Decrease") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    pixelsOperation = PixelsOperationFactory.create(image,
                            scope, OperationType.RGB, ColorActionType.Invert);
                    bufferedImage = pixelsOperation.operate();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("RGB") + "_" + Languages.message("Invert") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    pixelsOperation = PixelsOperationFactory.create(image,
                            scope, OperationType.Red, ColorActionType.Filter);
                    bufferedImage = pixelsOperation.operate();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("Red") + "_" + Languages.message("Filter") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    pixelsOperation = PixelsOperationFactory.create(image,
                            scope, OperationType.Yellow, ColorActionType.Increase);
                    pixelsOperation.setIntPara1(60);
                    bufferedImage = pixelsOperation.operate();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("Yellow") + "_" + Languages.message("Increase") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    pixelsOperation = PixelsOperationFactory.create(image,
                            scope, OperationType.Magenta, ColorActionType.Decrease);
                    pixelsOperation.setIntPara1(60);
                    bufferedImage = pixelsOperation.operate();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("Magenta") + "_" + Languages.message("Decrease") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                } catch (Exception e) {

                }
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                demoButton.setDisable(false);
                if (files.isEmpty()) {
                    return;
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ImagesBrowserController controller
                                    = (ImagesBrowserController) WindowTools.openStage(Fxmls.ImagesBrowserFxml);
                            controller.loadFiles(files);
                        } catch (Exception e) {
                            MyBoxLog.error(e.toString());
                        }
                    }
                });

            }

        };
        Thread thread = new Thread(demoTask);
        thread.setDaemon(false);
        thread.start();

    }

    @Override
    public boolean altFilter(KeyEvent event) {
        if (!event.isAltDown() || event.getCode() == null) {
            return false;
        }
        switch (event.getCode()) {
            case DIGIT1:
                if (opBox.getChildren().contains(setButton) && !setButton.isDisabled()) {
                    setAction();
                }
                return true;
            case DIGIT2:
                if (opBox.getChildren().contains(colorIncreaseButton) && !colorIncreaseButton.isDisabled()) {
                    increaseAction();
                }
                return true;
            case DIGIT3:
                if (opBox.getChildren().contains(colorDecreaseButton) && !colorDecreaseButton.isDisabled()) {
                    decreaseAction();
                }
                return true;
            case DIGIT4:
                if (opBox.getChildren().contains(colorFilterButton) && !colorFilterButton.isDisabled()) {
                    filterAction();
                }
                return true;
            case DIGIT5:
                if (opBox.getChildren().contains(colorInvertButton) && !colorInvertButton.isDisabled()) {
                    invertAction();
                }
                return true;
        }
        return super.altFilter(event);
    }

}
