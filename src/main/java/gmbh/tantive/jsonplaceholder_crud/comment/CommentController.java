package gmbh.tantive.jsonplaceholder_crud.comment;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/comments")
@Tag(name = "Comments")
@SecurityRequirement(name = "bearerAuth")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "List comments, optionally filtered by post")
    public List<Comment> findAll(@RequestParam(required = false) Long postId,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return commentService.findByPostId(postId, new CollectionPage(start, limit));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get a comment by ID")
    public Comment findById(@PathVariable long id) {
        return commentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a comment")
    public Comment create(@Valid @RequestBody Comment comment) {
        return commentService.create(comment);
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Replace a comment")
    public Comment replace(@PathVariable long id, @Valid @RequestBody Comment comment) {
        return commentService.replace(id, comment);
    }

    @PatchMapping("/{id:\\d+}")
    @Operation(summary = "Partially update a comment")
    public Comment update(@PathVariable long id, @RequestBody Comment changes) {
        return commentService.update(id, changes);
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Delete a comment")
    public Map<String, Object> delete(@PathVariable long id) {
        commentService.delete(id);
        return Map.of();
    }
}