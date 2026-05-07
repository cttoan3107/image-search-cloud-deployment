/*
  Global role of this class:

  - Handles all data access for images (database + file system)
  - Stores image metadata in DB and image files on disk
  - Manages keywords, reactions, and search filtering
  - Computes and stores image descriptors for similarity search
*/

package pdl.backend;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import pdl.backend.imageprocessing.DescriptorType;
import pdl.backend.imageprocessing.ImageDescriptor;
import pdl.backend.imageprocessing.ImageProcessingService;

/**
 * DAO responsible for image persistence.
 *
 * This class stores image metadata in the database, stores image files on disk
 * inside the ./images folder, and keeps an in-memory cache of loaded images.
 *
 * It also manages:
 * - image keywords/tags
 * - computed image descriptors used for image processing / search
 *
 * Spring creates this repository as a bean, and after dependency injection
 * the method afterPropertiesSet() is automatically called to initialize
 * storage.
 */
@Repository
public class ImageDao implements Dao<Image>, InitializingBean {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ImageProcessingService imageProcessingService;

    /** Directory where image files are physically stored. */
    private static final Path IMAGE_DIR = Paths.get("images");

    /** Allowed image file extensions when scanning the folder at startup. */
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png");

    /**
     * In-memory cache of images indexed by id.
     * This avoids reading image files from disk for every access.
     */
    private final Map<Long, Image> images = new HashMap<>();

    private boolean matchesName(Image img, String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        return img.getName().toLowerCase().contains(name.toLowerCase());
    }

    private boolean matchesFormat(Image img, String format) {
        if (format == null || format.isBlank()) {
            return true;
        }

        String lowerName = img.getName().toLowerCase();
        String lowerFormat = format.toLowerCase();

        if (lowerFormat.equals("jpg") || lowerFormat.equals("jpeg")) {
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg");
        }

        if (lowerFormat.equals("png")) {
            return lowerName.endsWith(".png");
        }

        return false;
    }

