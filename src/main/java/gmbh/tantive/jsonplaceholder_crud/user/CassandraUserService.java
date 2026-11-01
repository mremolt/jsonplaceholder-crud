package gmbh.tantive.jsonplaceholder_crud.user;

import java.util.List;

import tools.jackson.databind.ObjectMapper;
import gmbh.tantive.jsonplaceholder_crud.persistence.AbstractCassandraResourceService;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.stereotype.Service;

@Service
public class CassandraUserService extends AbstractCassandraResourceService<User> implements UserService {

    public CassandraUserService(ResourceRecordRepository repository,
            ResourceRelationRecordRepository relationRepository, ObjectMapper objectMapper) {
        super("users", User.class, repository, relationRepository, objectMapper);
    }

    @Override
    public List<User> findByIdFilter(Long id, CollectionPage page) {
        return findPageByIdFilter(id, page);
    }
}