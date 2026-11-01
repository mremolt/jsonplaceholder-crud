package gmbh.tantive.jsonplaceholder_crud.post;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;

public interface PostService extends ResourceCrudService<Post> {

    List<Post> findByUserId(Long userId, CollectionPage page);
}