package mara.mybox.bufferedimage;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.imagefile.ImageFileReaders;
import static mara.mybox.imagefile.ImageFileReaders.getReader;
import static mara.mybox.imagefile.ImageFileReaders.readBrokenImage;
import mara.mybox.imagefile.ImageFileWriters;
import mara.mybox.tools.FileNameTools;
import mara.mybox.tools.FileTools;
import mara.mybox.tools.TmpFileTools;
import mara.mybox.value.FileExtensions;
import mara.mybox.value.Languages;
import net.sf.image4j.codec.ico.ICODecoder;
import net.sf.image4j.codec.ico.ICOEncoder;

/**
 * @Author Mara
 * @CreateDate 2019-6-22 9:52:02
 * @License Apache License Version 2.0
 */
public class ImageConvertTools {

    // dpi(dot per inch) convert to dpm(dot per millimeter)
    public static int dpi2dpmm(int dpi) {
        return Math.round(dpi / 25.4f);
    }

    // dpi(dot per inch) convert to  ppm(dot per meter)
    public static int dpi2dpm(int dpi) {
        return Math.round(1000 * dpi / 25.4f);
    }

    // ppm(dot Per Meter)  convert to  dpi(dot per inch)
    public static int dpm2dpi(int dpm) {
        return Math.round(dpm * 25.4f / 1000f);
    }

    // dpi(dot per inch) convert to dpm(dot per centimeter)
    public static int dpi2dpcm(int dpi) {
        return Math.round(dpi / 2.54f);
    }

    // dpm(dot per centimeter) convert to  dpi(dot per inch)
    public static int dpcm2dpi(int dpcm) {
        return Math.round(dpcm * 2.54f);
    }

    public static int inch2cm(float inch) {
        return Math.round(inch / 2.54f);
    }

    // "pixel size in millimeters" convert to  dpi(dot per inch)
    public static int pixelSizeMm2dpi(float psmm) { //
        if (psmm == 0) {
            return 0;
        }
        return Math.round(25.4f / psmm);
    }

    //  dpi(dot per inch) convert to  "pixel size in millimeters"
    public static float dpi2pixelSizeMm(int dpi) { //
        if (dpi == 0) {
            return 0;
        }
        return 25.4f / dpi;
    }

    public static BufferedImage convertBinary(BufferedImage bufferedImage, ImageAttributes attributes) {
        try {
            if (bufferedImage == null || attributes == null) {
                return bufferedImage;
            }
            int color = bufferedImage.getType();
            ImageBinary imageBinary;
            if (attributes.getBinaryConversion() == ImageAttributes.BinaryConversion.BINARY_THRESHOLD
                    && attributes.getThreshold() >= 0) {
                imageBinary = new ImageBinary(bufferedImage, attributes.getThreshold());
            } else if (attributes.getBinaryConversion() == ImageAttributes.BinaryConversion.BINARY_OTSU) {
                imageBinary = new ImageBinary(bufferedImage, -1);
                imageBinary.setCalculate(true);
            } else if (color != BufferedImage.TYPE_BYTE_BINARY || attributes.isIsDithering()) {
                imageBinary = new ImageBinary(bufferedImage, -1);
            } else {
                return bufferedImage;
            }
            imageBinary.setIsDithering(attributes.isIsDithering());
            bufferedImage = imageBinary.operate();
            bufferedImage = ImageBinary.byteBinary(bufferedImage);
            return bufferedImage;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return bufferedImage;
        }
    }

    public static BufferedImage convertColorSpace(BufferedImage srcImage, ImageAttributes attributes) {
        try {
            if (srcImage == null || attributes == null) {
                return srcImage;
            }
            String targetFormat = attributes.getImageFormat();
            if ("ico".equals(targetFormat) || "icon".equals(targetFormat)) {
                return convertToIcon(srcImage, attributes);
            }
            BufferedImage tmpImage = srcImage;
            if (null != attributes.getAlpha()) {
                switch (attributes.getAlpha()) {
                    case PremultipliedAndKeep:
                        tmpImage = AlphaTools.premultipliedAlpha(srcImage, false);
                        break;
                    case PremultipliedAndRemove:
                        tmpImage = AlphaTools.premultipliedAlpha(srcImage, true);
                        break;
                    case Remove:
                        tmpImage = AlphaTools.removeAlpha(srcImage);
                        break;
                }
            }

            ICC_Profile targetProfile = attributes.getProfile();
            String csName = attributes.getColorSpaceName();
            if (targetProfile == null) {
                if (csName == null) {
                    return tmpImage;
                }
                if ("BlackOrWhite".equals(csName) || Languages.message("BlackOrWhite").equals(csName)) {
                    return convertBinary(tmpImage, attributes);
                } else {
                    if (Languages.message("Gray").equals(csName)) {
                        csName = "Gray";
                    }
                    targetProfile = ImageColorSpace.internalProfileByName(csName);
                    attributes.setProfile(targetProfile);
                    attributes.setProfileName(csName);
                }
            }

            ICC_ColorSpace targetColorSpace = new ICC_ColorSpace(targetProfile);
            ColorConvertOp c = new ColorConvertOp(targetColorSpace, null);
            BufferedImage targetImage = c.filter(tmpImage, null);
            return targetImage;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return null;
        }
    }

