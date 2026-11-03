package gmbh.tantive.jsonplaceholder_crud.photo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PhotoRequest(@NotNull @Positive Long albumId, @NotBlank String title, @NotBlank String url,
        @NotBlank String thumbnailUrl) {

    static PhotoRequest from(Photo photo) {
        return new PhotoRequest(photo.getAlbumId(), photo.getTitle(), photo.getUrl(), photo.getThumbnailUrl());
    }

    Photo toModel() {
        Photo photo = new Photo();
        photo.setAlbumId(albumId);
        photo.setTitle(title);
        photo.setUrl(url);
        photo.setThumbnailUrl(thumbnailUrl);
        return photo;
    }
}
