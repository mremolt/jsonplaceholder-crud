package gmbh.tantive.jsonplaceholder_crud.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Checks the five generic CRUD routes of a resource controller: request bodies must reach the service with
 * every field bound, responses must contain the complete resource returned by the service, and an empty
 * merge patch must replace the resource with an unchanged copy.
 */
public final class CrudControllerTestSupport {

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private CrudControllerTestSupport() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends ResourceModel> void assertCrudRoutes(MockMvc mockMvc, String path,
            ResourceCrudService<T> service, T resource, String validBody) throws Exception {
        String resourceJson = JSON.writeValueAsString(resource);
        when(service.findById(1L)).thenReturn(resource);
        when(service.create(any())).thenReturn(resource);
        when(service.replace(eq(1L), any())).thenReturn(resource);

        mockMvc.perform(get(path + "/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(resourceJson, JsonCompareMode.STRICT));
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isCreated())
                .andExpect(content().json(resourceJson, JsonCompareMode.STRICT));
        mockMvc.perform(put(path + "/1").contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isOk())
                .andExpect(content().json(resourceJson, JsonCompareMode.STRICT));
        mockMvc.perform(patch(path + "/1").contentType("application/merge-patch+json").content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().json(resourceJson, JsonCompareMode.STRICT));
        mockMvc.perform(delete(path + "/1"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}", JsonCompareMode.STRICT));

        Class<T> type = (Class<T>) resource.getClass();
        ArgumentCaptor<T> created = ArgumentCaptor.forClass(type);
        verify(service).create(created.capture());
        assertBound(JSON.readTree(validBody), JSON.valueToTree(created.getValue()), "");
        ArgumentCaptor<T> replaced = ArgumentCaptor.forClass(type);
        verify(service, times(2)).replace(eq(1L), replaced.capture());
        List<T> replacements = replaced.getAllValues();
        assertBound(JSON.readTree(validBody), JSON.valueToTree(replacements.get(0)), "");
        JsonNode unchanged = JSON.valueToTree(resource);
        ((ObjectNode) unchanged).remove("id");
        assertBound(unchanged, JSON.valueToTree(replacements.get(1)), "");
        verify(service, times(2)).findById(1L);
        verify(service).delete(1L);
    }

    private static void assertBound(JsonNode expected, JsonNode actual, String path) {
        if (expected.isObject()) {
            for (Map.Entry<String, JsonNode> field : expected.properties()) {
                assertBound(field.getValue(), actual.path(field.getKey()), path + "/" + field.getKey());
            }
            return;
        }
        assertThat(actual.asString()).as("bound value of %s", path).isEqualTo(expected.asString());
    }
}
