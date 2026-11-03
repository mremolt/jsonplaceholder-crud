package gmbh.tantive.jsonplaceholder_crud.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PostRequest(@NotNull @Positive Long userId, @NotBlank String title, @NotBlank String body) {

    static PostRequest from(Post post) {
        return new PostRequest(post.getUserId(), post.getTitle(), post.getBody());
    }

    Post toModel() {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(title);
        post.setBody(body);
        return post;
    }
}
