package gmbh.tantive.jsonplaceholder_crud.user;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.album.Album;
import gmbh.tantive.jsonplaceholder_crud.album.AlbumService;
import gmbh.tantive.jsonplaceholder_crud.post.Post;
import gmbh.tantive.jsonplaceholder_crud.post.PostService;
import gmbh.tantive.jsonplaceholder_crud.support.CrudControllerTestSupport;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.todo.Todo;
import gmbh.tantive.jsonplaceholder_crud.todo.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final String VALID_USER_JSON = "{\"name\":\"Name\",\"username\":\"user\","
            + "\"email\":\"user@example.com\",\"address\":{\"street\":\"street\","
            + "\"suite\":\"suite\",\"city\":\"city\",\"zipcode\":\"12345\","
            + "\"geo\":{\"lat\":\"1\",\"lng\":\"2\"}},\"phone\":\"123\","
            + "\"website\":\"example.test\",\"company\":{\"name\":\"Company\","
            + "\"catchPhrase\":\"Phrase\",\"bs\":\"b\"}}";

    @Mock
    private UserService userService;

    @Mock
    private PostService postService;

    @Mock
    private AlbumService albumService;

    @Mock
    private TodoService todoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new UserController(userService, postService, albumService, todoService)).build();
    }

    @Test
    void supportsUserCrudRoutes() throws Exception {
        CrudControllerTestSupport.assertCrudRoutes(mockMvc, "/users", userService, user(1L), VALID_USER_JSON);
    }

    @Test
    void rejectsInvalidNestedAddress() throws Exception {
        String invalidUser = "{\"name\":\"Name\",\"username\":\"user\","
                + "\"email\":\"user@example.com\",\"address\":{},\"phone\":\"123\","
                + "\"website\":\"example.test\",\"company\":{\"name\":\"Company\","
                + "\"catchPhrase\":\"Phrase\",\"bs\":\"b\"}}";

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(invalidUser))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsUsersById() throws Exception {
        when(userService.findByIdFilter(1L, new CollectionPage(0, 100))).thenReturn(List.of(user(1L)));

        mockMvc.perform(get("/users?id=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Leanne Graham"));

        verify(userService).findByIdFilter(1L, new CollectionPage(0, 100));
    }

    @Test
    void listsTypedResourcesForUser() throws Exception {
        Post post = new Post();
        post.setId(2L);
        when(postService.findByUserId(1L, new CollectionPage(0, 100))).thenReturn(List.of(post));
        Album album = new Album();
        album.setId(3L);
        when(albumService.findByUserId(1L, new CollectionPage(0, 100))).thenReturn(List.of(album));
        Todo todo = new Todo();
        todo.setId(4L);
        when(todoService.findByUserIdAndCompleted(1L, null, new CollectionPage(0, 100)))
                .thenReturn(List.of(todo));

        mockMvc.perform(get("/users/1/posts")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(2));
        mockMvc.perform(get("/users/1/albums")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(3));
        mockMvc.perform(get("/users/1/todos")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(4));

        verify(postService).findByUserId(1L, new CollectionPage(0, 100));
        verify(albumService).findByUserId(1L, new CollectionPage(0, 100));
        verify(todoService).findByUserIdAndCompleted(1L, null, new CollectionPage(0, 100));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Leanne Graham");
        user.setUsername("Bret");
        user.setEmail("Sincere@april.biz");
        return user;
    }
}