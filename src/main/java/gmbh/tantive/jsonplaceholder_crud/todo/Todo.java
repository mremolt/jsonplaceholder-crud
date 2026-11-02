package gmbh.tantive.jsonplaceholder_crud.todo;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Todo extends ResourceModel {

    @NotNull
    @Positive
    private Long userId;
    @NotBlank
    private String title;
    @NotNull
    private Boolean completed;
}