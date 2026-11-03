package gmbh.tantive.jsonplaceholder_crud.post;

public record PostResponse(Long id, Long userId, String title, String body) {

    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getUserId(), post.getTitle(), post.getBody());
    }
}
