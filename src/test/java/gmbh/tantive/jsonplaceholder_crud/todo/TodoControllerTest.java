package gmbh.tantive.jsonplaceholder_crud.todo;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.support.CrudControllerTestSupport;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TodoControllerTest {

    @Mock
    private TodoService todoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TodoController(todoService)).build();
    }

    @Test
    void supportsTodoCrudRoutes() throws Exception {
        CrudControllerTestSupport.assertCrudRoutes(mockMvc, "/todos", todoService, todo(1L, 4L, true),
                "{\"userId\":4,\"title\":\"Todo\",\"completed\":true}");
    }

    @Test
    void filtersTodosByUserAndCompletionState() throws Exception {
        when(todoService.findByUserIdAndCompleted(4L, true, new CollectionPage(0, 100)))
                .thenReturn(List.of(todo(1L, 4L, true)));

        mockMvc.perform(get("/todos?userId=4&completed=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].completed").value(true));

        verify(todoService).findByUserIdAndCompleted(4L, true, new CollectionPage(0, 100));
    }

    private Todo todo(Long id, Long userId, Boolean completed) {
        Todo todo = new Todo();
        todo.setId(id);
        todo.setUserId(userId);
        todo.setTitle("Todo");
        todo.setCompleted(completed);
        return todo;
    }
}