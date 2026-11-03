package gmbh.tantive.jsonplaceholder_crud.album;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.photo.Photo;
import gmbh.tantive.jsonplaceholder_crud.photo.PhotoService;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.support.CrudControllerTestSupport;
import org.junit.jupiter.api.Test;
import gmbh.tantive.jsonplaceholder_crud.support.ApiWebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ApiWebMvcTest(AlbumController.class)
class AlbumControllerTest {

    @MockitoBean
    private AlbumService albumService;

    @MockitoBean
    private PhotoService photoService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void supportsAlbumCrudRoutes() throws Exception {
        CrudControllerTestSupport.assertCrudRoutes(mockMvc, "/albums", albumService, album(1L, 7L),
                "{\"userId\":7,\"title\":\"Album\"}");
    }

    @Test
    void listsAlbumsByUserId() throws Exception {
        when(albumService.findByUserId(7L, new CollectionPage(0, 100))).thenReturn(List.of(album(1L, 7L)));

        mockMvc.perform(get("/albums?userId=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(7));

        verify(albumService).findByUserId(7L, new CollectionPage(0, 100));
    }

    @Test
    void listsPhotosByAlbumId() throws Exception {
        Photo photo = new Photo();
        photo.setId(8L);
        photo.setAlbumId(1L);
        when(photoService.findByAlbumId(1L, new CollectionPage(0, 100))).thenReturn(List.of(photo));

        mockMvc.perform(get("/albums/1/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].albumId").value(1));

        verify(photoService).findByAlbumId(1L, new CollectionPage(0, 100));
    }

    private Album album(Long id, Long userId) {
        Album album = new Album();
        album.setId(id);
        album.setUserId(userId);
        album.setTitle("Album");
        return album;
    }
}