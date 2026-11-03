package gmbh.tantive.jsonplaceholder_crud.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

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
        InvalidPageException exception = assertThrows(InvalidPageException.class,
                () -> new CollectionPage(-1, 50));

        assertEquals("_start must be between 0 and 10000", exception.getMessage());
    }

    @Test
    void rejectsLimitsOutsideConfiguredBounds() {
        assertThrows(InvalidPageException.class, () -> new CollectionPage(0, 0));
        assertThrows(InvalidPageException.class, () -> new CollectionPage(0, CollectionPage.MAX_LIMIT + 1));
    }
}