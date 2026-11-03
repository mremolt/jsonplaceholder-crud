package gmbh.tantive.jsonplaceholder_crud.support;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import gmbh.tantive.jsonplaceholder_crud.post.Post;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceBulkService;

/** In-memory bulk service that keeps its resources by id and records every import batch. */
public class RecordingBulkService implements ResourceBulkService<Post> {

    private final String name;
    private final NavigableMap<Long, Post> stored = new TreeMap<>();
    private final List<List<Post>> imports = new ArrayList<>();

    public RecordingBulkService(String name, List<Post> existing) {
        this.name = name;
        existing.forEach(post -> stored.put(post.getId(), post));
    }

    public RecordingBulkService(String name) {
        this(name, List.of());
    }

    @Override
    public String resourceName() {
        return name;
    }

    @Override
    public Class<Post> resourceType() {
        return Post.class;
    }

    @Override
    public List<Post> findAfterId(long id, int limit) {
        return stored.tailMap(id, false).values().stream().limit(limit).toList();
    }

    @Override
    public long importAll(List<Post> resources) {
        imports.add(List.copyOf(resources));
        resources.forEach(post -> stored.put(post.getId(), post));
        return resources.size();
    }

    public List<List<Post>> imports() {
        return imports;
    }
}
