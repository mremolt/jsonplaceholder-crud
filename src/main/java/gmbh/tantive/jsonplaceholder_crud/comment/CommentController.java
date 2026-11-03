package gmbh.tantive.jsonplaceholder_crud.comment;

import static gmbh.tantive.jsonplaceholder_crud.api.ResourceController.AFTER_DESCRIPTION;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import gmbh.tantive.jsonplaceholder_crud.api.JsonMergePatcher;
import gmbh.tantive.jsonplaceholder_crud.api.ResourceController;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comments")
@Tag(name = "Comments")
@SecurityRequirement(name = "bearerAuth")
public class CommentController extends ResourceController<Comment, CommentRequest, CommentResponse> {

    private final CommentService commentService;

    public CommentController(CommentService commentService, JsonMergePatcher patcher) {
        super(commentService, CommentRequest.class, patcher);
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "List comments, optionally filtered by post")
    public List<CommentResponse> findAll(@RequestParam(required = false) Long postId,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit,
            @Parameter(description = AFTER_DESCRIPTION) @RequestParam(name = "_after", required = false) Long after) {
        return commentService.findByPostId(postId, new CollectionPage(start, limit, after)).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Override
    protected Comment toModel(CommentRequest request) {
        return request.toModel();
    }

    @Override
    protected CommentRequest toRequest(Comment comment) {
        return CommentRequest.from(comment);
    }

    @Override
    protected CommentResponse toResponse(Comment comment) {
        return CommentResponse.from(comment);
    }
}
