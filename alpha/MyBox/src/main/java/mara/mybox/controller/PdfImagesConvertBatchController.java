package mara.mybox.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import mara.mybox.dev.MyBoxLog;
import static mara.mybox.fxml.NodeStyleTools.badStyle;
import mara.mybox.bufferedimage.ImageAttributes;
import mara.mybox.bufferedimage.ImageConvertTools;
import mara.mybox.fxml.NodeStyleTools;
import mara.mybox.tools.FileNameTools;
import mara.mybox.tools.FileTools;
import mara.mybox.tools.PdfTools;
import mara.mybox.tools.TmpFileTools;
import mara.mybox.value.AppValues;
import mara.mybox.value.AppVariables;

import mara.mybox.value.Languages;
import mara.mybox.value.UserConfig;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * @Author Mara
 * @CreateDate 2020-11-1
 * @License Apache License Version 2.0
 */
public class PdfImagesConvertBatchController extends BaseBatchPdfController {

    protected String format;
    protected PDDocument targetDoc;
    protected File tmpFile;
    protected ImageAttributes attributes;

    @FXML
    protected CheckBox copyAllCheck;
    @FXML
    protected ControlImageFormat formatController;

    public PdfImagesConvertBatchController() {
        baseTitle = Languages.message("PdfImagesConvertBatch");
    }

    @Override
    public void initControls() {
        try {
            super.initControls();

            copyAllCheck.setSelected(UserConfig.getBoolean(baseName + "CopyAll", false));
            copyAllCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                    UserConfig.setBoolean(baseName + "CopyAll", false);
                }
            });

            if (formatController != null) {
                formatController.setParameters(this, true);

                startButton.disableProperty().unbind();
                startButton.disableProperty().bind(Bindings.isEmpty(tableView.getItems())
                                .or(Bindings.isEmpty(targetPathInput.textProperty()))
                                .or(targetPathInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                                .or(formatController.qualitySelector.getEditor().styleProperty().isEqualTo(NodeStyleTools.badStyle))
                                .or(formatController.dpiSelector.getEditor().styleProperty().isEqualTo(NodeStyleTools.badStyle))
                                .or(formatController.profileInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                                .or(formatController.thresholdInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                );
            }
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @Override
    public boolean makeActualParameters() {
        if (!super.makeActualParameters()) {
            return false;
        }
        if (formatController != null) {
            attributes = formatController.attributes;
            format = attributes.getImageFormat();
        }
        return true;
    }

    @Override
    public boolean preHandlePages() {
        try {
            File tFile = makeTargetFile(FileNameTools.getFilePrefix(currentParameters.currentSourceFile.getName()),
                    ".pdf", currentParameters.currentTargetPath);
            currentTargetFile = tFile.getAbsolutePath();
            tmpFile = TmpFileTools.getTempFile();
            if (copyAllCheck.isSelected()) {
//                doc.save(tFile);
                targetDoc = doc;
            } else {
                targetDoc = PdfTools.createPDF(tmpFile, "MyBox v" + AppValues.AppVersion);
            }

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            targetDoc = null;
        }
        return targetDoc != null;
    }

    @Override
    public int handleCurrentPage() {
        int count = 0;
        try {
            PDPage sourcePage = doc.getPage(currentParameters.currentPage - 1);  // 0-based
            PDResources pdResources = sourcePage.getResources();
            pdResources.getXObjectNames();
            Iterable<COSName> iterable = pdResources.getXObjectNames();
            if (iterable == null) {
                return 0;
            }
            Iterator<COSName> pageIterator = iterable.iterator();

            while (pageIterator.hasNext()) {
                if (task == null || task.isCancelled()) {
                    break;
                }
                COSName cosName = pageIterator.next();
                if (!pdResources.isImageXObject(cosName)) {
                    continue;
                }
                PDImageXObject pdxObject = (PDImageXObject) pdResources.getXObject(cosName);
                BufferedImage sourceImage = pdxObject.getImage();
                PDImageXObject newObject = handleImage(sourceImage);
                if (newObject != null) {
                    pdResources.put(cosName, newObject);
                    count++;
                    if (isPreview) {
                        break;
                    }
                }
            }
            if (copyAllCheck.isSelected()) {
                targetDoc.getPage(currentParameters.currentPage - 1).setResources(pdResources);
            } else {
                targetDoc.addPage(sourcePage);
            }
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
        return count;

    }

    public PDImageXObject handleImage(BufferedImage sourceImage) {
        if (sourceImage == null) {
            return null;
        }
        try {
            PDImageXObject newObject = null;
            BufferedImage targetImage;
            if ("ico".equals(format) || "icon".equals(format)) {
                targetImage = ImageConvertTools.convertToIcon(sourceImage, attributes);
            } else {
                targetImage = ImageConvertTools.convertColorSpace(sourceImage, attributes);
            }
            if (targetImage != null) {
                newObject = PdfTools.imageObject(doc, format, targetImage);
            }
            return newObject;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return null;
        }
    }

    @Override
    public void postHandlePages() {
        try {
            if (targetDoc != null) {
                targetDoc.save(tmpFile);
                targetDoc.close();
                File tFile = new File(currentTargetFile);
                if (FileTools.rename(tmpFile, tFile)) {
                    targetFileGenerated(tFile);
                }
            }
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
        targetDoc = null;
    }
}
