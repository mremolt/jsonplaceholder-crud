package gmbh.tantive.jsonplaceholder_crud.album;

public record AlbumResponse(Long id, Long userId, String title) {

    public static AlbumResponse from(Album album) {
        return new AlbumResponse(album.getId(), album.getUserId(), album.getTitle());
    }
}
