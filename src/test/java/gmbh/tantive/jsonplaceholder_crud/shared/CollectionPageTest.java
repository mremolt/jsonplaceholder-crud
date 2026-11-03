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

    @Test
    void acceptsAKeysetCursorAndFetchesOnlyOnePage() {
        CollectionPage page = new CollectionPage(0, 50, 120L);

        assertEquals(120L, page.after());
        assertEquals(50, page.fetchLimit());
    }

    @Test
    void rejectsCombiningOffsetAndCursor() {
        InvalidPageException exception = assertThrows(InvalidPageException.class,
                () -> new CollectionPage(10, 50, 120L));

        assertEquals("_start and _after cannot be combined", exception.getMessage());
    }

    @Test
    void rejectsNegativeCursors() {
        assertThrows(InvalidPageException.class, () -> new CollectionPage(0, 50, -1L));
    }
}
