package mara.mybox.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import mara.mybox.controller.ImageManufactureController_Image.ImageOperation;
import mara.mybox.db.data.ConvolutionKernel;
import mara.mybox.fxml.WindowTools;
import mara.mybox.bufferedimage.ImageContrast;
import mara.mybox.bufferedimage.ImageContrast.ContrastAlgorithm;
import mara.mybox.bufferedimage.ImageConvolution;
import mara.mybox.bufferedimage.ImageScope;
import mara.mybox.imagefile.ImageFileWriters;
import mara.mybox.value.AppVariables;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.bufferedimage.AlphaTools;
import mara.mybox.bufferedimage.ColorConvertTools;
import mara.mybox.bufferedimage.ScaleTools;
import static mara.mybox.value.Languages.message;

import mara.mybox.value.Fxmls;
import mara.mybox.value.Languages;

/**
 * @Author Mara
 * @CreateDate 2019-9-29
 * @Description
 * @License Apache License Version 2.0
 */
public class ImageManufactureEnhancementController extends ImageManufactureOperationController {

    @FXML
    protected ImageManufactureEnhancementOptionsController optionsController;
    @FXML
    protected Label commentsLabel;
    @FXML
    protected Button demoButton;

    @Override
    public void initPane() {
        try {

            optionsController.setValues(this);

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }

    }

    @Override
    protected void paneExpanded() {
        imageController.showRightPane();
        optionsController.checkEnhanceType();
    }

