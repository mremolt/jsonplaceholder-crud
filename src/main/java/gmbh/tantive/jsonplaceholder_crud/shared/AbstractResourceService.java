package gmbh.tantive.jsonplaceholder_crud.shared;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tools.jackson.databind.ObjectMapper;

/**
 * CRUD, paging and relation handling for resources stored as JSON payloads in a {@link ResourceStore}.
 * Subclasses declare the relations of their resource and expose the matching queries.
 */
public abstract class AbstractResourceService<T extends ResourceModel>
        implements ResourceCrudService<T>, ResourceBulkService<T> {

    private final String resourceName;
    private final Class<T> resourceType;
    private final ResourceStore store;
    private final ObjectMapper objectMapper;
    private long lastAssignedId;
    private boolean idSequenceInitialized;

    protected AbstractResourceService(String resourceName, Class<T> resourceType, ResourceStore store,
            ObjectMapper objectMapper) {
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.store = store;
        this.objectMapper = objectMapper;
    }

    @Override
    public String resourceName() {
        return resourceName;
    }

    @Override
    public Class<T> resourceType() {
        return resourceType;
    }

    @Override
    public List<T> findAll(CollectionPage page) {
        List<String> payloads = page.after() == null ? store.findPayloads(resourceName, page.fetchLimit())
                : store.findPayloadsAfterId(resourceName, page.after(), page.limit());
        return page(payloads, page);
    }

    @Override
    public List<T> findAfterId(long id, int limit) {
        return store.findPayloadsAfterId(resourceName, id, limit).stream().map(this::deserialize).toList();
    }

    @Override
    public T findById(long id) {
        return store.findPayload(resourceName, id)
                .map(this::deserialize)
                .orElseThrow(() -> new ResourceNotFoundException(resourceName, id));
    }

    @Override
    public T create(T resource) {
        resource.setId(nextId());
        return persist(resource, List.of());
    }

    @Override
    public T replace(long id, T resource) {
        T previous = findById(id);
        resource.setId(id);
        return persist(resource, relationReferences(previous));
    }

    @Override
    public void delete(long id) {
        store.delete(toStored(findById(id)));
    }

    @Override
    public long importAll(List<T> resources) {
        for (T resource : resources) {
            if (resource.getId() == null || resource.getId() < 1L) {
                throw new IllegalArgumentException("Imported records must have a positive numeric id");
            }
        }
        List<StoredResource> stored = resources.stream().map(this::toStored).toList();
        synchronized (this) {
            initializeIdSequence();
            store.saveAll(stored);
            for (StoredResource resource : stored) {
                lastAssignedId = Math.max(lastAssignedId, resource.id());
            }
        }
        return stored.size();
    }

    private synchronized long nextId() {
        initializeIdSequence();
        return ++lastAssignedId;
    }

    private void initializeIdSequence() {
        if (!idSequenceInitialized) {
            lastAssignedId = store.findHighestId(resourceName);
            idSequenceInitialized = true;
        }
    }

    /** Lists the resources related to the parent, or all resources if no parent is given. */
    protected List<T> findRelatedOrAll(Relationship relationship, Long parentId, CollectionPage page) {
        return parentId == null ? findAll(page) : findRelated(relationship, parentId, page);
    }

    protected List<T> findRelated(Relationship relationship, long parentId, CollectionPage page) {
        List<String> payloads = page.after() == null
                ? store.findRelatedPayloads(relationship, parentId, resourceName, page.fetchLimit())
                : store.findRelatedPayloadsAfterId(relationship, parentId, resourceName, page.after(), page.limit());
        return page(payloads, page);
    }

    protected List<ResourceRelationReference> relationReferences(T resource) {
        return List.of();
    }

    protected List<T> findPageByIdFilter(Long id, CollectionPage page) {
        if (id == null) {
            return findAll(page);
        }
        if (page.after() != null && id <= page.after()) {
            return List.of();
        }
        return store.findPayload(resourceName, id).map(this::deserialize).map(List::of).orElseGet(List::of);
    }

    private List<T> page(List<String> payloads, CollectionPage page) {
        return payloads.stream()
                .skip(page.start())
                .limit(page.limit())
                .map(this::deserialize)
                .toList();
    }

    private T persist(T resource, List<ResourceRelationReference> previousRelations) {
        StoredResource stored = toStored(resource);
        Set<ResourceRelationReference> current = new HashSet<>(stored.relations());
        List<ResourceRelationReference> obsoleteRelations = previousRelations.stream()
                .filter(reference -> !current.contains(reference))
                .toList();
        store.save(stored, obsoleteRelations);
        return resource;
    }

    private StoredResource toStored(T resource) {
        return new StoredResource(resourceName, resource.getId(), serialize(resource), relationReferences(resource));
    }

    private String serialize(T resource) {
        try {
            return objectMapper.writeValueAsString(resource);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize resource", exception);
        }
    }

    private T deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, resourceType);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read stored resource", exception);
        }
    }
}
