package gmbh.tantive.jsonplaceholder_crud.todo;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;

public interface TodoService extends ResourceCrudService<Todo> {

    List<Todo> findByUserIdAndCompleted(Long userId, Boolean completed, CollectionPage page);
}