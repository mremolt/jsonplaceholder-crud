package gmbh.tantive.jsonplaceholder_crud.comment;

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
@Order(2)
public class DefaultCommentService extends AbstractResourceService<Comment> implements CommentService {

    public DefaultCommentService(ResourceStore store, ObjectMapper objectMapper) {
        super("comments", Comment.class, store, objectMapper);
    }

    @Override
    public List<Comment> findByPostId(Long postId, CollectionPage page) {
        return findRelatedOrAll(Relationship.POST, postId, page);
    }

    @Override
    protected List<ResourceRelationReference> relationReferences(Comment comment) {
        return comment.getPostId() == null ? List.of()
                : List.of(new ResourceRelationReference(Relationship.POST, comment.getPostId()));
    }
}
