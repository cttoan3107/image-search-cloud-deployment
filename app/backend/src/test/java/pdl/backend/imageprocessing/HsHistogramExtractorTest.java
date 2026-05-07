/*
  Global role of this class:

  - Tests HS histogram extraction
  - Ensures descriptors are normalized and comparable
*/

package pdl.backend.imageprocessing;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class HsHistogramExtractorTest {

    @Test
    void shouldExtractNormalizedHsHistogram() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        image.setRGB(0, 0, 0xFF0000); // red
        image.setRGB(1, 0, 0x00FF00); // green
        image.setRGB(0, 1, 0x0000FF); // blue
        image.setRGB(1, 1, 0xFFFFFF); // white

        HsHistogramExtractor extractor = new HsHistogramExtractor(12, 4);
        ImageDescriptor descriptor = extractor.extract(image);

        assertEquals(DescriptorType.HS, descriptor.getType());
        assertEquals(48, descriptor.size());

        double sum = 0.0;
        for (double v : descriptor.getValues()) {
            sum += v;
        }

        assertEquals(1.0, sum, 1e-9);
    }

    @Test
    void sameImageShouldHaveZeroDistanceForHsDescriptor() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        image.setRGB(0, 0, 0xFF0000);
        image.setRGB(1, 0, 0x00FF00);
        image.setRGB(0, 1, 0x0000FF);
        image.setRGB(1, 1, 0xFFFFFF);

        HsHistogramExtractor extractor = new HsHistogramExtractor(12, 4);
        ImageDescriptor d1 = extractor.extract(image);
        ImageDescriptor d2 = extractor.extract(image);

        SimilarityService similarityService = new SimilarityService();
        double distance = similarityService.euclideanDistance(d1, d2);

        assertEquals(0.0, distance, 1e-9);
    }

    @Test
    void differentImagesShouldHaveDifferentHsDescriptors() {
        BufferedImage redImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        BufferedImage blueImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                redImage.setRGB(x, y, 0xFF0000);
                blueImage.setRGB(x, y, 0x0000FF);
            }
        }

        HsHistogramExtractor extractor = new HsHistogramExtractor(12, 4);
        ImageDescriptor redDescriptor = extractor.extract(redImage);
        ImageDescriptor blueDescriptor = extractor.extract(blueImage);

        SimilarityService similarityService = new SimilarityService();
        double distance = similarityService.euclideanDistance(redDescriptor, blueDescriptor);

        assertTrue(distance > 0.0);
    }
}