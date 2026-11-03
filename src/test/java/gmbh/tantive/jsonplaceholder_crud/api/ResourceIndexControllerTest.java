package gmbh.tantive.jsonplaceholder_crud.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceBulkService;
import gmbh.tantive.jsonplaceholder_crud.support.RecordingBulkService;
import org.junit.jupiter.api.Test;
import gmbh.tantive.jsonplaceholder_crud.support.ApiWebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@ApiWebMvcTest(ResourceIndexController.class)
@Import(ResourceIndexControllerTest.TwoResources.class)
class ResourceIndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void appliesTheProductionSecurityFilterChain() throws Exception {
        mockMvc.perform(get("/").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsTheDiscoveredResourcesInBeanOrder() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"posts\",\"users\"]", JsonCompareMode.STRICT));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TwoResources {

        @Bean
        @Order(2)
        ResourceBulkService<?> users() {
            return new RecordingBulkService("users", List.of());
        }

        @Bean
        @Order(1)
        ResourceBulkService<?> posts() {
            return new RecordingBulkService("posts", List.of());
        }
    }
}