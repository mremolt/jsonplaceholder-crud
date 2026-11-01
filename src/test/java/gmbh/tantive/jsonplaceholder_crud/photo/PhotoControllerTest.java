package gmbh.tantive.jsonplaceholder_crud.photo;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.support.CrudControllerTestSupport;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PhotoControllerTest {

    @Mock
    private PhotoService photoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PhotoController(photoService)).build();
    }

    @Test
    void supportsPhotoCrudRoutes() throws Exception {
        CrudControllerTestSupport.assertCrudRoutes(mockMvc, "/photos", photoService, photo(1L, 5L),
                "{\"albumId\":5,\"title\":\"Photo\",\"url\":\"https://example.test/photo\",\"thumbnailUrl\":\"https://example.test/thumb\"}");
    }

    @Test
    void listsPhotosByAlbumId() throws Exception {
        when(photoService.findByAlbumId(5L, new CollectionPage(0, 100))).thenReturn(List.of(photo(1L, 5L)));

        mockMvc.perform(get("/photos?albumId=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].albumId").value(5));

        verify(photoService).findByAlbumId(5L, new CollectionPage(0, 100));
    }

    private Photo photo(Long id, Long albumId) {
        Photo photo = new Photo();
        photo.setId(id);
        photo.setAlbumId(albumId);
        photo.setTitle("Photo");
        photo.setUrl("https://example.test/photo");
        photo.setThumbnailUrl("https://example.test/thumb");
        return photo;
    }
}