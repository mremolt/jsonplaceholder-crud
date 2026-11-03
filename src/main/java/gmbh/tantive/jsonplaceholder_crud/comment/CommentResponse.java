package gmbh.tantive.jsonplaceholder_crud.comment;

public record CommentResponse(Long id, Long postId, String name, String email, String body) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getPostId(), comment.getName(), comment.getEmail(),
                comment.getBody());
    }
}
