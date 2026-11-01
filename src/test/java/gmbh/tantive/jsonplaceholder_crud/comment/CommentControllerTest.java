package gmbh.tantive.jsonplaceholder_crud.comment;

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
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CommentController(commentService)).build();
    }

    @Test
    void supportsCommentCrudRoutes() throws Exception {
        Comment comment = comment(1L, 3L);
        CrudControllerTestSupport.assertCrudRoutes(mockMvc, "/comments", commentService, comment,
                "{\"postId\":3,\"name\":\"Comment\",\"email\":\"comment@example.com\",\"body\":\"Body\"}");
    }

    @Test
    void listsCommentsByPostId() throws Exception {
        when(commentService.findByPostId(3L, new CollectionPage(0, 100))).thenReturn(List.of(comment(1L, 3L)));

        mockMvc.perform(get("/comments?postId=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value(3));

        verify(commentService).findByPostId(3L, new CollectionPage(0, 100));
    }

    private Comment comment(Long id, Long postId) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setPostId(postId);
        comment.setName("Comment");
        comment.setEmail("comment@example.com");
        comment.setBody("Body");
        return comment;
    }
}