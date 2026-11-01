package gmbh.tantive.jsonplaceholder_crud.comment;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;

public interface CommentService extends ResourceCrudService<Comment> {

    List<Comment> findByPostId(Long postId, CollectionPage page);
}