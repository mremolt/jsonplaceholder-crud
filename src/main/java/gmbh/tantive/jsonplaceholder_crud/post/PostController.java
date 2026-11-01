package gmbh.tantive.jsonplaceholder_crud.post;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import gmbh.tantive.jsonplaceholder_crud.comment.Comment;
import gmbh.tantive.jsonplaceholder_crud.comment.CommentService;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
@Tag(name = "Posts")
@SecurityRequirement(name = "bearerAuth")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    public PostController(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "List posts, optionally filtered by user")
    public List<Post> findAll(@RequestParam(required = false) Long userId,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return postService.findByUserId(userId, new CollectionPage(start, limit));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get a post by ID")
    public Post findById(@PathVariable long id) {
        return postService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a post")
    public Post create(@Valid @RequestBody Post post) {
        return postService.create(post);
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Replace a post")
    public Post replace(@PathVariable long id, @Valid @RequestBody Post post) {
        return postService.replace(id, post);
    }

    @PatchMapping("/{id:\\d+}")
    @Operation(summary = "Partially update a post")
    public Post update(@PathVariable long id, @RequestBody Post changes) {
        return postService.update(id, changes);
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Delete a post")
    public Map<String, Object> delete(@PathVariable long id) {
        postService.delete(id);
        return Map.of();
    }

    @GetMapping("/{id:\\d+}/comments")
    @Operation(summary = "List comments for a post")
    public List<Comment> comments(@PathVariable long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return commentService.findByPostId(id, new CollectionPage(start, limit));
    }
}