    public static boolean convertColorSpace(File srcFile, ImageAttributes attributes, File targetFile) {
        try {
            if (srcFile == null || targetFile == null || attributes == null) {
                return false;
            }

            String targetFormat = attributes.getImageFormat();
            if ("ico".equals(targetFormat) || "icon".equals(targetFormat)) {
                return convertToIcon(srcFile, attributes, targetFile);
            }
            String sourceFormat = FileNameTools.getFileSuffix(srcFile);
            if ("ico".equals(sourceFormat) || "icon".equals(sourceFormat)) {
                return convertFromIcon(srcFile, attributes, targetFile);
            }
            boolean supportMultiFrames = FileExtensions.MultiFramesImages.contains(targetFormat);
            ImageWriter writer = ImageFileWriters.getWriter(targetFormat);
            if (writer == null) {
                return false;
            }
            ImageWriteParam param = ImageFileWriters.getWriterParam(attributes, writer);
            File tmpFile = TmpFileTools.getTempFile();
            try ( ImageInputStream iis = ImageIO.createImageInputStream(srcFile);
                     ImageOutputStream out = ImageIO.createImageOutputStream(tmpFile)) {
                ImageReader reader = ImageFileReaders.getReader(iis, sourceFormat);
                if (reader == null) {
                    writer.dispose();
                    return false;
                }
                reader.setInput(iis, false);
                writer.setOutput(out);
                if (supportMultiFrames) {
                    writer.prepareWriteSequence(null);
                }
                BufferedImage bufferedImage;
                int index = 0;
                while (true) {
                    try {
                        bufferedImage = reader.read(index);
                    } catch (Exception e) {
                        if (e.toString().contains("java.lang.IndexOutOfBoundsException")) {
                            break;
                        }
                        bufferedImage = readBrokenImage(e, srcFile.getAbsolutePath(), index, null, -1);
                    }
                    if (bufferedImage == null) {
                        continue;
                    }
                    bufferedImage = ImageConvertTools.convertColorSpace(bufferedImage, attributes);
                    if (bufferedImage == null) {
                        continue;
                    }
                    IIOMetadata metaData
                            = ImageFileWriters.getWriterMetaData(targetFormat, attributes, bufferedImage, writer, param);
                    if (supportMultiFrames) {
                        writer.writeToSequence(new IIOImage(bufferedImage, null, metaData), param);
                    } else {
                        writer.write(metaData, new IIOImage(bufferedImage, null, metaData), param);
                    }
                    index++;
                }
                if (supportMultiFrames) {
                    writer.endWriteSequence();
                }
                out.flush();
                reader.dispose();
            }
            writer.dispose();
            return FileTools.rename(tmpFile, targetFile);

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return false;
        }
    }

    public static BufferedImage convertToPNG(BufferedImage srcImage) {
        try {
            if (srcImage == null
                    || srcImage.getType() == BufferedImage.TYPE_4BYTE_ABGR
                    || srcImage.getType() == BufferedImage.TYPE_4BYTE_ABGR_PRE) {
                return srcImage;
            }
            ICC_Profile targetProfile = ICC_Profile.getInstance(ICC_ColorSpace.CS_sRGB);
            ICC_ColorSpace targetColorSpace = new ICC_ColorSpace(targetProfile);
            ColorConvertOp c = new ColorConvertOp(targetColorSpace, null);
            BufferedImage targetImage = c.filter(srcImage, null);
            return targetImage;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return null;
        }
    }

    public static boolean convertToIcon(File srcFile, ImageAttributes attributes, File targetFile) {
        try {
            if (srcFile == null || targetFile == null) {
                return false;
            }
            List<BufferedImage> images = new ArrayList();
            try ( ImageInputStream iis = ImageIO.createImageInputStream(new BufferedInputStream(new FileInputStream(srcFile)))) {
                ImageReader reader = getReader(iis, FileNameTools.getFileSuffix(srcFile));
                if (reader == null) {
                    return false;
                }
                reader.setInput(iis, false);
                BufferedImage bufferedImage;
                int index = 0;
                while (true) {
                    try {
                        bufferedImage = reader.read(index);
                    } catch (Exception e) {
                        if (e.toString().contains("java.lang.IndexOutOfBoundsException")) {
                            break;
                        }
                        bufferedImage = readBrokenImage(e, srcFile.getAbsolutePath(), index, null, -1);
                    }
                    if (bufferedImage != null) {
                        bufferedImage = convertToIcon(bufferedImage, attributes);
                        images.add(bufferedImage);
                        index++;
                    } else {
                        break;
                    }
                }
                reader.dispose();
            }
            if (images.isEmpty()) {
                return false;
            }
            ICOEncoder.write(images, targetFile);
            return true;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return false;
        }
    }

