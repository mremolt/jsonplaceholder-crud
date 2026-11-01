package gmbh.tantive.jsonplaceholder_crud.post;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class Post extends ResourceModel {

    @NotNull
    @Positive
    private Long userId;
    @NotBlank
    private String title;
    @NotBlank
    private String body;

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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}