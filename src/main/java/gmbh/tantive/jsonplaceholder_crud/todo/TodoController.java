package gmbh.tantive.jsonplaceholder_crud.todo;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/todos")
@Tag(name = "Todos")
@SecurityRequirement(name = "bearerAuth")
public class TodoController extends ResourceController<Todo, TodoRequest, TodoResponse> {

    private final TodoService todoService;

    public TodoController(TodoService todoService, JsonMergePatcher patcher) {
        super(todoService, TodoRequest.class, patcher);
        this.todoService = todoService;
    }

    @GetMapping
    @Operation(summary = "List todos, optionally filtered by user and completion state")
    public List<TodoResponse> findAll(@RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return todoService.findByUserIdAndCompleted(userId, completed, new CollectionPage(start, limit)).stream()
                .map(TodoResponse::from)
                .toList();
    }

    @Override
    protected Todo toModel(TodoRequest request) {
        return request.toModel();
    }

    @Override
    protected TodoRequest toRequest(Todo todo) {
        return TodoRequest.from(todo);
    }

    @Override
    protected TodoResponse toResponse(Todo todo) {
        return TodoResponse.from(todo);
    }
}
