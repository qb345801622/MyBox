package mara.mybox.controller;

import javafx.fxml.FXML;
import mara.mybox.data.DoubleCircle;
import mara.mybox.data.DoubleEllipse;
import mara.mybox.data.DoubleRectangle;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.NodeStyleTools;
import static mara.mybox.tools.DoubleTools.scale;
import mara.mybox.value.Languages;

/**
 * @Author Mara
 * @CreateDate 2021-8-13
 * @License Apache License Version 2.0
 */
public class ImageManufactureScopeController_Area extends ImageManufactureScopeController_Base {

    @FXML
    public void okRectangle() {
        try {
            if (scope == null) {
                return;
            }
            double x1, y1, x2, y2;
            try {
                x1 = Double.parseDouble(rectLeftTopXInput.getText());
                rectLeftTopXInput.setStyle(null);
            } catch (Exception e) {
                rectLeftTopXInput.setStyle(NodeStyleTools.badStyle);
                return;
            }
            try {
                y1 = Double.parseDouble(rectLeftTopYInput.getText());
                rectLeftTopYInput.setStyle(null);
            } catch (Exception e) {
                rectLeftTopYInput.setStyle(NodeStyleTools.badStyle);
                return;
            }
            try {
                x2 = Double.parseDouble(rightBottomXInput.getText());
                rightBottomXInput.setStyle(null);
            } catch (Exception e) {
                rightBottomXInput.setStyle(NodeStyleTools.badStyle);
                return;
            }
            try {
                y2 = Double.parseDouble(rightBottomYInput.getText());
                rightBottomYInput.setStyle(null);
            } catch (Exception e) {
                rightBottomYInput.setStyle(NodeStyleTools.badStyle);
                return;
            }
            DoubleRectangle rect = new DoubleRectangle(x1, y1, x2, y2);
            if (!rect.isValid()) {
                popError(Languages.message("InvalidData"));
                return;
            }
            switch (scope.getScopeType()) {
                case Rectangle:
                case RectangleColor:
                    maskRectangleData = rect;
                    scope.setRectangle(maskRectangleData.cloneValues());
                    drawMaskRectangleLineAsData();
                    break;
                case Ellipse:
                case EllipseColor:
                    maskEllipseData = new DoubleEllipse(x1, y1, x2, y2);
                    scope.setEllipse(maskEllipseData.cloneValues());
                    drawMaskEllipseLineAsData();
                    break;
                default:
                    return;
            }

            indicateScope();

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @FXML
    public void okCircle() {
        try {
            if (scope == null) {
                return;
            }
            double x, y, r;
            try {
                x = Double.parseDouble(circleCenterXInput.getText());
                circleCenterXInput.setStyle(null);
            } catch (Exception e) {
                circleCenterXInput.setStyle(NodeStyleTools.badStyle);
                return;
            }
            try {
                y = Double.parseDouble(circleCenterYInput.getText());
                circleCenterYInput.setStyle(null);
            } catch (Exception e) {
                circleCenterYInput.setStyle(NodeStyleTools.badStyle);
                return;
            }
            try {
                r = Double.parseDouble(circleRadiusInput.getText());
                circleRadiusInput.setStyle(null);
            } catch (Exception e) {
                circleRadiusInput.setStyle(NodeStyleTools.badStyle);
                return;
            }
            DoubleCircle circle = new DoubleCircle(x, y, r);
            if (!circle.isValid()) {
                popError(Languages.message("InvalidData"));
                return;
            }
            switch (scope.getScopeType()) {
                case Circle:
                case CircleColor:
                    maskCircleData = circle;
                    scope.setCircle(maskCircleData.cloneValues());
                    drawMaskCircleLineAsData();
                    break;
                default:
                    return;
            }
            indicateScope();

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @Override
    public boolean drawMaskRectangleLineAsData() {
        if (maskRectangleLine == null || !maskPane.getChildren().contains(maskRectangleLine)
                || maskRectangleData == null
                || imageView == null || imageView.getImage() == null) {
            return false;
        }
        if (!super.drawMaskRectangleLineAsData()) {
            return false;
        }
        rectLeftTopXInput.setText(scale(maskRectangleData.getSmallX(), 2) + "");
        rectLeftTopYInput.setText(scale(maskRectangleData.getSmallY(), 2) + "");
        rightBottomXInput.setText(scale(maskRectangleData.getBigX(), 2) + "");
        rightBottomYInput.setText(scale(maskRectangleData.getBigY(), 2) + "");
        return true;
    }

    @Override
    public boolean drawMaskCircleLineAsData() {
        if (maskCircleLine == null || !maskCircleLine.isVisible()
                || maskCircleData == null
                || imageView == null || imageView.getImage() == null) {
            return false;
        }
        if (!super.drawMaskCircleLineAsData()) {
            return false;
        }
        circleCenterXInput.setText(scale(maskCircleData.getCenterX(), 2) + "");
        circleCenterYInput.setText(scale(maskCircleData.getCenterY(), 2) + "");
        circleRadiusInput.setText(scale(maskCircleData.getRadius(), 2) + "");
        return true;
    }

    @Override
    public boolean drawMaskEllipseLineAsData() {
        if (maskEllipseLine == null || !maskEllipseLine.isVisible()
                || maskEllipseData == null
                || imageView == null || imageView.getImage() == null) {
            return false;
        }
        if (!super.drawMaskEllipseLineAsData()) {
            return false;
        }
        DoubleRectangle rect = maskEllipseData.getRectangle();
        rectLeftTopXInput.setText(scale(rect.getSmallX(), 2) + "");
        rectLeftTopYInput.setText(scale(rect.getSmallY(), 2) + "");
        rightBottomXInput.setText(scale(rect.getBigX(), 2) + "");
        rightBottomYInput.setText(scale(rect.getBigY(), 2) + "");
        return true;
    }

}
