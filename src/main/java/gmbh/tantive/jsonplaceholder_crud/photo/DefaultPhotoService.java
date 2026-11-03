package gmbh.tantive.jsonplaceholder_crud.photo;

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
@Order(4)
public class DefaultPhotoService extends AbstractResourceService<Photo> implements PhotoService {

    public DefaultPhotoService(ResourceStore store, ObjectMapper objectMapper) {
        super("photos", Photo.class, store, objectMapper);
    }

    @Override
    public List<Photo> findByAlbumId(Long albumId, CollectionPage page) {
        return findRelatedOrAll(Relationship.ALBUM, albumId, page);
    }

    @Override
    protected List<ResourceRelationReference> relationReferences(Photo photo) {
        return photo.getAlbumId() == null ? List.of()
                : List.of(new ResourceRelationReference(Relationship.ALBUM, photo.getAlbumId()));
    }
}
