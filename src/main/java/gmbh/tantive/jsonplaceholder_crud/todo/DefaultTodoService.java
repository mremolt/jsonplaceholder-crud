package gmbh.tantive.jsonplaceholder_crud.todo;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.ObjectMapper;
import gmbh.tantive.jsonplaceholder_crud.shared.AbstractResourceService;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.Relationship;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceRelationReference;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceStore;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(5)
public class DefaultTodoService extends AbstractResourceService<Todo> implements TodoService {

    public DefaultTodoService(ResourceStore store, ObjectMapper objectMapper) {
        super("todos", Todo.class, store, objectMapper);
    }

    @Override
    public List<Todo> findByUserIdAndCompleted(Long userId, Boolean completed, CollectionPage page) {
        if (userId != null && completed != null) {
            return findRelated(Relationship.userCompletion(completed), userId, page);
        }
        if (completed != null) {
            return findRelated(Relationship.COMPLETED, completionKey(completed), page);
        }
        return findRelatedOrAll(Relationship.USER, userId, page);
    }

    @Override
    protected List<ResourceRelationReference> relationReferences(Todo todo) {
        List<ResourceRelationReference> references = new ArrayList<>();
        if (todo.getUserId() != null) {
            references.add(new ResourceRelationReference(Relationship.USER, todo.getUserId()));
            if (todo.getCompleted() != null) {
                references.add(new ResourceRelationReference(
                        Relationship.userCompletion(todo.getCompleted()), todo.getUserId()));
            }
        }
        if (todo.getCompleted() != null) {
            references.add(new ResourceRelationReference(Relationship.COMPLETED, completionKey(todo.getCompleted())));
        }
        return references;
    }

    private static long completionKey(boolean completed) {
        return completed ? 1L : 0L;
    }
}
