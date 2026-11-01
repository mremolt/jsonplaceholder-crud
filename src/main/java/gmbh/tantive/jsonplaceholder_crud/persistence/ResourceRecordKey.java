package gmbh.tantive.jsonplaceholder_crud.persistence;

import java.io.Serializable;
import java.util.Objects;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

@PrimaryKeyClass
public class ResourceRecordKey implements Serializable {

	@PrimaryKeyColumn(name = "resource", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
	private String resource;

	@PrimaryKeyColumn(name = "id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
	private Long id;

	public ResourceRecordKey() {
	}

	public ResourceRecordKey(String resource, Long id) {
		this.resource = resource;
		this.id = id;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ResourceRecordKey that)) {
			return false;
		}
		return Objects.equals(resource, that.resource) && Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(resource, id);
	}
}