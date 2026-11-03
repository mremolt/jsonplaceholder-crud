package gmbh.tantive.jsonplaceholder_crud.todo;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.support.CrudControllerTestSupport;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.junit.jupiter.api.Test;
import gmbh.tantive.jsonplaceholder_crud.support.ApiWebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ApiWebMvcTest(TodoController.class)
class TodoControllerTest {

    @MockitoBean
    private TodoService todoService;

    @Autowired
    private MockMvc mockMvc;

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