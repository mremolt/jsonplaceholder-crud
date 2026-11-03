package gmbh.tantive.jsonplaceholder_crud.photo;

public record PhotoResponse(Long id, Long albumId, String title, String url, String thumbnailUrl) {

    public static PhotoResponse from(Photo photo) {
        return new PhotoResponse(photo.getId(), photo.getAlbumId(), photo.getTitle(), photo.getUrl(),
                photo.getThumbnailUrl());
    }
}
