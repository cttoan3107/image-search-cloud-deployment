/*
  Global role of this class:

  - Central service for image processing
  - Extracts descriptors and computes similarity between images
*/
package pdl.backend.imageprocessing;

import java.awt.image.BufferedImage;

import org.springframework.stereotype.Service;

@Service
public class ImageProcessingService {

    private final RgbHistogramExtractor rgbExtractor;
    private final HsHistogramExtractor hsExtractor;
    private final GradientOrientationExtractor gradientExtractor;
    private final SimilarityService similarityService;

    public ImageProcessingService() {
        this.rgbExtractor = new RgbHistogramExtractor();
        this.hsExtractor = new HsHistogramExtractor();
        this.gradientExtractor = new GradientOrientationExtractor();
        this.similarityService = new SimilarityService();
    }

    public ImageProcessingService(RgbHistogramExtractor rgbExtractor,
                                  HsHistogramExtractor hsExtractor,
                                  GradientOrientationExtractor gradientExtractor,
                                  SimilarityService similarityService) {
        if (rgbExtractor == null || hsExtractor == null || gradientExtractor == null || similarityService == null) {
            throw new IllegalArgumentException("Extractors and similarity service must not be null.");
        }
        this.rgbExtractor = rgbExtractor;
        this.hsExtractor = hsExtractor;
        this.gradientExtractor = gradientExtractor;
        this.similarityService = similarityService;
    }

    public ImageDescriptor extract(BufferedImage image, DescriptorType type) {
        if (image == null) {
            throw new IllegalArgumentException("Image must not be null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Descriptor type must not be null.");
        }

        return switch (type) {
            case RGB -> rgbExtractor.extract(image);
            case HS -> hsExtractor.extract(image);
            case GRADIENT -> gradientExtractor.extract(image);
        };
    }

    public double distance(BufferedImage image1, BufferedImage image2, DescriptorType type) {
        if (image1 == null || image2 == null) {
            throw new IllegalArgumentException("Images must not be null.");
        }

        ImageDescriptor d1 = extract(image1, type);
        ImageDescriptor d2 = extract(image2, type);

        return similarityService.euclideanDistance(d1, d2);
    }

    public SimilarityService getSimilarityService() {
        return similarityService;
    }
}