    public static boolean convertToIcon(BufferedImage bufferedImage, ImageAttributes attributes, File targetFile) {
        try {
            if (bufferedImage == null || targetFile == null) {
                return false;
            }
            BufferedImage icoImage = convertToIcon(bufferedImage, attributes);
            ICOEncoder.write(icoImage, targetFile);
            return true;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return false;
        }
    }

    public static BufferedImage convertToIcon(BufferedImage bufferedImage, ImageAttributes attributes) {
        try {
            if (bufferedImage == null) {
                return null;
            }
            int width = 0;
            if (attributes != null) {
                width = attributes.getWidth();
            }
            if (width <= 0) {
                width = Math.min(512, bufferedImage.getWidth());
            }
            BufferedImage icoImage = ScaleTools.scaleImageWidthKeep(bufferedImage, width);
            return icoImage;
        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return null;
        }
    }

    public static boolean convertFromIcon(File srcFile, ImageAttributes attributes, File targetFile) {
        try {
            if (srcFile == null || targetFile == null) {
                return false;
            }
            List<BufferedImage> images = ICODecoder.read(srcFile);
            if (images == null || images.isEmpty()) {
                return false;
            }
            String targetFormat = attributes.getImageFormat();
            boolean supportMultiFrames = FileExtensions.MultiFramesImages.contains(targetFormat);
            ImageWriter writer = ImageFileWriters.getWriter(targetFormat);
            ImageWriteParam param = ImageFileWriters.getWriterParam(attributes, writer);
            File tmpFile = TmpFileTools.getTempFile();
            try ( ImageOutputStream out = ImageIO.createImageOutputStream(tmpFile)) {
                writer.setOutput(out);
                int num;
                if (supportMultiFrames) {
                    num = images.size();
                    writer.prepareWriteSequence(null);
                } else {
                    num = 1;
                }
                BufferedImage bufferedImage;
                for (int i = 0; i < num; ++i) {
                    bufferedImage = images.get(i);
                    bufferedImage = ImageConvertTools.convertColorSpace(bufferedImage, attributes);
                    if (bufferedImage == null) {
                        continue;
                    }
                    IIOMetadata metaData
                            = ImageFileWriters.getWriterMetaData(targetFormat, attributes, bufferedImage, writer, param);
                    if (supportMultiFrames) {
                        writer.writeToSequence(new IIOImage(bufferedImage, null, metaData), param);
                    } else {
                        writer.write(metaData, new IIOImage(bufferedImage, null, metaData), param);
                    }
                }
                if (supportMultiFrames) {
                    writer.endWriteSequence();
                }
                out.flush();
            }
            writer.dispose();

            return FileTools.rename(tmpFile, targetFile);

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
            return false;
        }
    }

    // https://stackoverflow.com/questions/2408613/unable-to-read-jpeg-image-using-imageio-readfile-file/12132805#12132805
    // https://stackoverflow.com/questions/50798014/determining-color-space-for-jpeg/50861048?r=SearchResults#50861048
    // https://blog.idrsolutions.com/2011/10/ycck-color-conversion-in-pdf-files/
    // https://community.oracle.com/message/10003648?tstart=0
    public static void ycck2cmyk(WritableRaster rast, boolean invertedColors) {
        int w = rast.getWidth(), h = rast.getHeight();
        double c, m, y, k;
        double Y, Cb, Cr, K;
        int[] pixels = null;
        for (int row = 0; row < h; row++) {
            pixels = rast.getPixels(0, row, w, 1, pixels);
            for (int i = 0; i < pixels.length; i += 4) {
                Y = pixels[i];
                Cb = pixels[i + 1];
                Cr = pixels[i + 2];
                K = pixels[i + 3];

                c = Math.min(255, Math.max(0, 255 - (Y + 1.402 * Cr - 179.456)));
                m = Math.min(255, Math.max(0, 255 - (Y - 0.34414 * Cb - 0.71414 * Cr + 135.45984)));
                y = Math.min(255, Math.max(0, 255 - (Y + 1.7718d * Cb - 226.816)));
                k = K;

                if (invertedColors) {
                    pixels[i] = (byte) (255 - c);
                    pixels[i + 1] = (byte) (255 - m);
                    pixels[i + 2] = (byte) (255 - y);
                    pixels[i + 3] = (byte) (255 - k);
                } else {
                    pixels[i] = (byte) c;
                    pixels[i + 1] = (byte) m;
                    pixels[i + 2] = (byte) y;
                    pixels[i + 3] = (byte) k;
                }
            }
            rast.setPixels(0, row, w, 1, pixels);
        }
    }

