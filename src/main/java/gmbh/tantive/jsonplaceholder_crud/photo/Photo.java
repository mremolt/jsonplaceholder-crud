package gmbh.tantive.jsonplaceholder_crud.photo;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Photo extends ResourceModel {

    @NotNull
    @Positive
    private Long albumId;
    @NotBlank
    private String title;
    @NotBlank
    private String url;
    @NotBlank
    private String thumbnailUrl;
}