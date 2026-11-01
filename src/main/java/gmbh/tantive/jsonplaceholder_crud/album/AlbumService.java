package gmbh.tantive.jsonplaceholder_crud.album;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;

public interface AlbumService extends ResourceCrudService<Album> {

    List<Album> findByUserId(Long userId, CollectionPage page);
}