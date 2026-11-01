package gmbh.tantive.jsonplaceholder_crud.comment;

import java.util.List;

import tools.jackson.databind.ObjectMapper;
import gmbh.tantive.jsonplaceholder_crud.persistence.AbstractCassandraResourceService;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationReference;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.stereotype.Service;

@Service
public class CassandraCommentService extends AbstractCassandraResourceService<Comment> implements CommentService {

    public CassandraCommentService(ResourceRecordRepository repository,
            ResourceRelationRecordRepository relationRepository, ObjectMapper objectMapper) {
        super("comments", Comment.class, repository, relationRepository, objectMapper);
    }

    @Override
    public List<Comment> findByPostId(Long postId, CollectionPage page) {
        return findPageByPostId(postId, page);
    }

    @Override
    protected List<ResourceRelationReference> relationReferences(Comment comment) {
        return comment.getPostId() == null ? List.of()
                : List.of(new ResourceRelationReference("post", comment.getPostId()));
    }
}