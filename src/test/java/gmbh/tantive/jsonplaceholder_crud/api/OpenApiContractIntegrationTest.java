package gmbh.tantive.jsonplaceholder_crud.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gmbh.tantive.jsonplaceholder_crud.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Checks the published OpenAPI document against the API contract the frontend generates its types from.
 * The rules are generic, so new resources are covered without changing this test.
 */
@IntegrationTest
class OpenApiContractIntegrationTest {

    private static final String SCHEMA_PREFIX = "#/components/schemas/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode spec;

    @BeforeEach
    void loadSpec() throws Exception {
        spec = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse()
                .getContentAsString());
    }

    @Test
    void documentsInheritedCrudOperationsWithConcreteResourceSchemas() {
        assertThat(ref(spec.at("/paths/~1posts~1{id}/get/responses/200/content/application~1json/schema")))
                .isEqualTo("PostResponse");
        assertThat(ref(spec.at("/paths/~1users~1{id}/put/requestBody/content/application~1json/schema")))
                .isEqualTo("UserRequest");
        assertThat(ref(spec.at("/paths/~1todos/post/requestBody/content/application~1json/schema")))
                .isEqualTo("TodoRequest");
        assertThat(spec.at("/paths/~1posts/get/responses/200/content/application~1json/schema/items/$ref")
                .asString()).isEqualTo(SCHEMA_PREFIX + "PostResponse");
    }

    @Test
    void declaresEveryFieldOfEveryResponseAsRequired() {
        List<String> responseSchemas = new ArrayList<>();
        for (Map.Entry<String, JsonNode> schema : schemas().properties()) {
            if (!schema.getKey().endsWith("Response")) {
                continue;
            }
            responseSchemas.add(schema.getKey());
            assertThat(names(schema.getValue().path("required")))
                    .as("required fields of %s", schema.getKey())
                    .containsExactlyInAnyOrderElementsOf(schema.getValue().path("properties").propertyNames());
        }
        assertThat(responseSchemas).contains("PostResponse", "UserResponse", "TodoResponse", "AuthTokenResponse",
                "AuthAccountResponse");
    }

    @Test
    void describesEveryPatchBodyAsAnOptionalCopyOfTheReplaceBody() {
        List<String> patchedPaths = new ArrayList<>();
        for (Map.Entry<String, JsonNode> path : spec.path("paths").properties()) {
            JsonNode patch = path.getValue().path("patch");
            if (patch.isMissingNode()) {
                continue;
            }
            patchedPaths.add(path.getKey());
            String requestSchema = ref(path.getValue().at("/put/requestBody/content/application~1json/schema"));
            JsonNode content = patch.at("/requestBody/content");
            assertThat(content.propertyNames()).as(path.getKey())
                    .containsExactlyInAnyOrder("application/merge-patch+json", "application/json");
            for (JsonNode mediaType : content) {
                assertThat(ref(mediaType.path("schema"))).as(path.getKey()).isEqualTo(requestSchema + "Patch");
            }
            assertOptionalCopy(requestSchema);
        }
        assertThat(patchedPaths).hasSize(6);
        assertThat(schemas().has("JsonNode")).isFalse();
        assertThat(ref(schemas().at("/UserRequestPatch/properties/address"))).isEqualTo("AddressPatch");
        assertThat(ref(schemas().at("/AddressPatch/properties/geo"))).isEqualTo("GeoPatch");
    }

    private void assertOptionalCopy(String schemaName) {
        JsonNode original = schemas().path(schemaName);
        JsonNode patch = schemas().path(schemaName + "Patch");
        assertThat(patch.has("required")).as("%sPatch has no required fields", schemaName).isFalse();
        assertThat(patch.path("properties").propertyNames()).as(schemaName + "Patch")
                .containsExactlyInAnyOrderElementsOf(original.path("properties").propertyNames());
        for (Map.Entry<String, JsonNode> property : original.path("properties").properties()) {
            if (property.getValue().has("$ref")) {
                String nested = ref(property.getValue());
                assertThat(ref(patch.path("properties").path(property.getKey()))).isEqualTo(nested + "Patch");
                assertOptionalCopy(nested);
            }
        }
    }

    private JsonNode schemas() {
        return spec.path("components").path("schemas");
    }

    private static String ref(JsonNode schema) {
        return schema.path("$ref").asString().replace(SCHEMA_PREFIX, "");
    }

    private static List<String> names(JsonNode array) {
        List<String> names = new ArrayList<>();
        array.forEach(name -> names.add(name.asString()));
        return names;
    }
}
