package gmbh.tantive.jsonplaceholder_crud.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.LongStream;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceNotFoundException;
import gmbh.tantive.jsonplaceholder_crud.support.InMemoryResourceStore;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class DefaultPostServiceTest {

    private static final CollectionPage FIRST_PAGE = new CollectionPage(0, 10);

    private final InMemoryResourceStore store = new InMemoryResourceStore();
    private final DefaultPostService service = newService();

    @Test
    void assignsIdsAfterTheHighestImportedId() {
        service.importAll(List.of(post(11L, 4L, "First"), post(12L, 4L, "Second")));

        Post created = service.create(post(null, 4L, "Third"));

        assertThat(created.getId()).isEqualTo(13L);
        assertThat(service.findById(13L).getTitle()).isEqualTo("Third");
        assertThat(ids(service.findByUserId(4L, FIRST_PAGE))).containsExactly(11L, 12L, 13L);
    }

    @Test
    void continuesTheIdSequenceOfDataStoredByAnEarlierInstance() {
        service.importAll(List.of(post(42L, 4L, "Existing")));

        DefaultPostService restarted = newService();

        assertThat(restarted.create(post(null, 4L, "First")).getId()).isEqualTo(43L);
        assertThat(restarted.create(post(null, 4L, "Second")).getId()).isEqualTo(44L);
    }

    @Test
    void returnsTheRequestedWindowOfAllPostsInIdOrder() {
        service.importAll(LongStream.rangeClosed(1, 15).mapToObj(id -> post(id, 7L, "Post " + id)).toList());

        assertThat(ids(service.findAll(new CollectionPage(10, 5)))).containsExactly(11L, 12L, 13L, 14L, 15L);
        assertThat(ids(service.findByUserId(null, new CollectionPage(0, 2)))).containsExactly(1L, 2L);
    }

    @Test
    void continuesListsAfterTheCursorId() {
        service.importAll(LongStream.rangeClosed(1, 6).mapToObj(id -> post(id, id % 2 == 0 ? 8L : 7L, "Post " + id))
                .toList());

        assertThat(ids(service.findAll(new CollectionPage(0, 2, 2L)))).containsExactly(3L, 4L);
        assertThat(ids(service.findAll(new CollectionPage(0, 2, 6L)))).isEmpty();
        assertThat(ids(service.findByUserId(7L, new CollectionPage(0, 10, 1L)))).containsExactly(3L, 5L);
        assertThat(ids(service.findByUserId(null, new CollectionPage(0, 1, 5L)))).containsExactly(6L);
    }

    @Test
    void readsBackfillPagesWithAnIdCursor() {
        service.importAll(LongStream.rangeClosed(1, 7).mapToObj(id -> post(id, 7L, "Post " + id)).toList());

        assertThat(ids(service.findAfterId(5L, 2))).containsExactly(6L, 7L);
        assertThat(service.findAfterId(7L, 2)).isEmpty();
    }

    @Test
    void replaceStoresTheNewVersionUnderTheNewUser() {
        long id = service.create(post(null, 7L, "Title")).getId();

        service.replace(id, post(null, 9L, "Replaced"));

        assertThat(service.findByUserId(7L, FIRST_PAGE)).isEmpty();
        assertThat(service.findByUserId(9L, FIRST_PAGE)).singleElement()
                .satisfies(post -> assertThat(post.getTitle()).isEqualTo("Replaced"));
    }

    @Test
    void deleteRemovesThePostAndItsRelation() {
        long id = service.create(post(null, 7L, "Title")).getId();

        service.delete(id);

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.findByUserId(7L, FIRST_PAGE)).isEmpty();
    }

    @Test
    void reportsUnknownPostsAsNotFound() {
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No posts entry with id 99");
        assertThatThrownBy(() -> service.replace(99L, post(null, 7L, "Title")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsImportsWithoutPositiveIds() {
        assertThatThrownBy(() -> service.importAll(List.of(post(0L, 7L, "Title"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.findAll(FIRST_PAGE)).isEmpty();
    }

    private DefaultPostService newService() {
        return new DefaultPostService(store, JsonMapper.builder().build());
    }

    private static List<Long> ids(List<? extends ResourceModel> resources) {
        return resources.stream().map(ResourceModel::getId).toList();
    }

    private static Post post(Long id, Long userId, String title) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(userId);
        post.setTitle(title);
        post.setBody("Body");
        return post;
    }
}
