package gmbh.tantive.jsonplaceholder_crud.todo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;
import gmbh.tantive.jsonplaceholder_crud.support.InMemoryResourceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class DefaultTodoServiceTest {

    private static final CollectionPage FIRST_PAGE = new CollectionPage(0, 10);

    private final DefaultTodoService service = new DefaultTodoService(new InMemoryResourceStore(),
            JsonMapper.builder().build());

    @BeforeEach
    void setUp() {
        service.importAll(List.of(todo(1L, 4L, true), todo(2L, 4L, false), todo(3L, 5L, true)));
    }

    @Test
    void filtersByUserCompletionStateOrBoth() {
        assertThat(ids(service.findByUserIdAndCompleted(4L, null, FIRST_PAGE))).containsExactly(1L, 2L);
        assertThat(ids(service.findByUserIdAndCompleted(null, true, FIRST_PAGE))).containsExactly(1L, 3L);
        assertThat(ids(service.findByUserIdAndCompleted(null, false, FIRST_PAGE))).containsExactly(2L);
        assertThat(ids(service.findByUserIdAndCompleted(4L, true, FIRST_PAGE))).containsExactly(1L);
        assertThat(ids(service.findByUserIdAndCompleted(null, null, FIRST_PAGE))).containsExactly(1L, 2L, 3L);
    }

    @Test
    void completingATodoMovesItBetweenCompletionPartitions() {
        service.replace(2L, todo(2L, 4L, true));

        assertThat(ids(service.findByUserIdAndCompleted(null, false, FIRST_PAGE))).isEmpty();
        assertThat(ids(service.findByUserIdAndCompleted(4L, false, FIRST_PAGE))).isEmpty();
        assertThat(ids(service.findByUserIdAndCompleted(4L, true, FIRST_PAGE))).containsExactly(1L, 2L);
        assertThat(ids(service.findByUserIdAndCompleted(null, true, FIRST_PAGE))).containsExactly(1L, 2L, 3L);
    }

    @Test
    void reassigningATodoMovesAllItsUserPartitions() {
        service.replace(1L, todo(1L, 5L, true));

        assertThat(ids(service.findByUserIdAndCompleted(4L, null, FIRST_PAGE))).containsExactly(2L);
        assertThat(ids(service.findByUserIdAndCompleted(4L, true, FIRST_PAGE))).isEmpty();
        assertThat(ids(service.findByUserIdAndCompleted(5L, true, FIRST_PAGE))).containsExactly(1L, 3L);
    }

    @Test
    void deleteRemovesTheTodoFromEveryFilter() {
        service.delete(1L);

        assertThat(ids(service.findByUserIdAndCompleted(4L, null, FIRST_PAGE))).containsExactly(2L);
        assertThat(ids(service.findByUserIdAndCompleted(4L, true, FIRST_PAGE))).isEmpty();
        assertThat(ids(service.findByUserIdAndCompleted(null, true, FIRST_PAGE))).containsExactly(3L);
    }

    private static List<Long> ids(List<? extends ResourceModel> resources) {
        return resources.stream().map(ResourceModel::getId).toList();
    }

    private static Todo todo(long id, long userId, boolean completed) {
        Todo todo = new Todo();
        todo.setId(id);
        todo.setUserId(userId);
        todo.setTitle("Todo " + id);
        todo.setCompleted(completed);
        return todo;
    }
}
