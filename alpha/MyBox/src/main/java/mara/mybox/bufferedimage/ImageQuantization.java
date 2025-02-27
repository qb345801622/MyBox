package mara.mybox.bufferedimage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import mara.mybox.data.StringTable;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fximage.FxColorTools;
import mara.mybox.tools.StringTools;
import static mara.mybox.value.Languages.message;
import mara.mybox.value.Languages;

/**
 * @Author Mara
 * @CreateDate 2019-2-13 14:44:03
 * @License Apache License Version 2.0
 */
// http://web.cs.wpi.edu/~matt/courses/cs563/talks/color_quant/CQindex.html
public class ImageQuantization extends PixelsOperation {

    public static enum QuantizationAlgorithm {
        RGBUniformQuantization, HSBUniformQuantization,
        PopularityQuantization, KMeansClustering
//        MedianCutQuantization, ANN
    }

    protected QuantizationAlgorithm algorithm;
    protected int quantizationSize, regionSize, weight1, weight2, weight3, intValue;
    protected boolean recordCount, ceil;
    protected Map<Color, Long> counts;
    protected List<ColorCount> sortedCounts;
    protected long totalCount;

    public ImageQuantization build() throws Exception {
        return this;
    }

    public void countColor(Color mappedColor) {
        if (recordCount && counts != null) {
            if (counts.containsKey(mappedColor)) {
                counts.put(mappedColor, counts.get(mappedColor) + 1);
            } else {
                counts.put(mappedColor, Long.valueOf(1));
            }
        }
    }

    public static class ColorCount {

        public Color color;
        public long count;

        public ColorCount(Color color, long count) {
            this.color = color;
            this.count = count;
        }
    }

    public List<ColorCount> sortCounts() {
        totalCount = 0;
        if (counts == null) {
            return null;
        }
        sortedCounts = new ArrayList<>();
        for (Color color : counts.keySet()) {
            sortedCounts.add(new ColorCount(color, counts.get(color)));
            totalCount += counts.get(color);
        }
        Collections.sort(sortedCounts, new Comparator<ColorCount>() {
            @Override
            public int compare(ColorCount v1, ColorCount v2) {
                long diff = v2.count - v1.count;
                if (diff == 0) {
                    return 0;
                } else if (diff > 0) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        return sortedCounts;
    }

    public StringTable countTable(String name) {
        try {
            sortedCounts = sortCounts();
            if (sortedCounts == null || totalCount == 0) {
                return null;
            }
            List<String> names = new ArrayList<>();
            names.addAll(Arrays.asList(Languages.message("ID"), Languages.message("PixelsNumber"),
                    Languages.message("Percentage"), Languages.message("Color"),
                    Languages.message("Red"), Languages.message("Green"), Languages.message("Blue"), Languages.message("Opacity"),
                    Languages.message("Hue"), Languages.message("Brightness"), Languages.message("Saturation")
            ));
            String title = Languages.message(algorithm.name());
            if (name != null) {
                title += "_" + name;
            }
            StringTable table = new StringTable(names, title, 3);
            int id = 1;
            for (ColorCount count : sortedCounts) {
                List<String> row = new ArrayList<>();
                javafx.scene.paint.Color color = ColorConvertTools.converColor(count.color);
                int red = (int) Math.round(color.getRed() * 255);
                int green = (int) Math.round(color.getGreen() * 255);
                int blue = (int) Math.round(color.getBlue() * 255);
                row.addAll(Arrays.asList((id++) + "", StringTools.format(count.count),
                        (int) (count.count * 100 / totalCount) + "%",
                        FxColorTools.color2rgba(color), red + " ", green + " ", blue + " ",
                        (int) Math.round(color.getOpacity() * 100) + "%",
                        Math.round(color.getHue()) + " ",
                        Math.round(color.getSaturation() * 100) + "%",
                        Math.round(color.getBrightness() * 100) + "%"
                ));
                table.add(row);
            }
            return table;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return null;
        }
    }

    @Override
    public Color operateColor(Color color) {
        return color;
    }

    public class PopularityRegion {

        protected long redAccum, greenAccum, blueAccum, pixelsCount;
        protected Color regionColor, averageColor;
    }

    /*
        get/set
     */
    public QuantizationAlgorithm getAlgorithm() {
        return algorithm;
    }

    public ImageQuantization setAlgorithm(QuantizationAlgorithm algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public int getQuantizationSize() {
        return quantizationSize;
    }

    public ImageQuantization setQuantizationSize(int quantizationSize) {
        this.quantizationSize = quantizationSize;
        return this;
    }

    public int getWeight1() {
        return weight1;
    }

    public ImageQuantization setWeight1(int weight1) {
        this.weight1 = weight1;
        return this;
    }

    public int getWeight2() {
        return weight2;
    }

    public ImageQuantization setWeight2(int weight2) {
        this.weight2 = weight2;
        return this;
    }

    public int getWeight3() {
        return weight3;
    }

    public ImageQuantization setWeight3(int weight3) {
        this.weight3 = weight3;
        return this;
    }

    public boolean isRecordCount() {
        return recordCount;
    }

    public ImageQuantization setRecordCount(boolean recordCount) {
        this.recordCount = recordCount;
        return this;
    }

    public Map<Color, Long> getCounts() {
        return counts;
    }

    public ImageQuantization setCounts(Map<Color, Long> counts) {
        this.counts = counts;
        return this;
    }

    public List<ColorCount> getSortedCounts() {
        return sortedCounts;
    }

    public ImageQuantization setSortedCounts(List<ColorCount> sortedCounts) {
        this.sortedCounts = sortedCounts;
        return this;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public ImageQuantization setTotalCount(long totalCount) {
        this.totalCount = totalCount;
        return this;
    }

    public int getIntValue() {
        return intValue;
    }

    public ImageQuantization setIntValue(int intValue) {
        this.intValue = intValue;
        return this;
    }

    public int getRegionSize() {
        return regionSize;
    }

    public ImageQuantization setRegionSize(int regionSize) {
        this.regionSize = regionSize;
        return this;
    }

    public boolean isCeil() {
        return ceil;
    }

    public ImageQuantization setCeil(boolean ceil) {
        this.ceil = ceil;
        return this;
    }

}
