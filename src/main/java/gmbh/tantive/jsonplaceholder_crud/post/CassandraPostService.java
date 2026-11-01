package gmbh.tantive.jsonplaceholder_crud.post;

import java.util.List;

import tools.jackson.databind.ObjectMapper;
import gmbh.tantive.jsonplaceholder_crud.persistence.AbstractCassandraResourceService;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationReference;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.stereotype.Service;

@Service
public class CassandraPostService extends AbstractCassandraResourceService<Post> implements PostService {

    public CassandraPostService(ResourceRecordRepository repository,
            ResourceRelationRecordRepository relationRepository, ObjectMapper objectMapper) {
        super("posts", Post.class, repository, relationRepository, objectMapper);
    }

    @Override
    public List<Post> findByUserId(Long userId, CollectionPage page) {
        return findPageByUserId(userId, page);
    }

    @Override
    protected List<ResourceRelationReference> relationReferences(Post post) {
        return post.getUserId() == null ? List.of() : List.of(new ResourceRelationReference("user", post.getUserId()));
    }
}