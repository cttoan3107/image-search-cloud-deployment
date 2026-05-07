/*
  Global role of this class:

  - Extracts a color histogram based on RGB values
  - Converts an image into a descriptor vector
*/
package pdl.backend.imageprocessing;

import java.awt.image.BufferedImage;

public class RgbHistogramExtractor {

    private final int binsPerChannel;

    public RgbHistogramExtractor() {
        this(4);
    }

    public RgbHistogramExtractor(int binsPerChannel) {
        if (binsPerChannel <= 0) {
            throw new IllegalArgumentException("binsPerChannel must be > 0");
        }
        this.binsPerChannel = binsPerChannel;
    }

    public ImageDescriptor extract(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image must not be null.");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        int histogramSize = binsPerChannel * binsPerChannel * binsPerChannel;
        double[] histogram = new double[histogramSize];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int rBin = Math.min((r * binsPerChannel) / 256, binsPerChannel - 1);
                int gBin = Math.min((g * binsPerChannel) / 256, binsPerChannel - 1);
                int bBin = Math.min((b * binsPerChannel) / 256, binsPerChannel - 1);

                int index = rBin * binsPerChannel * binsPerChannel
                        + gBin * binsPerChannel
                        + bBin;

                histogram[index] += 1.0;
            }
        }

        normalize(histogram, totalPixels);

        return new ImageDescriptor(DescriptorType.RGB, histogram);
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
