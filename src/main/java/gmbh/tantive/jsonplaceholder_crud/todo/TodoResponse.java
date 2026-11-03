package gmbh.tantive.jsonplaceholder_crud.todo;

public record TodoResponse(Long id, Long userId, String title, Boolean completed) {

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(todo.getId(), todo.getUserId(), todo.getTitle(), todo.getCompleted());
    }
}
