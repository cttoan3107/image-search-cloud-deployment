/*
  Global role of this class:

  - Extracts a histogram based on Hue and Saturation
  - Represents color distribution in HS color space
*/
package pdl.backend.imageprocessing;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class HsHistogramExtractor {

    private final int hueBins;
    private final int saturationBins;

    public HsHistogramExtractor() {
        this(12, 4);
    }

    public HsHistogramExtractor(int hueBins, int saturationBins) {
        if (hueBins <= 0) {
            throw new IllegalArgumentException("hueBins must be > 0");
        }
        if (saturationBins <= 0) {
            throw new IllegalArgumentException("saturationBins must be > 0");
        }
        this.hueBins = hueBins;
        this.saturationBins = saturationBins;
    }

    public ImageDescriptor extract(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image must not be null.");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        double[] histogram = new double[hueBins * saturationBins];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                float[] hsb = Color.RGBtoHSB(r, g, b, null);

                float hue = hsb[0];         // [0, 1]
                float saturation = hsb[1];  // [0, 1]

                int hueBin = Math.min((int) (hue * hueBins), hueBins - 1);
                int saturationBin = Math.min((int) (saturation * saturationBins), saturationBins - 1);

                int index = hueBin * saturationBins + saturationBin;
                histogram[index] += 1.0;
            }
        }

        normalize(histogram, totalPixels);

        return new ImageDescriptor(DescriptorType.HS, histogram);
    }

    private void normalize(double[] histogram, int totalPixels) {
        if (totalPixels == 0) {
            return;
        }

        for (int i = 0; i < histogram.length; i++) {
            histogram[i] /= totalPixels;
        }
    }
}