package gmbh.tantive.jsonplaceholder_crud.todo;

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
@RequestMapping("/todos")
@Tag(name = "Todos")
@SecurityRequirement(name = "bearerAuth")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    @Operation(summary = "List todos, optionally filtered by user and completion state")
    public List<Todo> findAll(@RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return todoService.findByUserIdAndCompleted(userId, completed, new CollectionPage(start, limit));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get a todo by ID")
    public Todo findById(@PathVariable long id) {
        return todoService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a todo")
    public Todo create(@Valid @RequestBody Todo todo) {
        return todoService.create(todo);
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Replace a todo")
    public Todo replace(@PathVariable long id, @Valid @RequestBody Todo todo) {
        return todoService.replace(id, todo);
    }

    @PatchMapping("/{id:\\d+}")
    @Operation(summary = "Partially update a todo")
    public Todo update(@PathVariable long id, @RequestBody Todo changes) {
        return todoService.update(id, changes);
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Delete a todo")
    public Map<String, Object> delete(@PathVariable long id) {
        todoService.delete(id);
        return Map.of();
    }
}