package gmbh.tantive.jsonplaceholder_crud.post;

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
@Order(1)
public class DefaultPostService extends AbstractResourceService<Post> implements PostService {

    public DefaultPostService(ResourceStore store, ObjectMapper objectMapper) {
        super("posts", Post.class, store, objectMapper);
    }

    @Override
    public List<Post> findByUserId(Long userId, CollectionPage page) {
        return findRelatedOrAll(Relationship.USER, userId, page);
    }

    @Override
    protected List<ResourceRelationReference> relationReferences(Post post) {
        return post.getUserId() == null ? List.of()
                : List.of(new ResourceRelationReference(Relationship.USER, post.getUserId()));
    }
}
