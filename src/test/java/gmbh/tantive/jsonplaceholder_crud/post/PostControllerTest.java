package gmbh.tantive.jsonplaceholder_crud.post;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceNotFoundException;
import gmbh.tantive.jsonplaceholder_crud.support.ApiWebMvcTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ApiWebMvcTest(PostController.class)
class PostControllerTest {

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private CommentService commentService;

    @Autowired
    private MockMvc mockMvc;

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
    void passesTheKeysetCursorToTheService() throws Exception {
        when(postService.findByUserId(7L, new CollectionPage(0, 10, 21L))).thenReturn(List.of(samplePost(22L)));

        mockMvc.perform(get("/posts?userId=7&_after=21&_limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(22));
    }

    @Test
    void rejectsCombiningOffsetAndCursor() throws Exception {
        mockMvc.perform(get("/posts?_start=10&_after=21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("_start and _after cannot be combined"));
    }

    @Test
    void rejectsOutOfRangePagination() throws Exception {
        mockMvc.perform(get("/posts?_start=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("_start must be between 0 and 10000"));
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
        when(postService.findById(99L)).thenThrow(new ResourceNotFoundException("posts", 99L));

        mockMvc.perform(get("/posts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No posts entry with id 99"));
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
    void patchesOnlyTheGivenFieldsWithAMergePatch() throws Exception {
        when(postService.findById(1L)).thenReturn(samplePost(1L));
        when(postService.replace(eq(1L), any())).thenAnswer(invocation -> {
            Post replaced = invocation.getArgument(1);
            replaced.setId(1L);
            return replaced;
        });

        mockMvc.perform(patch("/posts/1").contentType("application/merge-patch+json")
                .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.body").value("Body"));

        ArgumentCaptor<Post> replaced = ArgumentCaptor.forClass(Post.class);
        verify(postService).replace(eq(1L), replaced.capture());
        Assertions.assertEquals("Updated", replaced.getValue().getTitle());
        Assertions.assertEquals("Body", replaced.getValue().getBody());
        Assertions.assertEquals(7L, replaced.getValue().getUserId());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"title\":\" \"}  | title",
            "{\"body\":null}     | body",
            "{\"userId\":-1}     | userId" })
    void rejectsAPatchWhoseResultIsInvalid(String patch, String field) throws Exception {
        when(postService.findById(1L)).thenReturn(samplePost(1L));

        mockMvc.perform(patch("/posts/1").contentType("application/merge-patch+json").content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());

        verify(postService, never()).replace(eq(1L), any());
    }

    @Test
    void rejectsUnreadablePatches() throws Exception {
        when(postService.findById(1L)).thenReturn(samplePost(1L));

        mockMvc.perform(patch("/posts/1").contentType("application/merge-patch+json")
                .content("{\"userId\":\"seven\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith(
                        "The patched resource is not readable")));
        mockMvc.perform(patch("/posts/1").contentType(MediaType.TEXT_PLAIN).content("title=x"))
                .andExpect(status().isUnsupportedMediaType());
        verify(postService, never()).replace(eq(1L), any());
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