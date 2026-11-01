package gmbh.tantive.jsonplaceholder_crud.importer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.LongStream;

import gmbh.tantive.jsonplaceholder_crud.album.AlbumService;
import gmbh.tantive.jsonplaceholder_crud.comment.CommentService;
import gmbh.tantive.jsonplaceholder_crud.photo.PhotoService;
import gmbh.tantive.jsonplaceholder_crud.post.Post;
import gmbh.tantive.jsonplaceholder_crud.post.PostService;
import gmbh.tantive.jsonplaceholder_crud.todo.TodoService;
import gmbh.tantive.jsonplaceholder_crud.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class ResourceRelationBackfillTest {

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private AlbumService albumService;

    @Mock
    private PhotoService photoService;

    @Mock
    private TodoService todoService;

    @Mock
    private UserService userService;

    @Test
    void backfillsExistingPostsUsingAnIdCursor() {
        Post post = new Post();
        post.setId(42L);
        when(postService.findAfterId(0L, 500)).thenReturn(List.of(post));
        ResourceRelationBackfill backfill = new ResourceRelationBackfill(postService, commentService, albumService,
                photoService, todoService, userService);

        backfill.run(new DefaultApplicationArguments());

        verify(postService).importAll(List.of(post));
        verify(postService).findAfterId(0L, 500);
        verify(commentService).findAfterId(0L, 500);
        verify(albumService).findAfterId(0L, 500);
        verify(photoService).findAfterId(0L, 500);
        verify(todoService).findAfterId(0L, 500);
        verify(userService).findAfterId(0L, 500);
    }

    @Test
    void advancesTheIdCursorAcrossFullBatches() {
        List<Post> firstBatch = LongStream.rangeClosed(1L, 500L)
                .mapToObj(id -> post(id))
                .toList();
        Post finalPost = post(501L);
        when(postService.findAfterId(0L, 500)).thenReturn(firstBatch);
        when(postService.findAfterId(500L, 500)).thenReturn(List.of(finalPost));
        ResourceRelationBackfill backfill = new ResourceRelationBackfill(postService, commentService, albumService,
                photoService, todoService, userService);

        backfill.run(new DefaultApplicationArguments());

        verify(postService).importAll(firstBatch);
        verify(postService).importAll(List.of(finalPost));
        verify(postService).findAfterId(0L, 500);
        verify(postService).findAfterId(500L, 500);
    }

    private Post post(Long id) {
        Post post = new Post();
        post.setId(id);
        return post;
    }
}