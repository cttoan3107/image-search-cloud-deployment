/*
  Global role of this class:

  - Represents a result of the similarity search
  - Contains image ID, name, and similarity distance
*/
package pdl.backend;

public class SimilarImageResult {
    private final long id;
    private final String name;
    private final double distance;

    public SimilarImageResult(long id, String name, double distance) {
        this.id = id;
        this.name = name;
        this.distance = distance;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getDistance() {
        return distance;
    }
}