    private boolean matchesWidth(Image img, Integer width) {
        if (width == null) {
            return true;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(img.getData()));
            return image != null && image.getWidth() == width;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean matchesHeight(Image img, Integer height) {
        if (height == null) {
            return true;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(img.getData()));
            return image != null && image.getHeight() == height;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean matchesTag(Image img, String tag) {
        if (tag == null || tag.isBlank()) {
            return true;
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM image_keywords WHERE image_id = ? AND tag = ?",
                Integer.class,
                img.getId(),
                tag);

        return count != null && count > 0;
    }

    public List<Long> searchImageIds(ImageSearchRequest request) {
        return images.values().stream()
                .filter(img -> matchesName(img, request.getName()))
                .filter(img -> matchesFormat(img, request.getFormat()))
                .filter(img -> matchesWidth(img, request.getWidth()))
                .filter(img -> matchesHeight(img, request.getHeight()))
                .filter(img -> matchesKeywords(img, request.getKeywords()))
                .filter(img -> matchesReaction(img, request.getReaction()))
                .map(Image::getId)
                .toList();
    }
    
    private boolean matchesReaction(Image img, String reaction) {
        if (reaction == null || reaction.isBlank()) {
            return true;
        }

        String dbReaction = getReaction(img.getId());

        if (dbReaction == null) {
            return false;
        }

        return dbReaction.equalsIgnoreCase(reaction);
    }

    private boolean matchesKeywords(Image img, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        List<String> imageKeywords = getKeywords(img.getId()).stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();

        return keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(imageKeywords::contains);
    }

    /**
     * Called automatically by Spring once all properties have been injected.
     *
     * Initialization steps:
     * 1. Create database tables if they do not exist.
     * 2. Check that the ./images directory exists.
     * 3. Load images already referenced in the database into memory.
     * 4. Compute missing descriptors for loaded images.
     * 5. Scan the ./images folder for image files not yet in the DB and import
     * them.
     */
    @Override
    public void afterPropertiesSet() {
        // Main table containing image metadata.
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS images (" +
                        "id BIGINT PRIMARY KEY, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "media_type VARCHAR(255) NOT NULL" +
                        ")");

        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS image_reactions (" +
                        "image_id BIGINT PRIMARY KEY, " +
                        "reaction VARCHAR(20), " +
                        "FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE" +
                        ")");                

        // Table linking images to text keywords/tags.
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS image_keywords (" +
                        "image_id BIGINT NOT NULL, " +
                        "tag VARCHAR(255) NOT NULL, " +
                        "PRIMARY KEY (image_id, tag), " +
                        "FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE" +
                        ")");

        // Table storing computed descriptors for each image.
        // One descriptor per type, per image.
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS image_descriptors (" +
                        "image_id BIGINT NOT NULL, " +
                        "descriptor_type VARCHAR(50) NOT NULL, " +
                        "descriptor TEXT NOT NULL, " +
                        "PRIMARY KEY (image_id, descriptor_type), " +
                        "FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE" +
                        ")");

        // The application expects a local ./images directory.
        if (!Files.exists(IMAGE_DIR) || !Files.isDirectory(IMAGE_DIR)) {
            throw new IllegalStateException(
                    "Besoin 1: dossier './images' introuvable. " +
                            "Crée un dossier 'images' dans le répertoire où tu lances le serveur.");
        }

        // Reset in-memory cache before rebuilding it from storage.
        images.clear();

        // Load images that are already known in the database.
        jdbcTemplate.query(
                "SELECT id, name, media_type FROM images",
                (rs, rowNum) -> {
                    Long id = rs.getLong("id");
                    String name = rs.getString("name");
                    Path path = IMAGE_DIR.resolve(name);

                    // If the DB references a file that no longer exists, skip it.
                    if (!Files.exists(path)) {
                        System.err.println("Warning: image file missing, skipped: " + name);
                        return null;
                    }

                    try {
                        byte[] data = Files.readAllBytes(path);
                        Image img = new Image(id, name, data);
                        images.put(img.getId(), img);
                        return img;
                    } catch (IOException e) {
                        // If the file cannot be read, skip it but keep startup alive.
                        System.err.println("Warning: unable to read image, skipped: " + name);
                        return null;
                    }
                });

        // Ensure every loaded image has all its descriptors indexed in the DB.
        for (Image img : images.values()) {
            ensureReactionRowExists(img.getId());
            if (!hasAllDescriptors(img.getId())) {
                indexImageDescriptors(img);
            }
        }

        // Collect names of images already known in the database/cache.
        Set<String> knownNames = new HashSet<>();
        for (Image img : images.values()) {
            knownNames.add(img.getName());
        }

        // Scan the ./images folder and import new image files not yet registered.
        try (Stream<Path> files = Files.list(IMAGE_DIR)) {
            files
                    .filter(p -> !Files.isDirectory(p))
                    .filter(this::isAllowedImageFile)
                    .forEach(p -> {
                        String filename = p.getFileName().toString();

                        // Skip files already known.
                        if (knownNames.contains(filename)) {
                            return;
                        }

                        try {
                            byte[] data = Files.readAllBytes(p);
                            long nextId = nextId();
                            Image img = new Image(nextId, filename, data);
                            create(img);
                            knownNames.add(filename);
                        } catch (IOException e) {
                            throw new RuntimeException("Erreur lecture fichier image: " + filename, e);
                        }
                    });
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la lecture du dossier ./images", e);
        }
    }

    /**
     * Adds a keyword/tag to an image.
     *
     * If the keyword already exists for this image, nothing happens
     * because of ON CONFLICT DO NOTHING.
     */
    public void addKeyword(long imageId, String tag) {
        if (!images.containsKey(imageId)) {
            throw new NoSuchElementException("Image not found: " + imageId);
        }

        jdbcTemplate.update(
                "INSERT INTO image_keywords (image_id, tag) VALUES (?, ?) ON CONFLICT DO NOTHING",
                imageId,
                tag);
    }

    /**
     * Returns all keywords associated with a given image.
     */
    public List<String> getKeywords(long imageId) {
        return jdbcTemplate.queryForList(
                "SELECT tag FROM image_keywords WHERE image_id = ?",
                String.class,
                imageId);
    }

    /**
     * Removes a specific keyword from an image.
     */
    public boolean deleteKeyword(long imageId, String tag) {
        int rows = jdbcTemplate.update(
                "DELETE FROM image_keywords WHERE image_id = ? AND tag = ?",
                imageId,
                tag);

        return rows > 0;
    }

    /**
     * Searches images by:
     * - exact keyword match
     * - partial filename match
     *
     * Returns the corresponding Image objects from the in-memory cache.
     */
    public List<Image> searchImages(String tag, String format, Integer width, Integer height, String name) {
        return images.values().stream()
                .filter(img -> matchesName(img, name))
                .filter(img -> matchesFormat(img, format))
                .filter(img -> matchesWidth(img, width))
                .filter(img -> matchesHeight(img, height))
                .filter(img -> matchesTag(img, tag))
                .toList();
    }

    /**
     * Checks whether a file has an allowed image extension.
     */
    private boolean isAllowedImageFile(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        String ext = name.substring(dot + 1).toLowerCase();
        return ALLOWED_EXT.contains(ext);
    }

    /**
     * Computes the next available image id based on the current in-memory cache.
     *
     * Note: this works because ids are managed locally in memory here.
     */
    private long nextId() {
        return images.keySet().stream().mapToLong(Long::longValue).max().orElse(-1L) + 1;
    }

    /**
     * Converts a descriptor vector into a comma-separated string for DB storage.
     */
    private String serializeDescriptor(double[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(values[i]);
        }
        return sb.toString();
    }

    /**
     * Checks whether the image already has one stored descriptor
     * for every available DescriptorType.
     */
    private boolean hasAllDescriptors(long imageId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM image_descriptors WHERE image_id = ?",
                Integer.class,
                imageId);
        return count != null && count == DescriptorType.values().length;
    }

    /**
     * Computes and stores all descriptors for an image.
     *
     * Steps:
     * 1. Decode raw bytes into a BufferedImage
     * 2. Extract descriptors for every DescriptorType
     * 3. Save them into the database
     *
     * Existing descriptors are updated if already present.
     */
    private void indexImageDescriptors(Image img) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(img.getData()));
            if (bufferedImage == null) {
                System.err.println("Warning: unable to decode image for indexing: " + img.getName());
                return;
            }