    public static void invertPixelValue(WritableRaster raster) {
        int height = raster.getHeight();
        int width = raster.getWidth();
        int stride = width * 4;
        int[] pixelRow = new int[stride];
        for (int h = 0; h < height; h++) {
            raster.getPixels(0, h, width, 1, pixelRow);
            for (int x = 0; x < stride; x++) {
                pixelRow[x] = 255 - pixelRow[x];
            }
            raster.setPixels(0, h, width, 1, pixelRow);
        }
    }

    public static Raster ycck2cmyk(final byte[] buffer, final int w, final int h) throws IOException {
        final int pixelCount = w * h * 4;
        for (int i = 0; i < pixelCount; i = i + 4) {
            int y = (buffer[i] & 255);
            int cb = (buffer[i + 1] & 255);
            int cr = (buffer[i + 2] & 255);
//            int k = (buffer[i + 3] & 255);

            int r = Math.max(0, Math.min(255, (int) (y + 1.402 * cr - 179.456)));
            int g = Math.max(0, Math.min(255, (int) (y - 0.34414 * cb - 0.71414 * cr + 135.95984)));
            int b = Math.max(0, Math.min(255, (int) (y + 1.772 * cb - 226.316)));

            buffer[i] = (byte) (255 - r);
            buffer[i + 1] = (byte) (255 - g);
            buffer[i + 2] = (byte) (255 - b);
        }

        return Raster.createInterleavedRaster(new DataBufferByte(buffer, pixelCount), w, h, w * 4, 4, new int[]{0, 1, 2, 3}, null);
    }

    public static BufferedImage rgb2cmyk(ICC_Profile cmykProfile,
            BufferedImage inImage) throws IOException {
        if (cmykProfile == null) {
            cmykProfile = ImageColorSpace.eciCmykProfile();
        }
        ICC_ColorSpace cmykCS = new ICC_ColorSpace(cmykProfile);
        ColorConvertOp rgb2cmyk = new ColorConvertOp(cmykCS, null);
        return rgb2cmyk.filter(inImage, null);
    }

    public static BufferedImage cmyk2rgb(Raster cmykRaster,
            ICC_Profile cmykProfile) throws IOException {
        if (cmykProfile == null) {
            cmykProfile = ImageColorSpace.eciCmykProfile();
        }
        ICC_ColorSpace cmykCS = new ICC_ColorSpace(cmykProfile);
        BufferedImage rgbImage = new BufferedImage(cmykRaster.getWidth(), cmykRaster.getHeight(), BufferedImage.TYPE_INT_RGB);
        WritableRaster rgbRaster = rgbImage.getRaster();
        ColorSpace rgbCS = rgbImage.getColorModel().getColorSpace();
        ColorConvertOp cmykToRgb = new ColorConvertOp(cmykCS, rgbCS, null);
        cmykToRgb.filter(cmykRaster, rgbRaster);
        return rgbImage;
    }

    // https://bugs.openjdk.java.net/browse/JDK-8041125
    public static BufferedImage cmyk2rgb(final byte[] buffer, final int w,
            final int h) throws IOException {

        final ColorSpace CMYK = ImageColorSpace.adobeCmykColorSpace();

        final int pixelCount = w * h * 4;
        int C, M, Y, K, lastC = -1, lastM = -1, lastY = -1, lastK = -1;

        int j = 0;
        float[] RGB = new float[]{0f, 0f, 0f};
        //turn YCC in Buffer to CYM using profile
        for (int i = 0; i < pixelCount; i = i + 4) {

            C = (buffer[i] & 255);
            M = (buffer[i + 1] & 255);
            Y = (buffer[i + 2] & 255);
            K = (buffer[i + 3] & 255);

            // System.out.println(C+" "+M+" "+Y+" "+K);
            if (C == lastC && M == lastM && Y == lastY && K == lastK) {
                //no change so use last value
            } else { //new value

                RGB = CMYK.toRGB(new float[]{C / 255f, M / 255f, Y / 255f, K / 255f});

                //flag so we can just reuse if next value the same
                lastC = C;
                lastM = M;
                lastY = Y;
                lastK = K;
            }

            //put back as CMY
            buffer[j] = (byte) (RGB[0] * 255f);
            buffer[j + 1] = (byte) (RGB[1] * 255f);
            buffer[j + 2] = (byte) (RGB[2] * 255f);

            j = j + 3;

        }

        /**
         * create CMYK raster from buffer
         */
        final Raster raster = Raster.createInterleavedRaster(new DataBufferByte(buffer, j), w, h, w * 3, 3, new int[]{0, 1, 2}, null);

        //data now sRGB so create image
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        image.setData(raster);

        return image;
    }

}
