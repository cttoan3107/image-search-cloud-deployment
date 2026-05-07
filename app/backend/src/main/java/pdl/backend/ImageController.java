/*
  Global role of this class:

  - Main backend controller handling all image-related API endpoints
  - Acts as a bridge between frontend (Vue) and backend services (DAO + processing)
  - Centralizes all operations: upload, download, search, metadata, keywords, similarity, reactions
*/

package pdl.backend;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import pdl.backend.imageprocessing.DescriptorType;
import pdl.backend.imageprocessing.ImageProcessingService;

/**
 * REST controller exposing HTTP endpoints for image management.
 *
 * This class acts as the API layer between the frontend and the backend:
 * - upload an image
 * - download an image
 * - delete an image
 * - list all images
 * - search images
 * - manage keywords
 * - retrieve metadata
 * - find similar images
 *
 * The controller delegates persistence to ImageDao
 * and image comparison logic to ImageProcessingService.
 */
@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class ImageController {

  @Autowired
  private ObjectMapper mapper;

  /** DAO used to access and modify stored images. */
  private final ImageDao imageDao;

  /** Service used for image descriptor extraction and similarity computation. */
  private final ImageProcessingService imageProcessingService;

  public ImageController(ImageDao imageDao, ImageProcessingService imageProcessingService) {
    this.imageDao = imageDao;
    this.imageProcessingService = imageProcessingService;
  }

  /**
   * Returns the raw binary content of an image by id.
   *
   * The response content type is inferred from the file extension
   * so the browser/client can display the image correctly.
   *
   * Example:
   * GET /images/12
   */
  @RequestMapping(value = "/images/{id:\\d+}", method = RequestMethod.GET)
  public ResponseEntity<?> getImage(@PathVariable long id) {
    Optional<Image> imageOpt = imageDao.retrieve(id);

    if (imageOpt.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    Image img = imageOpt.get();

    MediaType mediaType;
    String name = img.getName().toLowerCase();

    // Determine MIME type from filename extension.
    if (name.endsWith(".png")) {
      mediaType = MediaType.IMAGE_PNG;
    } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
      mediaType = MediaType.IMAGE_JPEG;
    } else {
      mediaType = MediaType.APPLICATION_OCTET_STREAM;
    }

    return ResponseEntity.ok()
        .contentType(mediaType)
        .body(img.getData());
  }

  /**
   * Deletes an image by id.
   *
   * If the image exists:
   * - metadata is removed
   * - cached object is removed
   * - file is deleted from disk
   *
   * Example:
   * DELETE /images/12
   */
  @RequestMapping(value = "/images/{id:\\d+}", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteImage(@PathVariable long id) {
    Optional<Image> imageOpt = imageDao.retrieve(id);

    if (imageOpt.isPresent()) {
      imageDao.delete(imageOpt.get());
      return ResponseEntity.noContent().build();
    }

    return ResponseEntity.notFound().build();
  }

  /**
   * Uploads a new image.
   *
   * Only JPEG and PNG files are accepted.
   * The uploaded file is stored, indexed, and added to the image collection.
   *
   * Example:
   * POST /images
   * with multipart/form-data containing "file"
   */
  @RequestMapping(value = "/images", method = RequestMethod.POST)
  public ResponseEntity<?> addImage(@RequestParam MultipartFile file) {
    String ct = file.getContentType();
    boolean ok = MediaType.IMAGE_JPEG_VALUE.equals(ct) || MediaType.IMAGE_PNG_VALUE.equals(ct);

    // Reject unsupported file types.
    if (!ok) {
      return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
    }

    try {
      // Generate a simple unique id based on current time.
      // This is enough for a small local project, but not ideal in a large app.
      long newId = System.currentTimeMillis();
      Image img = new Image(newId, file.getOriginalFilename(), file.getBytes());

      imageDao.create(img);
      return ResponseEntity.status(HttpStatus.CREATED).build();
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns the list of all stored images as JSON.
   *
   * Each image contains:
   * - id
   * - name
   *
   * Example:
   * GET /images
   */
  @RequestMapping(value = "/images", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
  @ResponseBody
  public ArrayNode getImageList() {
    ArrayNode nodes = mapper.createArrayNode();
    List<Image> images = imageDao.retrieveAll();

    for (Image img : images) {
      var node = mapper.createObjectNode();
      node.put("name", img.getName());
      node.put("id", img.getId());
      nodes.add(node);
    }

    return nodes;
  }

  /**
   * Finds the most similar images to a given image.
   *
   * Parameters:
   * - id: id of the reference image
   * - number: maximum number of similar images to return
   * - descriptor: descriptor type used for comparison
   *
   * Workflow:
   * 1. Retrieve the base image
   * 2. Decode it into BufferedImage
   * 3. Compare it with every other image using the selected descriptor
   * 4. Sort by increasing distance (smaller distance = more similar)
   * 5. Return the best matches as JSON
   *
   * Example:
   * GET /images/5/similar?number=3&descriptor=color
   */
  @RequestMapping(value = "/images/{id:\\d+}/similar", method = RequestMethod.GET, produces = "application/json")
  @ResponseBody
  public ResponseEntity<?> findSimilarImages(
      @PathVariable long id,
      @RequestParam int number,
      @RequestParam String descriptor) {

    Optional<Image> baseImageOpt = imageDao.retrieve(id);
    if (baseImageOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    DescriptorType descriptorType;
    try {
      // Convert the descriptor name received in the request
      // into the corresponding enum constant.
      descriptorType = DescriptorType.valueOf(descriptor.toUpperCase());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("Unknown descriptor: " + descriptor);
    }

    BufferedImage baseBufferedImage;
    try {
      baseBufferedImage = ImageIO.read(new ByteArrayInputStream(baseImageOpt.get().getData()));
      if (baseBufferedImage == null) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Unable to decode base image.");
      }
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error while reading base image.");
    }

    List<SimilarImageResult> results = imageDao.retrieveAll().stream()
        // Do not compare the image to itself.
        .filter(img -> img.getId() != id)
        .map(img -> {
          try {
            BufferedImage otherBufferedImage = ImageIO.read(new ByteArrayInputStream(img.getData()));
            if (otherBufferedImage == null) {
              return null;
            }

            // Compute distance between the base image and the current candidate.
            double distance = imageProcessingService.distance(
                baseBufferedImage,
                otherBufferedImage,
                descriptorType);

            return new SimilarImageResult(img.getId(), img.getName(), distance);
          } catch (IOException | IllegalArgumentException e) {
            // If one image cannot be decoded or processed, ignore it.
            return null;
          }
        })
        .filter(result -> result != null)
        .sorted(Comparator.comparingDouble(SimilarImageResult::getDistance))
        .limit(number)
        .toList();

    ArrayNode nodes = mapper.createArrayNode();
    for (SimilarImageResult result : results) {
      var node = mapper.createObjectNode();
      node.put("id", result.getId());
      node.put("name", result.getName());
      node.put("score", result.getDistance());
      nodes.add(node);
    }

    return ResponseEntity.ok(nodes);
  }

  /**
   * Adds a keyword/tag to an image.
   *
   * Example:
   * PUT /images/8/keywords?tag=animal
   */
  @RequestMapping(value = "/images/{id:\\d+}/keywords", method = RequestMethod.PUT)
  public ResponseEntity<?> addKeyword(
      @PathVariable long id,
      @RequestParam String tag) {

    Optional<Image> imageOpt = imageDao.retrieve(id);

    if (imageOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    imageDao.addKeyword(id, tag);
    return ResponseEntity.noContent().build();
  }

  /**
   * Returns detailed metadata for one image.
   *
   * The JSON response contains:
   * - Name
   * - Type
   * - Size (width*height)
   * - Keywords
   *
   * This endpoint is used by ImageDetail.vue.
   */
  @RequestMapping(value = "/images/{id:\\d+}/metadata", method = RequestMethod.GET)
  public ResponseEntity<?> getMetadata(@PathVariable long id) {

    Optional<Image> imageOpt = imageDao.retrieve(id);

    if (imageOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Image img = imageOpt.get();

    var node = mapper.createObjectNode();

    node.put("Name", img.getName());
    String fileName = img.getName().toLowerCase();

    // Infer the image type from the file extension.
    if (fileName.endsWith(".png")) {
      node.put("Type", "png");
    } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
      node.put("Type", "jpeg");
    } else {
      node.put("Type", "unknown");
    }

    try {
      // Read the actual file to extract width and height.
      File file = new File("images/" + img.getName());
      BufferedImage image = ImageIO.read(file);

      int width = image.getWidth();
      int height = image.getHeight();

      node.put("Size", width + "*" + height);

    } catch (Exception e) {
      // If metadata extraction fails, keep the endpoint usable.
      node.put("Size", "unknown");
    }

    var keywordsArray = node.putArray("Keywords");

    List<String> keywords = imageDao.getKeywords(id);

    for (String k : keywords) {
      keywordsArray.add(k);
    }

    return ResponseEntity.ok(node);
  }

  /**
   * Deletes a keyword/tag from an image.
   *
   * Example:
   * DELETE /images/8/keywords?tag=animal
   */
  @RequestMapping(value = "/images/{id:\\d+}/keywords", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteKeyword(
      @PathVariable long id,
      @RequestParam String tag) {

    Optional<Image> imageOpt = imageDao.retrieve(id);

    if (imageOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    boolean deleted = imageDao.deleteKeyword(id, tag);

    if (!deleted) {
      return ResponseEntity.badRequest().body("Tag not associated with this image: " + tag);
    }

    return ResponseEntity.noContent().build();
  }

  /**
   * Searches images by keyword or filename.
   *
   * The actual search logic is handled by ImageDao.
   *
   * Example:
   * GET /search/images?tag=cat
   */
  @RequestMapping(value = "/images/search", method = RequestMethod.GET, produces = "application/json")
  @ResponseBody
  public ResponseEntity<?> searchImages(
      @RequestParam(required = false) String reaction,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String format,
      @RequestParam(required = false) Integer width,
      @RequestParam(required = false) Integer height,
      @RequestParam(required = false, name = "keywords") List<String> keywords,
      @RequestBody(required = false) ImageSearchRequest body) {
    ImageSearchRequest request = new ImageSearchRequest();

    boolean hasQueryParams = name != null ||
        format != null ||
        width != null ||
        height != null ||
        (keywords != null && !keywords.isEmpty()) ||
        reaction != null;

    if (hasQueryParams) {
      request.setName(name);
      request.setFormat(format);
      request.setWidth(width);
      request.setHeight(height);
      request.setKeywords(keywords);
      request.setReaction(reaction);
    } else if (body != null) {
      request.setName(body.getName());
      request.setFormat(body.getFormat());
      request.setWidth(body.getWidth());
      request.setHeight(body.getHeight());
      request.setKeywords(body.getKeywords());
      request.setReaction(body.getReaction());
    }

    List<Long> ids = imageDao.searchImageIds(request);

    ArrayNode nodes = mapper.createArrayNode();
    for (Long id : ids) {
      nodes.add(id);
    }

    return ResponseEntity.ok(nodes);
  }

  @RequestMapping(value = "/images/keywords", method = RequestMethod.GET, produces = "application/json")
  @ResponseBody
  public ArrayNode getAllKeywords() {
    ArrayNode nodes = mapper.createArrayNode();

    List<String> keywords = imageDao.getAllKeywords();

    for (String keyword : keywords) {
      nodes.add(keyword);
    }

    return nodes;
  }
  
  @RequestMapping(value = "/images/{id:\\d+}/like", method = RequestMethod.POST)
  public ResponseEntity<?> likeImage(@PathVariable long id) {
    Optional<Image> imageOpt = imageDao.retrieve(id);

    if (imageOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    imageDao.setReaction(id, "LIKE");
    return ResponseEntity.noContent().build();
  }
  
  @RequestMapping(value = "/images/{id:\\d+}/dislike", method = RequestMethod.POST)
  public ResponseEntity<?> dislikeImage(@PathVariable long id) {
    Optional<Image> imageOpt = imageDao.retrieve(id);

    if (imageOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    imageDao.setReaction(id, "DISLIKE");
    return ResponseEntity.noContent().build();
  }
  
  @RequestMapping(value = "/images/{id:\\d+}/reaction", method = RequestMethod.GET, produces = "application/json")
  @ResponseBody
  public ResponseEntity<?> getReaction(@PathVariable long id) {
    Optional<Image> imageOpt = imageDao.retrieve(id);

    if (imageOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    String reaction = imageDao.getReaction(id);

    var node = mapper.createObjectNode();
    node.put("imageId", id);
    if (reaction == null) {
      node.putNull("reaction");
    } else {
      node.put("reaction", reaction);
    }

    return ResponseEntity.ok(node);
  }
  @RequestMapping(value = "/images/{id:\\d+}/name", method = RequestMethod.PUT)
public ResponseEntity<?> renameImage(
    @PathVariable long id,
    @RequestParam String newName) {

  Optional<Image> imageOpt = imageDao.retrieve(id);

  if (imageOpt.isEmpty()) {
    return ResponseEntity.notFound().build();
  }

  boolean renamed = imageDao.renameImage(id, newName);

  if (!renamed) {
    return ResponseEntity.badRequest().body("Unable to rename image.");
  }

  return ResponseEntity.noContent().build();
}
}