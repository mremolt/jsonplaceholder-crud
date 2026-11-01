package gmbh.tantive.jsonplaceholder_crud.user;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;

public interface UserService extends ResourceCrudService<User> {

    List<User> findByIdFilter(Long id, CollectionPage page);
}