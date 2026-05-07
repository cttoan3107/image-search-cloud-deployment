/*
  Global role of this class:

  - Represents a numerical descriptor of an image
  - Stores the feature vector used for similarity comparison
*/
package pdl.backend.imageprocessing;

import java.util.Arrays;

public class ImageDescriptor {
    private final DescriptorType type;
    private final double[] values;

    public ImageDescriptor(DescriptorType type, double[] values) {
        if (type == null) {
            throw new IllegalArgumentException("Descriptor type must not be null.");
        }
        if (values == null) {
            throw new IllegalArgumentException("Descriptor values must not be null.");
        }
        this.type = type;
        this.values = values;
    }

    public DescriptorType getType() {
        return type;
    }

    public double[] getValues() {
        return values;
    }

    public int size() {
        return values.length;
    }

    @Override
    public String toString() {
        return "ImageDescriptor{" +
                "type=" + type +
                ", size=" + values.length +
                ", values=" + Arrays.toString(values) +
                '}';
    }
}
