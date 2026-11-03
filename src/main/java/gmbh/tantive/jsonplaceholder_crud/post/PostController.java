package gmbh.tantive.jsonplaceholder_crud.post;

import static gmbh.tantive.jsonplaceholder_crud.api.ResourceController.AFTER_DESCRIPTION;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import gmbh.tantive.jsonplaceholder_crud.api.JsonMergePatcher;
import gmbh.tantive.jsonplaceholder_crud.api.ResourceController;
import gmbh.tantive.jsonplaceholder_crud.comment.CommentResponse;
import gmbh.tantive.jsonplaceholder_crud.comment.CommentService;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
@Tag(name = "Posts")
@SecurityRequirement(name = "bearerAuth")
public class PostController extends ResourceController<Post, PostRequest, PostResponse> {

    private final PostService postService;
    private final CommentService commentService;

    public PostController(PostService postService, CommentService commentService, JsonMergePatcher patcher) {
        super(postService, PostRequest.class, patcher);
        this.postService = postService;
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "List posts, optionally filtered by user")
    public List<PostResponse> findAll(@RequestParam(required = false) Long userId,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit,
            @Parameter(description = AFTER_DESCRIPTION) @RequestParam(name = "_after", required = false) Long after) {
        return postService.findByUserId(userId, new CollectionPage(start, limit, after)).stream()
                .map(PostResponse::from)
                .toList();
    }

    @GetMapping("/{id:\\d+}/comments")
    @Operation(summary = "List comments for a post")
    public List<CommentResponse> comments(@PathVariable long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit,
            @Parameter(description = AFTER_DESCRIPTION) @RequestParam(name = "_after", required = false) Long after) {
        return commentService.findByPostId(id, new CollectionPage(start, limit, after)).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Override
    protected Post toModel(PostRequest request) {
        return request.toModel();
    }

    @Override
    protected PostRequest toRequest(Post post) {
        return PostRequest.from(post);
    }

    @Override
    protected PostResponse toResponse(Post post) {
        return PostResponse.from(post);
    }
}
