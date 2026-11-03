package gmbh.tantive.jsonplaceholder_crud.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * Adjusts the generated OpenAPI document to the API contract, so generated clients get precise types:
 * <ul>
 * <li>every field of a {@code …Response} schema is required, because responses always contain all fields;</li>
 * <li>every PATCH body is a {@code …Patch} schema derived from the PUT body: same fields, none required,
 * nested objects as their own {@code …Patch} schemas (JSON Merge Patch, RFC 7396).</li>
 * </ul>
 */
@Component
public class OpenApiContractCustomizer implements OpenApiCustomizer {

    private static final String SCHEMA_REF = "#/components/schemas/";
    private static final String PATCH_SUFFIX = "Patch";
    private static final String APPLICATION_JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

    @Override
    @SuppressWarnings("rawtypes")
    public void customise(OpenAPI openApi) {
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        schemas.forEach(OpenApiContractCustomizer::requireAllResponseFields);
        openApi.getPaths().values().forEach(path -> describePatchBody(path, schemas));
        // The PATCH endpoints declare JsonNode; after the replacement above nothing references it.
        schemas.remove("JsonNode");
    }

    private static void requireAllResponseFields(String name, Schema<?> schema) {
        if (name.endsWith("Response") && schema.getProperties() != null) {
            schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
        }
    }

    @SuppressWarnings("rawtypes")
    private static void describePatchBody(PathItem path, Map<String, Schema> schemas) {
        if (path.getPatch() == null || path.getPut() == null) {
            return;
        }
        Schema<?> replaceBody = path.getPut().getRequestBody().getContent().get(APPLICATION_JSON).getSchema();
        String patchSchema = patchSchema(schemaName(replaceBody), schemas);
        Content content = new Content();
        for (String mediaType : List.of(JsonMergePatcher.MEDIA_TYPE, APPLICATION_JSON)) {
            content.addMediaType(mediaType, new MediaType().schema(new Schema<>().$ref(SCHEMA_REF + patchSchema)));
        }
        path.getPatch().getRequestBody().setContent(content);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String patchSchema(String name, Map<String, Schema> schemas) {
        String patchName = name + PATCH_SUFFIX;
        if (schemas.containsKey(patchName)) {
            return patchName;
        }
        Schema<?> original = schemas.get(name);
        Map<String, Schema> properties = new LinkedHashMap<>();
        original.getProperties().forEach((property, schema) -> properties.put(property, schema.get$ref() == null
                ? schema
                : new Schema<>().$ref(SCHEMA_REF + patchSchema(schemaName(schema), schemas))));
        Schema<Object> patch = new Schema<>();
        patch.setType(original.getType());
        patch.setTypes(original.getTypes());
        patch.setProperties((Map) properties);
        patch.setDescription("JSON Merge Patch (RFC 7396) for " + name
                + ": all fields are optional; fields missing in the patch stay unchanged.");
        schemas.put(patchName, patch);
        return patchName;
    }

    private static String schemaName(Schema<?> reference) {
        return reference.get$ref().substring(SCHEMA_REF.length());
    }
}
