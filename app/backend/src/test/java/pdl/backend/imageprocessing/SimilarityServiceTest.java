/*
  Global role of this class:
  - Tests Euclidean distance computation
  - Validates similarity logic and error handling
*/
package pdl.backend.imageprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class SimilarityServiceTest {

    @Test
    void sameVectorsShouldHaveZeroDistance() {
        SimilarityService service = new SimilarityService();

        double[] a = {0.1, 0.2, 0.3};
        double[] b = {0.1, 0.2, 0.3};

        double distance = service.euclideanDistance(a, b);

        assertEquals(0.0, distance, 1e-9);
    }

    @Test
    void differentVectorsShouldHavePositiveDistance() {
        SimilarityService service = new SimilarityService();

        double[] a = {0.0, 0.0, 0.0};
        double[] b = {1.0, 1.0, 1.0};

        double distance = service.euclideanDistance(a, b);

        assertTrue(distance > 0.0);
    }

    @Test
    void shouldThrowExceptionWhenVectorsHaveDifferentLengths() {
        SimilarityService service = new SimilarityService();

        double[] a = {0.1, 0.2};
        double[] b = {0.1, 0.2, 0.3};

        assertThrows(IllegalArgumentException.class, () -> service.euclideanDistance(a, b));
    }

    @Test
    void shouldThrowExceptionWhenDescriptorTypesDiffer() {
        SimilarityService service = new SimilarityService();

        ImageDescriptor d1 = new ImageDescriptor(DescriptorType.RGB, new double[]{0.1, 0.2});
        ImageDescriptor d2 = new ImageDescriptor(DescriptorType.HS, new double[]{0.1, 0.2});

        assertThrows(IllegalArgumentException.class, () -> service.euclideanDistance(d1, d2));
    }
}