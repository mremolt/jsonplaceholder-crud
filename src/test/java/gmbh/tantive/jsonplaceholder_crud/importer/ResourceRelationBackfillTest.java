package gmbh.tantive.jsonplaceholder_crud.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.LongStream;

import gmbh.tantive.jsonplaceholder_crud.post.Post;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceBulkService;
import gmbh.tantive.jsonplaceholder_crud.support.RecordingBulkService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class ResourceRelationBackfillTest {

    @Test
    void reimportsEveryStoredResourceToRebuildItsRelations() {
        RecordingBulkService posts = new RecordingBulkService("posts", List.of(post(42L)));
        RecordingBulkService users = new RecordingBulkService("users");

        new ResourceRelationBackfill(List.<ResourceBulkService<?>>of(posts, users)).run(new DefaultApplicationArguments());

        assertThat(posts.imports()).singleElement()
                .satisfies(batch -> assertThat(batch).extracting(Post::getId).containsExactly(42L));
        assertThat(users.imports()).isEmpty();
    }

    @Test
    void advancesTheIdCursorAcrossFullBatches() {
        List<Post> stored = LongStream.rangeClosed(1L, 501L).mapToObj(ResourceRelationBackfillTest::post).toList();
        RecordingBulkService posts = new RecordingBulkService("posts", stored);

        new ResourceRelationBackfill(List.<ResourceBulkService<?>>of(posts)).run(new DefaultApplicationArguments());

        assertThat(posts.imports()).extracting(List::size).containsExactly(500, 1);
        assertThat(posts.imports().getLast()).extracting(Post::getId).containsExactly(501L);
    }

    private static Post post(long id) {
        Post post = new Post();
        post.setId(id);
        return post;
    }
}
