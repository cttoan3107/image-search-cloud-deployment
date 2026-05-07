/*
  Global role of this class:
  - Tests HTTP endpoints of ImageController
  - Verifies API behavior (status, responses) with mocked dependencies
*/
package pdl.backend;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import pdl.backend.imageprocessing.DescriptorType;
import pdl.backend.imageprocessing.ImageProcessingService;

/**
 * Controller tests for ImageController.
 *
 * These tests focus on HTTP behavior only.
 * DAO and ImageProcessingService are mocked, so no real DB is used.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
})
public class ImageControllerTests {

        @MockitoBean
        private ImageDao imageDao;

        @MockitoBean
        private ImageProcessingService imageProcessingService;

        @Autowired
        private MockMvc mockMvc;

        @Test
        void getImageShouldReturnJpegContent() throws Exception {
                Image image = new Image(1L, "test.jpg", "abc".getBytes());

                when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));

                mockMvc.perform(get("/images/1"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                                .andExpect(content().bytes("abc".getBytes()));

                verify(imageDao).retrieve(1L);
        }

        @Test
        void getImageShouldReturnPngContent() throws Exception {
                Image image = new Image(2L, "test.png", "pngdata".getBytes());

                when(imageDao.retrieve(2L)).thenReturn(Optional.of(image));

                mockMvc.perform(get("/images/2"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.IMAGE_PNG));

                verify(imageDao).retrieve(2L);
        }

        @Test
        void getImageShouldReturnNotFound() throws Exception {
                when(imageDao.retrieve(999L)).thenReturn(Optional.empty());

                mockMvc.perform(get("/images/999"))
                                .andExpect(status().isNotFound());

                verify(imageDao).retrieve(999L);
        }

        @Test
        void addImageShouldReturnCreatedForJpeg() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "dummy image content".getBytes());

                mockMvc.perform(multipart("/images").file(file))
                                .andExpect(status().isCreated());

                verify(imageDao).create(any(Image.class));
        }

        @Test
        void addImageShouldReturnCreatedForPng() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.png",
                                MediaType.IMAGE_PNG_VALUE,
                                "dummy png content".getBytes());

                mockMvc.perform(multipart("/images").file(file))
                                .andExpect(status().isCreated());

                verify(imageDao).create(any(Image.class));
        }

        @Test
        void addImageShouldReturnUnsupportedMediaType() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.gif",
                                MediaType.IMAGE_GIF_VALUE,
                                "dummy gif content".getBytes());

                mockMvc.perform(multipart("/images").file(file))
                                .andExpect(status().isUnsupportedMediaType());

                verify(imageDao, never()).create(any(Image.class));
        }

        @Test
        void deleteImagesWithoutIdShouldReturnMethodNotAllowed() throws Exception {
                mockMvc.perform(delete("/images"))
                                .andExpect(status().isMethodNotAllowed());
        }

        @Test
        void deleteImageShouldReturnNotFound() throws Exception {
                when(imageDao.retrieve(999L)).thenReturn(Optional.empty());

                mockMvc.perform(delete("/images/999"))
                                .andExpect(status().isNotFound());

                verify(imageDao).retrieve(999L);
        }

        @Test
        void deleteImageShouldReturnNoContent() throws Exception {
                Image image = new Image(1L, "test.jpg", "abc".getBytes());

                when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));

                mockMvc.perform(delete("/images/1"))
                                .andExpect(status().isNoContent());

                verify(imageDao).retrieve(1L);
                verify(imageDao).delete(image);
        }

        @Test
        void getImageListShouldReturnSuccess() throws Exception {
                List<Image> images = List.of(
                                new Image(1L, "a.jpg", "a".getBytes()),
                                new Image(2L, "b.png", "b".getBytes()));

                when(imageDao.retrieveAll()).thenReturn(images);

                mockMvc.perform(get("/images"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].name").value("a.jpg"))
                                .andExpect(jsonPath("$[1].id").value(2))
                                .andExpect(jsonPath("$[1].name").value("b.png"));

                verify(imageDao).retrieveAll();
        }

        @Test
        void findSimilarImagesShouldReturnNotFoundWhenBaseImageDoesNotExist() throws Exception {
                when(imageDao.retrieve(1L)).thenReturn(Optional.empty());

                mockMvc.perform(get("/images/1/similar")
                                .param("number", "3")
                                .param("descriptor", "RGB"))
                                .andExpect(status().isNotFound());

                verify(imageDao).retrieve(1L);
        }

        @Test
        void findSimilarImagesShouldReturnBadRequestForUnknownDescriptor() throws Exception {
                Image base = new Image(1L, "base.jpg", "abc".getBytes());
                when(imageDao.retrieve(1L)).thenReturn(Optional.of(base));

                mockMvc.perform(get("/images/1/similar")
                                .param("number", "3")
                                .param("descriptor", "UNKNOWN"))
                                .andExpect(status().isBadRequest());

                verify(imageDao).retrieve(1L);
        }

        @Test
        void findSimilarImagesShouldReturnSortedResults() throws Exception {
                byte[] tinyPng = new byte[] {
                                (byte) 137, 80, 78, 71, 13, 10, 26, 10,
                                0, 0, 0, 13, 73, 72, 68, 82,
                                0, 0, 0, 1, 0, 0, 0, 1,
                                8, 2, 0, 0, 0, (byte) 144, 119, 83,
                                (byte) 222, 0, 0, 0, 12, 73, 68, 65, 84,
                                8, (byte) 215, 99, (byte) 248, (byte) 207, (byte) 192, 0, 0, 3, 1, 1, 0,
                                (byte) 201, (byte) 254, (byte) 146, (byte) 239, 0, 0, 0, 0,
                                73, 69, 78, 68, (byte) 174, 66, 96, (byte) 130
                };

                Image base = new Image(1L, "base.png", tinyPng);
                Image img2 = new Image(2L, "img2.png", tinyPng);
                Image img3 = new Image(3L, "img3.png", tinyPng);

                when(imageDao.retrieve(1L)).thenReturn(Optional.of(base));
                when(imageDao.retrieveAll()).thenReturn(List.of(base, img2, img3));
                when(imageProcessingService.distance(any(), any(), eq(DescriptorType.RGB)))
                                .thenReturn(0.4, 0.2);

                mockMvc.perform(get("/images/1/similar")
                                .param("number", "2")
                                .param("descriptor", "RGB"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(3))
                                .andExpect(jsonPath("$[0].name").value("img3.png"))
                                .andExpect(jsonPath("$[0].score").value(0.2))
                                .andExpect(jsonPath("$[1].id").value(2))
                                .andExpect(jsonPath("$[1].name").value("img2.png"))
                                .andExpect(jsonPath("$[1].score").value(0.4));

                verify(imageDao).retrieve(1L);
                verify(imageDao).retrieveAll();
        }

        @Test
        void addKeywordShouldReturnNotFoundWhenImageDoesNotExist() throws Exception {
                when(imageDao.retrieve(1L)).thenReturn(Optional.empty());

                mockMvc.perform(put("/images/1/keywords").param("tag", "cat"))
                                .andExpect(status().isNotFound());

                verify(imageDao).retrieve(1L);
        }

        @Test
        void addKeywordShouldReturnNoContent() throws Exception {
                Image image = new Image(1L, "test.jpg", "abc".getBytes());
                when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));

                mockMvc.perform(put("/images/1/keywords").param("tag", "cat"))
                                .andExpect(status().isNoContent());

                verify(imageDao).retrieve(1L);
                verify(imageDao).addKeyword(1L, "cat");
        }

        @Test
        void getMetadataShouldReturnNotFoundWhenImageDoesNotExist() throws Exception {
                when(imageDao.retrieve(1L)).thenReturn(Optional.empty());

                mockMvc.perform(get("/images/1/metadata"))
                                .andExpect(status().isNotFound());

                verify(imageDao).retrieve(1L);
        }

        @Test
        void getMetadataShouldReturnMetadata() throws Exception {
                Image image = new Image(1L, "test.jpg", "abc".getBytes());

                when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));
                when(imageDao.getKeywords(1L)).thenReturn(List.of("cat", "animal"));

                mockMvc.perform(get("/images/1/metadata"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.Name").value("test.jpg"))
                                .andExpect(jsonPath("$.Type").value("jpeg"))
                                .andExpect(jsonPath("$.Keywords[0]").value("cat"))
                                .andExpect(jsonPath("$.Keywords[1]").value("animal"));

                verify(imageDao).retrieve(1L);
                verify(imageDao).getKeywords(1L);
        }

        @Test
        void deleteKeywordShouldReturnNotFoundWhenImageDoesNotExist() throws Exception {
                when(imageDao.retrieve(1L)).thenReturn(Optional.empty());

                mockMvc.perform(delete("/images/1/keywords").param("tag", "cat"))
                                .andExpect(status().isNotFound());

                verify(imageDao).retrieve(1L);
        }

        @Test
        void deleteKeywordShouldReturnBadRequestWhenTagIsNotAssociated() throws Exception {
                Image image = new Image(1L, "test.jpg", "abc".getBytes());

                when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));
                when(imageDao.deleteKeyword(1L, "cat")).thenReturn(false);

                mockMvc.perform(delete("/images/1/keywords").param("tag", "cat"))
                                .andExpect(status().isBadRequest());

                verify(imageDao).retrieve(1L);
                verify(imageDao).deleteKeyword(1L, "cat");
        }

        @Test
        void deleteKeywordShouldReturnNoContent() throws Exception {
                Image image = new Image(1L, "test.jpg", "abc".getBytes());

                when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));
                when(imageDao.deleteKeyword(1L, "cat")).thenReturn(true);

                mockMvc.perform(delete("/images/1/keywords").param("tag", "cat"))
                                .andExpect(status().isNoContent());

                verify(imageDao).retrieve(1L);
                verify(imageDao).deleteKeyword(1L, "cat");
        }

        @Test
        void searchImagesShouldReturnResults() throws Exception {
                when(imageDao.searchImageIds(any(ImageSearchRequest.class))).thenReturn(List.of(1L, 2L));

                mockMvc.perform(get("/images/search")
                                .param("name", "cat")
                                .param("format", "jpeg")
                                .param("width", "100")
                                .param("height", "200")
                                .param("keywords", "cat"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0]").value(1))
                                .andExpect(jsonPath("$[1]").value(2));

                verify(imageDao).searchImageIds(any(ImageSearchRequest.class));
        }

        @Test
        void getAllKeywordsShouldReturnResults() throws Exception {
                when(imageDao.getAllKeywords()).thenReturn(List.of("cat", "animal"));

                mockMvc.perform(get("/images/keywords"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0]").value("cat"))
                                .andExpect(jsonPath("$[1]").value("animal"));

                verify(imageDao).getAllKeywords();
        }

        @Test
        void getReactionShouldReturnNotFoundWhenImageDoesNotExist() throws Exception {
                when(imageDao.retrieve(1L)).thenReturn(Optional.empty());

                mockMvc.perform(get("/images/1/reaction"))
                                .andExpect(status().isNotFound());

                verify(imageDao).retrieve(1L);
        }
        
        @Test
        void getReactionShouldReturnLike() throws Exception {
                Image image = new Image(1L, "test.jpg", "abc".getBytes());

                when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));
                when(imageDao.getReaction(1L)).thenReturn("LIKE");

                mockMvc.perform(get("/images/1/reaction"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.imageId").value(1))
                                .andExpect(jsonPath("$.reaction").value("LIKE"));

                verify(imageDao).retrieve(1L);
                verify(imageDao).getReaction(1L);
        }
        
        @Test
        void getReactionShouldReturnDislike() throws Exception {
                Image image = new Image(1L, "test.jpg", "abc".getBytes());

                when(imageDao.retrieve(1L)).thenReturn(Optional.of(image));
                when(imageDao.getReaction(1L)).thenReturn("DISLIKE");

                mockMvc.perform(get("/images/1/reaction"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.imageId").value(1))
                                .andExpect(jsonPath("$.reaction").value("DISLIKE"));

                verify(imageDao).retrieve(1L);
                verify(imageDao).getReaction(1L);
        }
}