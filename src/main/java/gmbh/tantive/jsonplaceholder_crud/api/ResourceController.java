package gmbh.tantive.jsonplaceholder_crud.api;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import tools.jackson.databind.JsonNode;

/**
 * Generic CRUD endpoints of a resource. The API speaks request records ({@code Q}, validated, without id) and
 * response records ({@code R}); the service and the storage only see the model ({@code M}).
 */
public abstract class ResourceController<M extends ResourceModel, Q, R> {

    private final ResourceCrudService<M> service;
    private final Class<Q> requestType;
    private final JsonMergePatcher patcher;

    protected ResourceController(ResourceCrudService<M> service, Class<Q> requestType, JsonMergePatcher patcher) {
        this.service = service;
        this.requestType = requestType;
        this.patcher = patcher;
    }

    protected abstract M toModel(Q request);

    protected abstract Q toRequest(M model);

    protected abstract R toResponse(M model);

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get by ID")
    public R findById(@PathVariable long id) {
        return toResponse(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create")
    public R create(@Valid @RequestBody Q request) {
        return toResponse(service.create(toModel(request)));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Replace")
    public R replace(@PathVariable long id, @Valid @RequestBody Q request) {
        return toResponse(service.replace(id, toModel(request)));
    }

    @PatchMapping(path = "/{id:\\d+}", consumes = { JsonMergePatcher.MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE })
    @Operation(summary = "Update with a JSON Merge Patch (RFC 7396); null removes a field, "
            + "the result is validated like a full replace")
    public R update(@PathVariable long id, @RequestBody JsonNode patch) {
        Q patched = patcher.apply(toRequest(service.findById(id)), patch, requestType);
        return toResponse(service.replace(id, toModel(patched)));
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Delete")
    public Map<String, Object> delete(@PathVariable long id) {
        service.delete(id);
        return Map.of();
    }
}
