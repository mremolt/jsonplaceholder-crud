package gmbh.tantive.jsonplaceholder_crud.shared;

import java.util.List;

public interface ResourceCrudService<T extends ResourceModel> {

    List<T> findAll(CollectionPage page);

    List<T> findAfterId(long id, int limit);

    T findById(long id);

    T create(T resource);

    T replace(long id, T resource);

    T update(long id, T changes);

    void delete(long id);

    long importAll(List<T> resources);
}