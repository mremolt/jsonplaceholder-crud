package gmbh.tantive.jsonplaceholder_crud.persistence;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public abstract class AbstractCassandraResourceService<T extends ResourceModel> implements ResourceCrudService<T> {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final String resourceName;
    private final Class<T> resourceType;
    private final ResourceRecordRepository repository;
    private final ResourceRelationRecordRepository relationRepository;
    private final ObjectMapper objectMapper;
    private long lastAssignedId;
    private boolean idSequenceInitialized;

    protected AbstractCassandraResourceService(String resourceName, Class<T> resourceType,
            ResourceRecordRepository repository, ResourceRelationRecordRepository relationRepository,
            ObjectMapper objectMapper) {
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.repository = repository;
        this.relationRepository = relationRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<T> findAll(CollectionPage page) {
        List<ResourceRecord> records = repository.findPageByResource(resourceName, page.fetchLimit());
        return pageRecords(records, page);
    }

    @Override
    public List<T> findAfterId(long id, int limit) {
        return repository.findPageAfterId(resourceName, id, limit).stream()
                .map(this::deserialize)
                .toList();
    }

    @Override
    public T findById(long id) {
        return repository.findById(new ResourceRecordKey(resourceName, id))
                .map(this::deserialize)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
    public T update(long id, T changes) {
        T resource = findById(id);
        List<ResourceRelationReference> previousRelations = relationReferences(resource);
        BeanWrapper source = new BeanWrapperImpl(changes);
        String[] nullProperties = Arrays.stream(source.getPropertyDescriptors())
                .map(descriptor -> descriptor.getName())
                .filter(name -> !"class".equals(name) && source.getPropertyValue(name) == null)
                .toArray(String[]::new);
        BeanUtils.copyProperties(changes, resource, nullProperties);
        resource.setId(id);
        return persist(resource, previousRelations);
    }

    @Override
    public void delete(long id) {
        T resource = findById(id);
        repository.deleteById(new ResourceRecordKey(resourceName, id));
        deleteRelations(resource);
    }

    @Override
    public long importAll(List<T> resources) {
        List<ResourceRecord> records = new ArrayList<>(resources.size());
        List<ResourceRelationRecord> relationRecords = new ArrayList<>();
        for (T resource : resources) {
            if (resource.getId() == null || resource.getId() < 1L) {
                throw new IllegalArgumentException("Imported records must have a positive numeric id");
            }
            ResourceRecord record = toRecord(resource);
            records.add(record);
            relationRecords.addAll(toRelationRecords(resource, record.getPayload()));
        }
        synchronized (this) {
            initializeIdSequence();
            repository.saveAll(records);
            if (!relationRecords.isEmpty()) {
                relationRepository.saveAll(relationRecords);
            }
            for (T resource : resources) {
                Long importedId = resource.getId();
                if (importedId != null) {
                    lastAssignedId = Math.max(lastAssignedId, importedId);
                }
            }
        }
        return records.size();
    }

    private synchronized long nextId() {
        initializeIdSequence();
        return ++lastAssignedId;
    }

    private void initializeIdSequence() {
        if (!idSequenceInitialized) {
            lastAssignedId = repository.findLatestByResource(resourceName)
                    .map(record -> record.getKey().getId())
                    .orElse(0L);
            idSequenceInitialized = true;
        }
    }

    protected List<T> findPageByUserId(Long userId, CollectionPage page) {
        if (userId == null) {
            return findAll(page);
        }
        return findRelated("user", userId, page);
    }

    protected List<T> findPageByPostId(Long postId, CollectionPage page) {
        if (postId == null) {
            return findAll(page);
        }
        return findRelated("post", postId, page);
    }

    protected List<T> findPageByAlbumId(Long albumId, CollectionPage page) {
        if (albumId == null) {
            return findAll(page);
        }
        return findRelated("album", albumId, page);
    }

    protected List<T> findPageByUserIdAndCompleted(Long userId, Boolean completed, CollectionPage page) {
        if (userId != null && completed != null) {
            return findRelated("user-completed-" + completed, userId, page);
        }
        if (userId != null) {
            return findPageByUserId(userId, page);
        }
        if (completed != null) {
            return findRelated("completed", completed ? 1L : 0L, page);
        }
        return findAll(page);
    }

    protected List<T> findRelated(String relationship, Long parentId, CollectionPage page) {
        return relationRepository.findPage(relationship, parentId, resourceName, page.fetchLimit()).stream()
                .map(record -> record.getPayload())
                .map(this::deserializePayload)
                .skip(page.start())
                .limit(page.limit())
                .toList();
    }

    protected List<ResourceRelationReference> relationReferences(T resource) {
        return List.of();
    }

    protected List<T> findPageByIdFilter(Long id, CollectionPage page) {
        if (id == null) {
            return findAll(page);
        }
        return repository.findById(new ResourceRecordKey(resourceName, id))
                .map(this::deserialize)
                .map(List::of)
                .orElseGet(List::of);
    }

    private List<T> pageRecords(List<ResourceRecord> records, CollectionPage page) {
        return records.stream()
                .sorted(Comparator.comparingLong(record -> record.getKey().getId()))
                .map(this::deserialize)
                .skip(page.start())
                .limit(page.limit())
                .toList();
    }

    private T persist(T resource, List<ResourceRelationReference> previousRelations) {
        ResourceRecord record = toRecord(resource);
        repository.save(record);
        List<ResourceRelationRecord> relations = toRelationRecords(resource, record.getPayload());
        if (!relations.isEmpty()) {
            relationRepository.saveAll(relations);
        }
        Set<ResourceRelationReference> current = new HashSet<>(relationReferences(resource));
        for (ResourceRelationReference oldReference : previousRelations) {
            if (!current.contains(oldReference)) {
                relationRepository.deleteById(relationKey(oldReference, resource.getId()));
            }
        }
        return resource;
    }

    private void deleteRelations(T resource) {
        for (ResourceRelationReference reference : relationReferences(resource)) {
            relationRepository.deleteById(relationKey(reference, resource.getId()));
        }
    }

    private List<ResourceRelationRecord> toRelationRecords(T resource, String payload) {
        return relationReferences(resource).stream()
                .map(reference -> new ResourceRelationRecord(relationKey(reference, resource.getId()), payload))
                .toList();
    }

    private ResourceRelationKey relationKey(ResourceRelationReference reference, Long id) {
        return new ResourceRelationKey(reference.relationship(), reference.parentId(), resourceName, id);
    }

    private ResourceRecord toRecord(T resource) {
        try {
            String payload = objectMapper.writeValueAsString(resource);
            Map<String, Object> values = objectMapper.readValue(payload, MAP_TYPE);
            return new ResourceRecord(new ResourceRecordKey(resourceName, resource.getId()), payload,
                    number(values.get("userId")), number(values.get("postId")), number(values.get("albumId")),
                    booleanValue(values.get("completed")));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize resource", exception);
        }
    }

    private T deserialize(ResourceRecord record) {
        return deserializePayload(record.getPayload());
    }

    private T deserializePayload(String payload) {
        try {
            return objectMapper.readValue(payload, resourceType);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read stored resource", exception);
        }
    }

    private Long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private Boolean booleanValue(Object value) {
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }
}