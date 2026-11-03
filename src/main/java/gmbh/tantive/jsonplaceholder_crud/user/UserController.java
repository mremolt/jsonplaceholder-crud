package gmbh.tantive.jsonplaceholder_crud.user;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import gmbh.tantive.jsonplaceholder_crud.album.AlbumResponse;
import gmbh.tantive.jsonplaceholder_crud.album.AlbumService;
import gmbh.tantive.jsonplaceholder_crud.api.JsonMergePatcher;
import gmbh.tantive.jsonplaceholder_crud.api.ResourceController;
import gmbh.tantive.jsonplaceholder_crud.post.PostResponse;
import gmbh.tantive.jsonplaceholder_crud.post.PostService;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.todo.TodoResponse;
import gmbh.tantive.jsonplaceholder_crud.todo.TodoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController extends ResourceController<User, UserRequest, UserResponse> {

    private final UserService userService;
    private final PostService postService;
    private final AlbumService albumService;
    private final TodoService todoService;

    public UserController(UserService userService, PostService postService, AlbumService albumService,
            TodoService todoService, JsonMergePatcher patcher) {
        super(userService, UserRequest.class, patcher);
        this.userService = userService;
        this.postService = postService;
        this.albumService = albumService;
        this.todoService = todoService;
    }

    @GetMapping
    @Operation(summary = "List users, optionally filtered by ID")
    public List<UserResponse> findAll(@RequestParam(required = false) Long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit,
            @Parameter(description = AFTER_DESCRIPTION) @RequestParam(name = "_after", required = false) Long after) {
        return userService.findByIdFilter(id, new CollectionPage(start, limit, after)).stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/{id:\\d+}/posts")
    @Operation(summary = "List posts for a user")
    public List<PostResponse> posts(@PathVariable long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit,
            @Parameter(description = AFTER_DESCRIPTION) @RequestParam(name = "_after", required = false) Long after) {
        return postService.findByUserId(id, new CollectionPage(start, limit, after)).stream()
                .map(PostResponse::from)
                .toList();
    }

    @GetMapping("/{id:\\d+}/albums")
    @Operation(summary = "List albums for a user")
    public List<AlbumResponse> albums(@PathVariable long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit,
            @Parameter(description = AFTER_DESCRIPTION) @RequestParam(name = "_after", required = false) Long after) {
        return albumService.findByUserId(id, new CollectionPage(start, limit, after)).stream()
                .map(AlbumResponse::from)
                .toList();
    }

    @GetMapping("/{id:\\d+}/todos")
    @Operation(summary = "List todos for a user")
    public List<TodoResponse> todos(@PathVariable long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit,
            @Parameter(description = AFTER_DESCRIPTION) @RequestParam(name = "_after", required = false) Long after) {
        return todoService.findByUserIdAndCompleted(id, null, new CollectionPage(start, limit, after)).stream()
                .map(TodoResponse::from)
                .toList();
    }

    @Override
    protected User toModel(UserRequest request) {
        return request.toModel();
    }

    @Override
    protected UserRequest toRequest(User user) {
        return UserRequest.from(user);
    }

    @Override
    protected UserResponse toResponse(User user) {
        return UserResponse.from(user);
    }
}
