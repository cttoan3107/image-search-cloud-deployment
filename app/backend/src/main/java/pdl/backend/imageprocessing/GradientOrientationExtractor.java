/*
  Global role of this class:

  - Extracts gradient orientation features from an image
  - Captures edge direction information for similarity
*/
package pdl.backend.imageprocessing;

import java.awt.image.BufferedImage;

public class GradientOrientationExtractor {

    private final int orientationBins;

    public GradientOrientationExtractor() {
        this(9);
    }

    public GradientOrientationExtractor(int orientationBins) {
        if (orientationBins <= 0) {
            throw new IllegalArgumentException("orientationBins must be > 0.");
        }
        this.orientationBins = orientationBins;
    }

    public ImageDescriptor extract(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image must not be null.");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        if (width < 3 || height < 3) {
            throw new IllegalArgumentException("Image must be at least 3x3 to compute gradients.");
        }

        double[] histogram = new double[orientationBins];
        double totalWeight = 0.0;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double grayLeft = toGray(image.getRGB(x - 1, y));
                double grayRight = toGray(image.getRGB(x + 1, y));
                double grayUp = toGray(image.getRGB(x, y - 1));
                double grayDown = toGray(image.getRGB(x, y + 1));

                double gx = grayRight - grayLeft;
                double gy = grayDown - grayUp;

                double magnitude = Math.sqrt(gx * gx + gy * gy);

                if (magnitude == 0.0) {
                    continue;
                }

                double orientation = Math.atan2(gy, gx); // [-pi, pi]

                // Bring orientation into [0, pi)
                if (orientation < 0) {
                    orientation += Math.PI;
                }
                if (orientation >= Math.PI) {
                    orientation -= Math.PI;
                }

                int bin = getOrientationBin(orientation);
                histogram[bin] += magnitude;
                totalWeight += magnitude;
            }
        }

        normalize(histogram, totalWeight);

        return new ImageDescriptor(DescriptorType.GRADIENT, histogram);
    }

    private int getOrientationBin(double orientation) {
        int bin = (int) (orientation / Math.PI * orientationBins);
        if (bin < 0) {
            return 0;
        }
        if (bin >= orientationBins) {
            return orientationBins - 1;
        }
        return bin;
    }

    private double toGray(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        return 0.299 * r + 0.587 * g + 0.114 * b;
    }

    private void normalize(double[] histogram, double totalWeight) {
        if (totalWeight == 0.0) {
            return;
        }

        for (int i = 0; i < histogram.length; i++) {
            histogram[i] /= totalWeight;
        }
    }
}