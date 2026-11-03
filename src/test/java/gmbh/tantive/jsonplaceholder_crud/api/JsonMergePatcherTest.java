package gmbh.tantive.jsonplaceholder_crud.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import gmbh.tantive.jsonplaceholder_crud.post.PostRequest;
import gmbh.tantive.jsonplaceholder_crud.user.UserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class JsonMergePatcherTest {

    private final ObjectMapper json = JsonMapper.builder().build();
    private final JsonMergePatcher patcher = new JsonMergePatcher(json,
            Validation.buildDefaultValidatorFactory().getValidator());

    private final PostRequest post = new PostRequest(7L, "Title", "Body");
    private final UserRequest user = new UserRequest("Leanne Graham", "Bret", "sincere@april.biz",
            new UserRequest.Address("Kulas Light", "Apt. 1", "Gwenborough", "92998-3874",
                    new UserRequest.Geo("-37.3159", "81.1496")),
            "1-770-736-8031", "hildegard.org",
            new UserRequest.Company("Romaguera-Crona", "Multi-layered", "harness"));

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource(delimiter = '|', value = {
            "{\"a\":\"b\"}            | {\"a\":\"c\"}             | {\"a\":\"c\"}",
            "{\"a\":\"b\"}            | {\"b\":\"c\"}             | {\"a\":\"b\",\"b\":\"c\"}",
            "{\"a\":\"b\"}            | {\"a\":null}              | {}",
            "{\"a\":\"b\",\"b\":\"c\"} | {\"a\":null}              | {\"b\":\"c\"}",
            "{\"a\":[\"b\"]}          | {\"a\":\"c\"}             | {\"a\":\"c\"}",
            "{\"a\":{\"b\":\"c\"}}     | {\"a\":{\"b\":\"d\",\"c\":null}} | {\"a\":{\"b\":\"d\"}}",
            "{\"a\":[{\"b\":\"c\"}]}  | {\"a\":[1]}               | {\"a\":[1]}",
            "{\"e\":null}             | {\"a\":1}                 | {\"e\":null,\"a\":1}",
            "{}                       | {\"a\":{\"bb\":{\"ccc\":null}}} | {\"a\":{\"bb\":{}}}" })
    void mergesLikeTheRfc7396Examples(String target, String patch, String expected) {
        assertThat(JsonMergePatcher.merge(tree(target), tree(patch))).isEqualTo(tree(expected));
    }

    @Test
    void changesOnlyTheFieldsInThePatch() {
        PostRequest patched = patcher.apply(post, tree("{\"title\":\"Patched\"}"), PostRequest.class);

        assertThat(patched).isEqualTo(new PostRequest(7L, "Patched", "Body"));
    }

    @Test
    void nullRemovesAFieldSoRequiredFieldsFailValidation() {
        assertThatThrownBy(() -> patcher.apply(post, tree("{\"title\":null}"), PostRequest.class))
                .isInstanceOfSatisfying(ConstraintViolationException.class, exception -> assertThat(
                        exception.getConstraintViolations()).extracting(violation -> violation.getPropertyPath()
                        .toString()).containsExactly("title"));
    }

    @Test
    void mergesNestedObjectsFieldByField() {
        UserRequest patched = patcher.apply(user,
                tree("{\"address\":{\"street\":\"New Street\",\"geo\":{\"lat\":\"1.5\"}}}"), UserRequest.class);

        assertThat(patched.address()).isEqualTo(new UserRequest.Address("New Street", "Apt. 1", "Gwenborough",
                "92998-3874", new UserRequest.Geo("1.5", "81.1496")));
        assertThat(patched.company()).isEqualTo(user.company());
    }

    @Test
    void validatesNestedValuesOfThePatchedResult() {
        assertThatThrownBy(() -> patcher.apply(user, tree("{\"address\":{\"city\":\" \"},\"email\":\"no-mail\"}"),
                UserRequest.class))
                .isInstanceOfSatisfying(ConstraintViolationException.class, exception -> assertThat(
                        exception.getConstraintViolations()).extracting(violation -> violation.getPropertyPath()
                        .toString()).containsExactlyInAnyOrder("address.city", "email"));
    }

    @Test
    void rejectsPatchesThatAreNoObject() {
        assertThatThrownBy(() -> patcher.apply(post, tree("[]"), PostRequest.class))
                .isInstanceOf(InvalidPatchException.class)
                .hasMessage("A merge patch must be a JSON object");
    }

    @Test
    void rejectsValuesOfTheWrongType() {
        assertThatThrownBy(() -> patcher.apply(post, tree("{\"userId\":\"seven\"}"), PostRequest.class))
                .isInstanceOf(InvalidPatchException.class)
                .hasMessageStartingWith("The patched resource is not readable");
    }

    private JsonNode tree(String content) {
        return json.readTree(content);
    }
}
