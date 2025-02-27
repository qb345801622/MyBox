package mara.mybox.controller;

import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import mara.mybox.color.CIEDataTools;
import mara.mybox.color.RGBColorSpace;
import mara.mybox.color.RGBColorSpace.ColorSpaceType;
import static mara.mybox.color.RGBColorSpace.primariesTristimulus;
import static mara.mybox.color.RGBColorSpace.whitePointMatrix;
import mara.mybox.data.StringTable;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.NodeStyleTools;
import mara.mybox.tools.MatrixDoubleTools;
import mara.mybox.value.Languages;

/**
 * @Author Mara
 * @CreateDate 2019-5-20
 * @Description
 * @License Apache License Version 2.0
 */
public class RGBColorSpacesController extends ChromaticityBaseController {

    @FXML
    public RGBColorSpaceController rgbController;
    @FXML
    public WhitePointController whiteController;
    @FXML
    protected Button calculateButton, exportButton;
    @FXML
    protected HtmlTableController primariesController;

    public RGBColorSpacesController() {
        baseTitle = Languages.message("RGBColorSpaces");
        exportName = "RGBColorSpaces";
    }

    @Override
    public void initControls() {
        try {
            super.initControls();
            initData();

            initAdaptation();

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }

    }

    private void initAdaptation() {

        initOptions();

        calculateButton.disableProperty().bind(Bindings.isEmpty(scaleInput.textProperty())
                .or(scaleInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.redXInput.textProperty()))
                .or(rgbController.redXInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.redYInput.textProperty()))
                .or(rgbController.redYInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.redZInput.textProperty()))
                .or(rgbController.redZInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.greenXInput.textProperty()))
                .or(rgbController.greenXInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.greenYInput.textProperty()))
                .or(rgbController.greenYInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.greenZInput.textProperty()))
                .or(rgbController.greenZInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.blueXInput.textProperty()))
                .or(rgbController.blueXInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.blueYInput.textProperty()))
                .or(rgbController.blueYInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.blueZInput.textProperty()))
                .or(rgbController.blueZInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.whiteXInput.textProperty()))
                .or(rgbController.whiteXInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.whiteYInput.textProperty()))
                .or(rgbController.whiteYInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(rgbController.whiteZInput.textProperty()))
                .or(rgbController.whiteZInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(whiteController.xInput.textProperty()))
                .or(whiteController.xInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(whiteController.yInput.textProperty()))
                .or(whiteController.yInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
                .or(Bindings.isEmpty(whiteController.zInput.textProperty()))
                .or(whiteController.zInput.styleProperty().isEqualTo(NodeStyleTools.badStyle))
        );

    }

    private void initData() {
        synchronized (this) {
            if (task != null && !task.isQuit()) {
                return;
            }
            task = new SingletonTask<Void>() {

                private StringTable table;

                @Override
                protected boolean handle() {
                    table = RGBColorSpace.allTable();
                    return table != null;
                }

                @Override
                protected void whenSucceeded() {
                    primariesController.loadTable(table);
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
    public void calculateAction(ActionEvent event) {
        try {
            webView.getEngine().loadContent("");
            if (calculateButton.isDisabled()) {
                return;
            }
            double[][] primaries, sourceWhitePoint;
            if (rgbController.colorSpaceName != null) {
                ColorSpaceType cs = RGBColorSpace.type(rgbController.colorSpaceName);
                primaries = primariesTristimulus(cs);
                sourceWhitePoint = whitePointMatrix(cs);
            } else {
                primaries = new double[3][];
                primaries[0] = rgbController.red;
                primaries[1] = rgbController.green;
                primaries[2] = rgbController.blue;
                sourceWhitePoint = MatrixDoubleTools.columnVector(rgbController.white);
            }
            double[][] targetWhitePoint = MatrixDoubleTools.columnVector(whiteController.relative);
            if (primaries == null || sourceWhitePoint == null || targetWhitePoint == null) {
                return;
            }
            Map<String, Object> adapted = (Map<String, Object>) RGBColorSpace.primariesAdapted(primaries,
                    sourceWhitePoint, targetWhitePoint, algorithm, scale, true);
            double[][] adaptedPrimaries = (double[][]) adapted.get("adaptedPrimaries");
            double[][] normalized = MatrixDoubleTools.scale(CIEDataTools.normalize(adaptedPrimaries), scale);
            double[][] relative = MatrixDoubleTools.scale(CIEDataTools.relative(adaptedPrimaries), scale);
            String s = Languages.message("AdaptedPrimaries") + ": \n"
                    + Languages.message("NormalizedValuesCC") + " = \n"
                    + MatrixDoubleTools.print(normalized, 20, scale)
                    + Languages.message("RelativeValues") + " = \n"
                    + MatrixDoubleTools.print(relative, 20, scale)
                    + Languages.message("Tristimulus") + " = \n"
                    + MatrixDoubleTools.print(adaptedPrimaries, 20, scale)
                    + "\n----------------" + Languages.message("CalculationProcedure") + "----------------\n"
                    + Languages.message("ReferTo") + "： \n"
                    + "            http://brucelindbloom.com/index.html?WorkingSpaceInfo.html \n"
                    + "            http://brucelindbloom.com/index.html?Eqn_ChromAdapt.html \n\n"
                    + (String) adapted.get("procedure");
            webView.getEngine().loadContent("<pre>" + s + "</pre>");
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

}
