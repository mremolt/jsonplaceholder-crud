package gmbh.tantive.jsonplaceholder_crud.importer;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.album.AlbumService;
import gmbh.tantive.jsonplaceholder_crud.comment.CommentService;
import gmbh.tantive.jsonplaceholder_crud.photo.PhotoService;
import gmbh.tantive.jsonplaceholder_crud.post.PostService;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;
import gmbh.tantive.jsonplaceholder_crud.todo.TodoService;
import gmbh.tantive.jsonplaceholder_crud.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "jsonplaceholder.relations.backfill.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ResourceRelationBackfill implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRelationBackfill.class);
    private static final int PAGE_SIZE = 500;

    private final PostService postService;
    private final CommentService commentService;
    private final AlbumService albumService;
    private final PhotoService photoService;
    private final TodoService todoService;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        backfill("posts", postService);
        backfill("comments", commentService);
        backfill("albums", albumService);
        backfill("photos", photoService);
        backfill("todos", todoService);
        backfill("users", userService);
    }

    private <T extends ResourceModel> void backfill(String resource, ResourceCrudService<T> service) {
        long afterId = 0L;
        long total = 0L;
        while (true) {
            List<T> page = service.findAfterId(afterId, PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            service.importAll(page);
            total += page.size();
            afterId = page.getLast().getId();
        }
        LOGGER.info("Backfilled {} {} relation rows", total, resource);
    }
}