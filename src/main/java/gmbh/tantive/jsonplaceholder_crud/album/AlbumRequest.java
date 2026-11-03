package gmbh.tantive.jsonplaceholder_crud.album;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AlbumRequest(@NotNull @Positive Long userId, @NotBlank String title) {

    static AlbumRequest from(Album album) {
        return new AlbumRequest(album.getUserId(), album.getTitle());
    }

    Album toModel() {
        Album album = new Album();
        album.setUserId(userId);
        album.setTitle(title);
        return album;
    }
}
