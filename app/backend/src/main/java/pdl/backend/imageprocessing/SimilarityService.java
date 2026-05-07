/*
  Global role of this class:

  - Computes similarity between images
  - Uses Euclidean distance on descriptor vectors
*/
package pdl.backend.imageprocessing;

public class SimilarityService {

    public double euclideanDistance(ImageDescriptor d1, ImageDescriptor d2) {
        if (d1 == null || d2 == null) {
            throw new IllegalArgumentException("Descriptors must not be null.");
        }

        if (d1.getType() != d2.getType()) {
            throw new IllegalArgumentException("Descriptors must have the same type.");
        }

        return euclideanDistance(d1.getValues(), d2.getValues());
    }

    public double euclideanDistance(double[] a, double[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Descriptor vectors must not be null.");
        }

        if (a.length != b.length) {
            throw new IllegalArgumentException("Descriptor vectors must have the same length.");
        }

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }
}