            for (DescriptorType type : DescriptorType.values()) {
                ImageDescriptor descriptor = imageProcessingService.extract(bufferedImage, type);
                double[] values = descriptor.getValues();

                jdbcTemplate.update(
                        "INSERT INTO image_descriptors (image_id, descriptor_type, descriptor) " +
                                "VALUES (?, ?, ?) " +
                                "ON CONFLICT (image_id, descriptor_type) DO UPDATE SET descriptor = EXCLUDED.descriptor",
                        img.getId(),
                        type.name(),
                        serializeDescriptor(values));
            }
        } catch (Exception e) {
            // Descriptor extraction failure should not crash the app.
            System.err
                    .println("Warning: failed to index image descriptors for " + img.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Returns one image by id from the in-memory cache.
     */
    @Override
    public Optional<Image> retrieve(long id) {
        return Optional.ofNullable(images.get(id));
    }

    /**
     * Returns all cached images.
     */
    @Override
    public List<Image> retrieveAll() {
        return List.copyOf(images.values());
    }

    /**
     * Creates a new image:
     * 1. save file bytes to disk
     * 2. detect media type
     * 3. insert metadata into DB
     * 4. add to in-memory cache
     * 5. compute descriptors
     *
     * If the image id already exists in DB, insertion is ignored.
     */
    @Override
    public void create(Image img) {
        Path out = IMAGE_DIR.resolve(img.getName());
        try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
            fos.write(img.getData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String mediaType;
        try {
            mediaType = Files.probeContentType(out);
            if (mediaType == null) {
                mediaType = "application/octet-stream";
            }
        } catch (IOException e) {
            mediaType = "application/octet-stream";
        }

        int rows = jdbcTemplate.update(
                "INSERT INTO images (id, name, media_type) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING",
                img.getId(),
                img.getName(),
                mediaType);

        // Only update cache and descriptors if the DB insert really happened.
        if (rows > 0) {
            images.put(img.getId(), img);
            ensureReactionRowExists(img.getId());
            indexImageDescriptors(img);
        }
    }

    /**
     * Update is not used in this implementation.
     */
    @Override
    public void update(Image img, String[] params) {
        // Non utilisé
    }

    /**
     * Deletes an image:
     * 1. remove DB metadata (keywords/descriptors are removed automatically by
     * cascade)
     * 2. remove it from the in-memory cache
     * 3. delete the physical file from disk
     */
    @Override
    public void delete(Image img) {
        jdbcTemplate.update("DELETE FROM images WHERE id = ?", img.getId());
        images.remove(img.getId());

        Path file = IMAGE_DIR.resolve(img.getName());
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getAllKeywords() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT tag FROM image_keywords ORDER BY tag",
                String.class);
    }
    
    private void ensureReactionRowExists(long imageId) {
        jdbcTemplate.update(
                "INSERT INTO image_reactions (image_id, reaction) VALUES (?, NULL) ON CONFLICT (image_id) DO NOTHING",
                imageId);
    }
    
    public boolean setReaction(long imageId, String reaction) {
        if (!images.containsKey(imageId)) {
            return false;
        }

        ensureReactionRowExists(imageId);

        jdbcTemplate.update(
                "UPDATE image_reactions SET reaction = ? WHERE image_id = ?",
                reaction,
                imageId);

        return true;
    }

    public String getReaction(long imageId) {
        if (!images.containsKey(imageId)) {
            throw new NoSuchElementException("Image not found: " + imageId);
        }

        ensureReactionRowExists(imageId);

        return jdbcTemplate.queryForObject(
                "SELECT reaction FROM image_reactions WHERE image_id = ?",
                String.class,
                imageId);
    }
    public boolean renameImage(long imageId, String newName) {
    if (!images.containsKey(imageId)) {
        return false;
    }

    if (newName == null || newName.isBlank()) {
        return false;
    }

    String trimmedName = newName.trim();
    Image img = images.get(imageId);
    String oldName = img.getName();

    if (oldName.equals(trimmedName)) {
        return true;
    }

    if (!isAllowedImageFile(Paths.get(trimmedName))) {
        return false;
    }

    Path oldPath = IMAGE_DIR.resolve(oldName);
    Path newPath = IMAGE_DIR.resolve(trimmedName);

    if (Files.exists(newPath)) {
        return false;
    }

    try {
        Files.move(oldPath, newPath);

        jdbcTemplate.update(
                "UPDATE images SET name = ? WHERE id = ?",
                trimmedName,
                imageId);

        img.setName(trimmedName);
        return true;

    } catch (IOException e) {
        return false;
    }
}
    

}