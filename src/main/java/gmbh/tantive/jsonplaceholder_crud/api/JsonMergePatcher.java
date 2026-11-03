package gmbh.tantive.jsonplaceholder_crud.api;

import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Applies a JSON Merge Patch (RFC 7396) to a request representation and validates the result like a full
 * PUT body: fields missing in the patch stay unchanged, {@code null} removes a field, nested objects merge.
 */
@Component
@RequiredArgsConstructor
public class JsonMergePatcher {

    public static final String MEDIA_TYPE = "application/merge-patch+json";

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public <T> T apply(T current, JsonNode patch, Class<T> type) {
        if (!patch.isObject()) {
            throw new InvalidPatchException("A merge patch must be a JSON object");
        }
        T patched;
        try {
            patched = objectMapper.treeToValue(merge(objectMapper.valueToTree(current), patch), type);
        } catch (JacksonException exception) {
            throw new InvalidPatchException("The patched resource is not readable: " + exception.getOriginalMessage());
        }
        Set<ConstraintViolation<T>> violations = validator.validate(patched);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return patched;
    }

    static JsonNode merge(JsonNode target, JsonNode patch) {
        if (!patch.isObject()) {
            return patch;
        }
        ObjectNode result = target != null && target.isObject() ? ((ObjectNode) target).deepCopy()
                : ((ObjectNode) patch).objectNode();
        for (Map.Entry<String, JsonNode> field : patch.properties()) {
            if (field.getValue().isNull()) {
                result.remove(field.getKey());
            } else {
                result.set(field.getKey(), merge(result.get(field.getKey()), field.getValue()));
            }
        }
        return result;
    }
}
