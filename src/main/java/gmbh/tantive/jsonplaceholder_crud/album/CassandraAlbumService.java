package gmbh.tantive.jsonplaceholder_crud.album;

import java.util.List;

import tools.jackson.databind.ObjectMapper;
import gmbh.tantive.jsonplaceholder_crud.persistence.AbstractCassandraResourceService;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationReference;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.stereotype.Service;

@Service
public class CassandraAlbumService extends AbstractCassandraResourceService<Album> implements AlbumService {

    public CassandraAlbumService(ResourceRecordRepository repository,
            ResourceRelationRecordRepository relationRepository, ObjectMapper objectMapper) {
        super("albums", Album.class, repository, relationRepository, objectMapper);
    }

    @Override
    public List<Album> findByUserId(Long userId, CollectionPage page) {
        return findPageByUserId(userId, page);
    }

    @Override
    protected List<ResourceRelationReference> relationReferences(Album album) {
        return album.getUserId() == null ? List.of()
                : List.of(new ResourceRelationReference("user", album.getUserId()));
    }
}