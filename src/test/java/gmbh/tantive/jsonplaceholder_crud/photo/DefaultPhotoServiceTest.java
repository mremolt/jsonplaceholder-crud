package gmbh.tantive.jsonplaceholder_crud.photo;

import static org.assertj.core.api.Assertions.assertThat;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.support.InMemoryResourceStore;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class DefaultPhotoServiceTest {

    private static final CollectionPage FIRST_PAGE = new CollectionPage(0, 10);

    private final PhotoService service = new DefaultPhotoService(new InMemoryResourceStore(),
            JsonMapper.builder().build());

    @Test
    void findsPhotosByAlbumUntilTheyAreDeleted() {
        long id = service.create(photo(6L)).getId();
        service.create(photo(7L));

        assertThat(service.findByAlbumId(6L, FIRST_PAGE)).extracting(Photo::getId).containsExactly(id);

        service.delete(id);

        assertThat(service.findByAlbumId(6L, FIRST_PAGE)).isEmpty();
        assertThat(service.findByAlbumId(7L, FIRST_PAGE)).hasSize(1);
    }

    private static Photo photo(long albumId) {
        Photo photo = new Photo();
        photo.setAlbumId(albumId);
        photo.setTitle("Photo");
        photo.setUrl("https://example.com/photo.png");
        photo.setThumbnailUrl("https://example.com/thumb.png");
        return photo;
    }
}
