package gmbh.tantive.jsonplaceholder_crud.album;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;
import gmbh.tantive.jsonplaceholder_crud.support.InMemoryResourceStore;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class DefaultAlbumServiceTest {

    private static final CollectionPage FIRST_PAGE = new CollectionPage(0, 10);

    private final AlbumService service = new DefaultAlbumService(new InMemoryResourceStore(),
            JsonMapper.builder().build());

    @Test
    void findsAlbumsByUserAndFollowsAReplace() {
        long id = service.create(album(3L, "Holiday")).getId();
        service.create(album(4L, "Other"));

        assertThat(ids(service.findByUserId(3L, FIRST_PAGE))).containsExactly(id);

        service.replace(id, album(4L, "Moved"));

        assertThat(service.findByUserId(3L, FIRST_PAGE)).isEmpty();
        assertThat(service.findByUserId(4L, FIRST_PAGE)).extracting(Album::getTitle)
                .containsExactly("Moved", "Other");
    }

    private static List<Long> ids(List<? extends ResourceModel> resources) {
        return resources.stream().map(ResourceModel::getId).toList();
    }

    private static Album album(long userId, String title) {
        Album album = new Album();
        album.setUserId(userId);
        album.setTitle(title);
        return album;
    }
}