    @FXML
    @Override
    public void okAction() {
        if (imageController == null || optionsController.enhanceType == null) {
            return;
        }
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {

                private Image newImage;
                private String value = null;

                @Override
                protected boolean handle() {
                    ImageConvolution imageConvolution;
                    switch (optionsController.enhanceType) {
                        case Contrast:
                            ImageContrast imageContrast = new ImageContrast(imageView.getImage(), optionsController.contrastAlgorithm);
                            imageContrast.setIntPara1(optionsController.intPara1);
                            imageContrast.setIntPara2(optionsController.intPara2);
                            newImage = imageContrast.operateFxImage();
                            break;
                        case Convolution:
                            if (optionsController.kernel == null) {
                                int index = optionsController.stringSelector.getSelectionModel().getSelectedIndex();
                                if (optionsController.kernels == null || optionsController.kernels.isEmpty() || index < 0) {
                                    return false;
                                }
                                optionsController.kernel = optionsController.kernels.get(index);
                            }
                            imageConvolution = ImageConvolution.create().
                                    setImage(imageView.getImage()).setScope(scopeController.scope).
                                    setKernel(optionsController.kernel);
                            newImage = imageConvolution.operateFxImage();
                            optionsController.loadedKernel = null;
                            break;
                        case Smooth:
                            switch (optionsController.smoothAlgorithm) {
                                case AverageBlur:
                                    optionsController.kernel = ConvolutionKernel.makeAverageBlur(optionsController.intPara1);
                                    break;
                                case GaussianBlur:
                                    optionsController.kernel = ConvolutionKernel.makeGaussBlur(optionsController.intPara1);
                                    break;
                                case MotionBlur:
                                    optionsController.kernel = ConvolutionKernel.makeMotionBlur(optionsController.intPara1);
                                    break;
                                default:
                                    return false;
                            }
                            imageConvolution = ImageConvolution.create().
                                    setImage(imageView.getImage()).setScope(scopeController.scope).
                                    setKernel(optionsController.kernel);
                            newImage = imageConvolution.operateFxImage();
                            value = optionsController.intPara1 + "";
                            break;
                        case Sharpen:
                            switch (optionsController.sharpenAlgorithm) {
                                case EightNeighborLaplace:
                                    optionsController.kernel = ConvolutionKernel.MakeSharpenEightNeighborLaplace();
                                    break;
                                case FourNeighborLaplace:
                                    optionsController.kernel = ConvolutionKernel.MakeSharpenFourNeighborLaplace();
                                    break;
                                case UnsharpMasking:
                                    optionsController.kernel = ConvolutionKernel.makeUnsharpMasking(optionsController.intPara1);
                                    break;
                                default:
                                    return false;
                            }
                            imageConvolution = ImageConvolution.create().
                                    setImage(imageView.getImage()).setScope(scopeController.scope).
                                    setKernel(optionsController.kernel);
                            newImage = imageConvolution.operateFxImage();
                            break;
                        default:
                            return false;
                    }
                    if (task == null || isCancelled()) {
                        return false;
                    }
                    return newImage != null;
                }

                @Override
                protected void whenSucceeded() {
                    imageController.popSuccessful();
                    imageController.updateImage(ImageOperation.Effects, optionsController.enhanceType.name(), value, newImage, cost);
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

                    ImageContrast imageContrast = new ImageContrast(image,
                            ContrastAlgorithm.HSB_Histogram_Equalization);
                    BufferedImage bufferedImage = imageContrast.operateImage();
                    String tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("HSBHistogramEqualization") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    imageContrast = new ImageContrast(image,
                            ContrastAlgorithm.Gray_Histogram_Equalization);
                    bufferedImage = imageContrast.operateImage();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("GrayHistogramEqualization") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    imageContrast = new ImageContrast(image,
                            ContrastAlgorithm.Gray_Histogram_Stretching);
                    imageContrast.setIntPara1(100);
                    imageContrast.setIntPara2(100);
                    bufferedImage = imageContrast.operateImage();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("GrayHistogramStretching") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    imageContrast = new ImageContrast(image,
                            ContrastAlgorithm.Gray_Histogram_Shifting);
                    imageContrast.setIntPara1(40);
                    bufferedImage = imageContrast.operateImage();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("GrayHistogramShifting") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    BufferedImage outlineSource = SwingFXUtils.fromFXImage(new Image("img/ImageTools.png"), null);
                    ImageScope scope = new ImageScope(SwingFXUtils.toFXImage(image, null));
                    scope.setScopeType(ImageScope.ScopeType.Outline);
                    if (sourceFile != null) {
                        scope.setFile(sourceFile.getAbsolutePath());
                    }
                    BufferedImage[] outline = AlphaTools.outline(outlineSource,
                            scope.getRectangle(), image.getWidth(), image.getHeight(),
                            true, ColorConvertTools.converColor(Color.WHITE), false);
                    scope.setOutlineSource(outlineSource);
                    scope.setOutline(outline[1]);

                    ConvolutionKernel kernel = ConvolutionKernel.makeUnsharpMasking(3);
                    ImageConvolution imageConvolution = ImageConvolution.create().
                            setImage(image).setScope(scope).setKernel(kernel);
                    bufferedImage = imageConvolution.operateImage();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("UnsharpMasking") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    kernel = ConvolutionKernel.MakeSharpenFourNeighborLaplace();
                    imageConvolution = ImageConvolution.create().
                            setImage(image).setScope(scope).setKernel(kernel);
                    bufferedImage = imageConvolution.operateImage();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("FourNeighborLaplace") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    kernel = ConvolutionKernel.MakeSharpenEightNeighborLaplace();
                    imageConvolution = ImageConvolution.create().
                            setImage(image).setScope(scope).setKernel(kernel);
                    bufferedImage = imageConvolution.operateImage();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("EightNeighborLaplace") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    kernel = ConvolutionKernel.makeGaussBlur(3);
                    imageConvolution = ImageConvolution.create().
                            setImage(image).setScope(scope).setKernel(kernel);
                    bufferedImage = imageConvolution.operateImage();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("GaussianBlur") + ".png";
                    if (ImageFileWriters.writeImageFile(bufferedImage, tmpFile)) {
                        files.add(tmpFile);
                    }

                    kernel = ConvolutionKernel.makeAverageBlur(3);
                    imageConvolution = ImageConvolution.create().
                            setImage(image).setScope(scope).setKernel(kernel);
                    bufferedImage = imageConvolution.operateImage();
                    tmpFile = AppVariables.MyBoxTempPath + File.separator
                            + Languages.message("AverageBlur") + ".png";
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

}
