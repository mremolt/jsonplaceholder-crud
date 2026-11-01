package gmbh.tantive.jsonplaceholder_crud.todo;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class Todo extends ResourceModel {

    @NotNull
    @Positive
    private Long userId;
    @NotBlank
    private String title;
    @NotNull
    private Boolean completed;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}