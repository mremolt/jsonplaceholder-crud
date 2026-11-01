package gmbh.tantive.jsonplaceholder_crud.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public final class CrudControllerTestSupport {

    private CrudControllerTestSupport() {
    }

    public static <T extends ResourceModel> void assertCrudRoutes(MockMvc mockMvc, String path,
            ResourceCrudService<T> service, T resource, String validBody) throws Exception {
        when(service.findById(1L)).thenReturn(resource);
        when(service.create(any())).thenReturn(resource);
        when(service.replace(eq(1L), any())).thenReturn(resource);
        when(service.update(eq(1L), any())).thenReturn(resource);

        mockMvc.perform(get(path + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(put(path + "/1").contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(patch(path + "/1").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(delete(path + "/1"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));

        verify(service).findById(1L);
        verify(service).create(any());
        verify(service).replace(eq(1L), any());
        verify(service).update(eq(1L), any());
        verify(service).delete(1L);
    }
}