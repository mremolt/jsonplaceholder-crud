package gmbh.tantive.jsonplaceholder_crud.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.support.InMemoryResourceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class DefaultUserServiceTest {

    private static final CollectionPage FIRST_PAGE = new CollectionPage(0, 10);

    private final DefaultUserService service = new DefaultUserService(new InMemoryResourceStore(),
            JsonMapper.builder().build());

    @BeforeEach
    void setUp() {
        service.importAll(List.of(UserFixtures.leanne(1L), UserFixtures.leanne(2L)));
    }

    @Test
    void filtersByIdOrListsAllUsers() {
        assertThat(service.findByIdFilter(2L, FIRST_PAGE)).extracting(User::getId).containsExactly(2L);
        assertThat(service.findByIdFilter(9L, FIRST_PAGE)).isEmpty();
        assertThat(service.findByIdFilter(null, FIRST_PAGE)).extracting(User::getId).containsExactly(1L, 2L);
    }

    @Test
    void storesNestedObjectsCompletely() {
        User replacement = UserFixtures.leanne(null);
        replacement.getAddress().getGeo().setLat("1.5");
        replacement.getCompany().setName("New Company");

        service.replace(1L, replacement);

        User stored = service.findById(1L);
        assertThat(stored.getAddress().getGeo().getLat()).isEqualTo("1.5");
        assertThat(stored.getAddress().getGeo().getLng()).isEqualTo("81.1496");
        assertThat(stored.getCompany().getName()).isEqualTo("New Company");
    }
}
