package gmbh.tantive.jsonplaceholder_crud.persistence;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("resource_relations")
public class ResourceRelationRecord {

    @PrimaryKey
    private ResourceRelationKey key;

    @Column("payload")
    private String payload;

    public ResourceRelationRecord() {
    }

    public ResourceRelationRecord(ResourceRelationKey key, String payload) {
        this.key = key;
        this.payload = payload;
    }

    public ResourceRelationKey getKey() {
        return key;
    }

    public void setKey(ResourceRelationKey key) {
        this.key = key;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}