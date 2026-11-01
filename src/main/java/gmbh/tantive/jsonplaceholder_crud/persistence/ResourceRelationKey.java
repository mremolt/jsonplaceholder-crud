package gmbh.tantive.jsonplaceholder_crud.persistence;

import java.io.Serializable;
import java.util.Objects;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

@PrimaryKeyClass
public class ResourceRelationKey implements Serializable {

    @PrimaryKeyColumn(name = "relationship", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String relationship;

    @PrimaryKeyColumn(name = "parent_id", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    private Long parentId;

    @PrimaryKeyColumn(name = "resource", ordinal = 2, type = PrimaryKeyType.PARTITIONED)
    private String resource;

    @PrimaryKeyColumn(name = "id", ordinal = 3, type = PrimaryKeyType.CLUSTERED)
    private Long id;

    public ResourceRelationKey() {
    }

    public ResourceRelationKey(String relationship, Long parentId, String resource, Long id) {
        this.relationship = relationship;
        this.parentId = parentId;
        this.resource = resource;
        this.id = id;
    }

    public String getRelationship() {
        return relationship;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getResource() {
        return resource;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ResourceRelationKey that)) {
            return false;
        }
        return Objects.equals(relationship, that.relationship)
                && Objects.equals(parentId, that.parentId)
                && Objects.equals(resource, that.resource)
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationship, parentId, resource, id);
    }
}