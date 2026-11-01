package gmbh.tantive.jsonplaceholder_crud.photo;

import java.util.List;

import tools.jackson.databind.ObjectMapper;
import gmbh.tantive.jsonplaceholder_crud.persistence.AbstractCassandraResourceService;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationReference;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.stereotype.Service;

@Service
public class CassandraPhotoService extends AbstractCassandraResourceService<Photo> implements PhotoService {

    public CassandraPhotoService(ResourceRecordRepository repository,
            ResourceRelationRecordRepository relationRepository, ObjectMapper objectMapper) {
        super("photos", Photo.class, repository, relationRepository, objectMapper);
    }

    @Override
    public List<Photo> findByAlbumId(Long albumId, CollectionPage page) {
        return findPageByAlbumId(albumId, page);
    }

    @Override
    protected List<ResourceRelationReference> relationReferences(Photo photo) {
        return photo.getAlbumId() == null ? List.of()
                : List.of(new ResourceRelationReference("album", photo.getAlbumId()));
    }
}