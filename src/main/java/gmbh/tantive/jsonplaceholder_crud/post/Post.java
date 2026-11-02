package gmbh.tantive.jsonplaceholder_crud.post;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Post extends ResourceModel {

    @NotNull
    @Positive
    private Long userId;
    @NotBlank
    private String title;
    @NotBlank
    private String body;
}