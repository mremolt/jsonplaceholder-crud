package gmbh.tantive.jsonplaceholder_crud.todo;

import java.util.List;
import java.util.ArrayList;

import tools.jackson.databind.ObjectMapper;
import gmbh.tantive.jsonplaceholder_crud.persistence.AbstractCassandraResourceService;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationReference;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.stereotype.Service;

@Service
public class CassandraTodoService extends AbstractCassandraResourceService<Todo> implements TodoService {

    public CassandraTodoService(ResourceRecordRepository repository,
            ResourceRelationRecordRepository relationRepository, ObjectMapper objectMapper) {
        super("todos", Todo.class, repository, relationRepository, objectMapper);
    }

    @Override
    public List<Todo> findByUserIdAndCompleted(Long userId, Boolean completed, CollectionPage page) {
        return findPageByUserIdAndCompleted(userId, completed, page);
    }

    @Override
    protected List<ResourceRelationReference> relationReferences(Todo todo) {
        List<ResourceRelationReference> references = new ArrayList<>();
        if (todo.getUserId() != null) {
            references.add(new ResourceRelationReference("user", todo.getUserId()));
            if (todo.getCompleted() != null) {
                references.add(new ResourceRelationReference(
                        "user-completed-" + todo.getCompleted(), todo.getUserId()));
            }
        }
        if (todo.getCompleted() != null) {
            references.add(new ResourceRelationReference("completed", todo.getCompleted() ? 1L : 0L));
        }
        return references;
    }
}