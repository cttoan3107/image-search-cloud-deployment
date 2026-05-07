/*
  Global role of this class:

  - Tests RGB histogram extraction
  - Verifies descriptor correctness and normalization
*/
package pdl.backend.imageprocessing;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class RgbHistogramExtractorTest {

    @Test
    void shouldExtractNormalizedRgbHistogram() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        image.setRGB(0, 0, 0xFF0000);
        image.setRGB(1, 0, 0x00FF00);
        image.setRGB(0, 1, 0x0000FF);
        image.setRGB(1, 1, 0xFFFFFF);

        RgbHistogramExtractor extractor = new RgbHistogramExtractor(4);
        ImageDescriptor descriptor = extractor.extract(image);

        assertEquals(DescriptorType.RGB, descriptor.getType());
        assertEquals(64, descriptor.size());

        double sum = 0.0;
        for (double v : descriptor.getValues()) {
            sum += v;
        }

        assertEquals(1.0, sum, 1e-9);
    }

    @Test
    void sameImageShouldHaveZeroDistance() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        image.setRGB(0, 0, 0xFF0000);
        image.setRGB(1, 0, 0x00FF00);
        image.setRGB(0, 1, 0x0000FF);
        image.setRGB(1, 1, 0xFFFFFF);

        RgbHistogramExtractor extractor = new RgbHistogramExtractor(4);
        ImageDescriptor d1 = extractor.extract(image);
        ImageDescriptor d2 = extractor.extract(image);

        SimilarityService similarityService = new SimilarityService();
        double distance = similarityService.euclideanDistance(d1, d2);

        assertEquals(0.0, distance, 1e-9);
    }
}
