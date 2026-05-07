/*
  Global role of this class:
  - Tests gradient descriptor extraction
  - Validates normalization and similarity behavior
*/
package pdl.backend.imageprocessing;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class GradientOrientationExtractorTest {

    @Test
    void shouldExtractNormalizedGradientHistogram() {
        BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);

        // Vertical edge: left black, right white
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                image.setRGB(x, y, x < 2 ? 0x000000 : 0xFFFFFF);
            }
        }

        GradientOrientationExtractor extractor = new GradientOrientationExtractor(9);
        ImageDescriptor descriptor = extractor.extract(image);

        assertEquals(DescriptorType.GRADIENT, descriptor.getType());
        assertEquals(9, descriptor.size());

        double sum = 0.0;
        for (double v : descriptor.getValues()) {
            sum += v;
        }

        assertEquals(1.0, sum, 1e-9);
    }

    @Test
    void sameImageShouldHaveZeroDistanceForGradientDescriptor() {
        BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                image.setRGB(x, y, x < 2 ? 0x000000 : 0xFFFFFF);
            }
        }

        GradientOrientationExtractor extractor = new GradientOrientationExtractor(9);
        ImageDescriptor d1 = extractor.extract(image);
        ImageDescriptor d2 = extractor.extract(image);

        SimilarityService similarityService = new SimilarityService();
        double distance = similarityService.euclideanDistance(d1, d2);

        assertEquals(0.0, distance, 1e-9);
    }

    @Test
    void differentEdgeDirectionsShouldGiveDifferentDescriptors() {
        BufferedImage verticalEdge = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        BufferedImage horizontalEdge = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                verticalEdge.setRGB(x, y, x < 2 ? 0x000000 : 0xFFFFFF);
                horizontalEdge.setRGB(x, y, y < 2 ? 0x000000 : 0xFFFFFF);
            }
        }

        GradientOrientationExtractor extractor = new GradientOrientationExtractor(9);
        ImageDescriptor d1 = extractor.extract(verticalEdge);
        ImageDescriptor d2 = extractor.extract(horizontalEdge);

        SimilarityService similarityService = new SimilarityService();
        double distance = similarityService.euclideanDistance(d1, d2);

        assertTrue(distance > 0.0);
    }

    @Test
    void shouldThrowExceptionForTooSmallImage() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        GradientOrientationExtractor extractor = new GradientOrientationExtractor(9);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extractor.extract(image)
        );

        assertTrue(exception.getMessage().contains("at least 3x3"));
    }
}