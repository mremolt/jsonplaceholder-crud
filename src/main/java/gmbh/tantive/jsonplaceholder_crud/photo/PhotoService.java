package gmbh.tantive.jsonplaceholder_crud.photo;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;

public interface PhotoService extends ResourceCrudService<Photo> {

    List<Photo> findByAlbumId(Long albumId, CollectionPage page);
}