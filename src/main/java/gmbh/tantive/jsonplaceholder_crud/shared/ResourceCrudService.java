package gmbh.tantive.jsonplaceholder_crud.shared;

import java.util.List;

/** Operations the REST API offers for every resource. */
public interface ResourceCrudService<T extends ResourceModel> {

    List<T> findAll(CollectionPage page);

    T findById(long id);

    T create(T resource);

    T replace(long id, T resource);

    void delete(long id);
}
