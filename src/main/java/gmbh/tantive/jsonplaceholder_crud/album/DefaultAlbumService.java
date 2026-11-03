package gmbh.tantive.jsonplaceholder_crud.album;

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
@Order(3)
public class DefaultAlbumService extends AbstractResourceService<Album> implements AlbumService {

    public DefaultAlbumService(ResourceStore store, ObjectMapper objectMapper) {
        super("albums", Album.class, store, objectMapper);
    }

    @Override
    public List<Album> findByUserId(Long userId, CollectionPage page) {
        return findRelatedOrAll(Relationship.USER, userId, page);
    }

    @Override
    protected List<ResourceRelationReference> relationReferences(Album album) {
        return album.getUserId() == null ? List.of()
                : List.of(new ResourceRelationReference(Relationship.USER, album.getUserId()));
    }
}
