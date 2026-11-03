package gmbh.tantive.jsonplaceholder_crud.comment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CommentRequest(@NotNull @Positive Long postId, @NotBlank String name, @NotBlank @Email String email,
        @NotBlank String body) {

    static CommentRequest from(Comment comment) {
        return new CommentRequest(comment.getPostId(), comment.getName(), comment.getEmail(), comment.getBody());
    }

    Comment toModel() {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setName(name);
        comment.setEmail(email);
        comment.setBody(body);
        return comment;
    }
}
