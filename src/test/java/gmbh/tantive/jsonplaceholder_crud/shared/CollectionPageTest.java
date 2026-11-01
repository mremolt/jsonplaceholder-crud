package gmbh.tantive.jsonplaceholder_crud.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CollectionPageTest {

    @Test
    void acceptsBoundedStartAndLimit() {
        CollectionPage page = new CollectionPage(25, 50);

        assertEquals(25, page.start());
        assertEquals(50, page.limit());
        assertEquals(75, page.fetchLimit());
    }

    @Test
    void rejectsNegativeStart() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new CollectionPage(-1, 50));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void rejectsLimitsOutsideConfiguredBounds() {
        assertThrows(ResponseStatusException.class, () -> new CollectionPage(0, 0));
        assertThrows(ResponseStatusException.class, () -> new CollectionPage(0, CollectionPage.MAX_LIMIT + 1));
    }
}