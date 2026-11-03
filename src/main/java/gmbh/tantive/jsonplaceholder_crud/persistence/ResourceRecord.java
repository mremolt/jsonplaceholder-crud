package gmbh.tantive.jsonplaceholder_crud.persistence;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("resources")
public class ResourceRecord {

	@PrimaryKey
	private ResourceRecordKey key;

	@Column("payload")
	private String payload;

	public ResourceRecord() {
	}

	public ResourceRecord(ResourceRecordKey key, String payload) {
		this.key = key;
		this.payload = payload;
	}

	public ResourceRecordKey getKey() {
		return key;
	}

	public void setKey(ResourceRecordKey key) {
		this.key = key;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}
}
