package gmbh.tantive.jsonplaceholder_crud.user;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import gmbh.tantive.jsonplaceholder_crud.album.Album;
import gmbh.tantive.jsonplaceholder_crud.album.AlbumService;
import gmbh.tantive.jsonplaceholder_crud.post.Post;
import gmbh.tantive.jsonplaceholder_crud.post.PostService;
import gmbh.tantive.jsonplaceholder_crud.todo.Todo;
import gmbh.tantive.jsonplaceholder_crud.todo.TodoService;
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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final AlbumService albumService;
    private final TodoService todoService;

    @GetMapping
    @Operation(summary = "List users, optionally filtered by ID")
    public List<User> findAll(@RequestParam(required = false) Long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return userService.findByIdFilter(id, new CollectionPage(start, limit));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get a user by ID")
    public User findById(@PathVariable long id) {
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user")
    public User create(@Valid @RequestBody User user) {
        return userService.create(user);
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Replace a user")
    public User replace(@PathVariable long id, @Valid @RequestBody User user) {
        return userService.replace(id, user);
    }

    @PatchMapping("/{id:\\d+}")
    @Operation(summary = "Partially update a user")
    public User update(@PathVariable long id, @RequestBody User changes) {
        return userService.update(id, changes);
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Delete a user")
    public Map<String, Object> delete(@PathVariable long id) {
        userService.delete(id);
        return Map.of();
    }

    @GetMapping("/{id:\\d+}/posts")
    @Operation(summary = "List posts for a user")
    public List<Post> posts(@PathVariable long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return postService.findByUserId(id, new CollectionPage(start, limit));
    }

    @GetMapping("/{id:\\d+}/albums")
    @Operation(summary = "List albums for a user")
    public List<Album> albums(@PathVariable long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return albumService.findByUserId(id, new CollectionPage(start, limit));
    }

    @GetMapping("/{id:\\d+}/todos")
    @Operation(summary = "List todos for a user")
    public List<Todo> todos(@PathVariable long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return todoService.findByUserIdAndCompleted(id, null, new CollectionPage(start, limit));
    }
}