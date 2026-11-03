package gmbh.tantive.jsonplaceholder_crud.todo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TodoRequest(@NotNull @Positive Long userId, @NotBlank String title, @NotNull Boolean completed) {

    static TodoRequest from(Todo todo) {
        return new TodoRequest(todo.getUserId(), todo.getTitle(), todo.getCompleted());
    }

    Todo toModel() {
        Todo todo = new Todo();
        todo.setUserId(userId);
        todo.setTitle(title);
        todo.setCompleted(completed);
        return todo;
    }
}
