package gmbh.tantive.jsonplaceholder_crud.user;

import java.util.List;

import tools.jackson.databind.ObjectMapper;
import gmbh.tantive.jsonplaceholder_crud.shared.AbstractResourceService;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceStore;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(6)
public class DefaultUserService extends AbstractResourceService<User> implements UserService {

    public DefaultUserService(ResourceStore store, ObjectMapper objectMapper) {
        super("users", User.class, store, objectMapper);
    }

    @Override
    public List<User> findByIdFilter(Long id, CollectionPage page) {
        return findPageByIdFilter(id, page);
    }
}
