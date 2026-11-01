package gmbh.tantive.jsonplaceholder_crud.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.comment.Comment;
import gmbh.tantive.jsonplaceholder_crud.comment.CommentService;
import gmbh.tantive.jsonplaceholder_crud.post.Post;
import gmbh.tantive.jsonplaceholder_crud.post.PostService;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.todo.Todo;
import gmbh.tantive.jsonplaceholder_crud.todo.TodoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CassandraRelationIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    private static final CassandraContainer CASSANDRA = new CassandraContainer("cassandra:5.0")
            .withInitScript("schema.cql");

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private TodoService todoService;

    @DynamicPropertySource
    static void cassandraProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cassandra.contact-points", CASSANDRA::getHost);
        registry.add("spring.cassandra.port", CASSANDRA::getFirstMappedPort);
        registry.add("spring.cassandra.local-datacenter", CASSANDRA::getLocalDatacenter);
        registry.add("spring.cassandra.keyspace-name", () -> "jsonplaceholder");
        registry.add("spring.cassandra.schema-action", () -> "create_if_not_exists");
        registry.add("security.jwt.secret", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("jsonplaceholder.import.enabled", () -> false);
        registry.add("jsonplaceholder.relations.backfill.enabled", () -> false);
    }

    @Test
    void persistsPagesAndMaintainsRelationRowsAcrossUpdatesAndDeletes() {
        Post post = new Post();
        post.setUserId(9001L);
        post.setTitle("Integration post");
        post.setBody("Body");
        Post savedPost = postService.create(post);

        assertEquals(List.of(savedPost.getId()), postService.findByUserId(9001L, new CollectionPage(0, 10))
                .stream().map(result -> result.getId()).toList());
        assertEquals(List.of(savedPost.getId()), postService.findAll(new CollectionPage(0, 10))
                .stream().map(result -> result.getId()).toList());

        Comment comment = new Comment();
        comment.setPostId(savedPost.getId());
        comment.setName("Integration comment");
        comment.setEmail("integration@example.com");
        comment.setBody("Body");
        Comment savedComment = commentService.create(comment);
        assertEquals(List.of(savedComment.getId()), commentService
                .findByPostId(savedPost.getId(), new CollectionPage(0, 10))
                .stream().map(result -> result.getId()).toList());

        Post changes = new Post();
        changes.setUserId(9002L);
        postService.update(savedPost.getId(), changes);
        assertEquals(List.of(), postService.findByUserId(9001L, new CollectionPage(0, 10)));
        assertEquals(List.of(savedPost.getId()), postService.findByUserId(9002L, new CollectionPage(0, 10))
                .stream().map(result -> result.getId()).toList());

        commentService.delete(savedComment.getId());
        assertEquals(List.of(), commentService.findByPostId(savedPost.getId(), new CollectionPage(0, 10)));
        postService.delete(savedPost.getId());

        Todo todo = new Todo();
        todo.setUserId(9001L);
        todo.setTitle("Incomplete todo");
        todo.setCompleted(false);
        Todo savedTodo = todoService.create(todo);
        assertEquals(List.of(savedTodo.getId()), todoService.findByUserIdAndCompleted(
                9001L, false, new CollectionPage(0, 10)).stream().map(result -> result.getId()).toList());
        todoService.delete(savedTodo.getId());
    }
}