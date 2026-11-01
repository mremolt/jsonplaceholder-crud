package gmbh.tantive.jsonplaceholder_crud.post;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.comment.Comment;
import gmbh.tantive.jsonplaceholder_crud.comment.CommentService;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PostController(postService, commentService)).build();
    }

    @Test
    void listsPostsByUser() throws Exception {
        when(postService.findByUserId(7L, new CollectionPage(20, 10))).thenReturn(List.of(samplePost(21L)));

        mockMvc.perform(get("/posts?userId=7&_start=20&_limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].userId").value(7));

        verify(postService).findByUserId(7L, new CollectionPage(20, 10));
    }

    @Test
    void rejectsOutOfRangePagination() throws Exception {
        mockMvc.perform(get("/posts?_start=-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getsPostById() throws Exception {
        when(postService.findById(1L)).thenReturn(samplePost(1L));

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(postService).findById(1L);
    }

    @Test
    void returnsNotFoundForUnknownPost() throws Exception {
        when(postService.findById(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/posts/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsPost() throws Exception {
        when(postService.create(any())).thenReturn(samplePost(11L));

        mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":7,\"title\":\"Title\",\"body\":\"Body\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11));

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postService).create(postCaptor.capture());
        Assertions.assertEquals(7L, postCaptor.getValue().getUserId());
    }

    @Test
    void rejectsPostWithoutRequiredFields() throws Exception {
        mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":7}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replacesPost() throws Exception {
        when(postService.replace(eq(1L), any())).thenReturn(samplePost(1L));

        mockMvc.perform(put("/posts/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":7,\"title\":\"Title\",\"body\":\"Body\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(postService).replace(eq(1L), any(Post.class));
    }

    @Test
    void patchesPostWithOnlyChangedFields() throws Exception {
        when(postService.update(eq(1L), any())).thenReturn(samplePost(1L));

        mockMvc.perform(patch("/posts/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<Post> changesCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postService).update(eq(1L), changesCaptor.capture());
        Assertions.assertEquals("Updated", changesCaptor.getValue().getTitle());
    }

    @Test
    void deletesPost() throws Exception {
        mockMvc.perform(delete("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));

        verify(postService).delete(1L);
    }

    @Test
    void listsCommentsForPost() throws Exception {
        Comment comment = new Comment();
        comment.setId(5L);
        comment.setPostId(1L);
        when(commentService.findByPostId(1L, new CollectionPage(0, 100))).thenReturn(List.of(comment));

        mockMvc.perform(get("/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value(1));

        verify(commentService).findByPostId(1L, new CollectionPage(0, 100));
    }

    private Post samplePost(Long id) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(7L);
        post.setTitle("Title");
        post.setBody("Body");
        return post;
    }
}