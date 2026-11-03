package gmbh.tantive.jsonplaceholder_crud.comment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;
import gmbh.tantive.jsonplaceholder_crud.support.InMemoryResourceStore;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class DefaultCommentServiceTest {

    private static final CollectionPage FIRST_PAGE = new CollectionPage(0, 10);

    private final CommentService service = new DefaultCommentService(new InMemoryResourceStore(),
            JsonMapper.builder().build());

    @Test
    void findsCommentsByPostAndFollowsAPostChange() {
        long first = service.create(comment(1L)).getId();
        long second = service.create(comment(1L)).getId();
        service.create(comment(2L));

        assertThat(ids(service.findByPostId(1L, FIRST_PAGE))).containsExactly(first, second);

        service.replace(second, comment(2L));

        assertThat(ids(service.findByPostId(1L, FIRST_PAGE))).containsExactly(first);
        assertThat(service.findByPostId(2L, FIRST_PAGE)).hasSize(2);
        assertThat(service.findByPostId(null, FIRST_PAGE)).hasSize(3);
    }

    private static List<Long> ids(List<? extends ResourceModel> resources) {
        return resources.stream().map(ResourceModel::getId).toList();
    }

    private static Comment comment(long postId) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setName("Name");
        comment.setEmail("reader@example.com");
        comment.setBody("Body");
        return comment;
    }
}
