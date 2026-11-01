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

	@Column("user_id")
	private Long userId;

	@Column("post_id")
	private Long postId;

	@Column("album_id")
	private Long albumId;

	@Column("completed")
	private Boolean completed;

	public ResourceRecord() {
	}

	public ResourceRecord(ResourceRecordKey key, String payload, Long userId, Long postId, Long albumId,
			Boolean completed) {
		this.key = key;
		this.payload = payload;
		this.userId = userId;
		this.postId = postId;
		this.albumId = albumId;
		this.completed = completed;
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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getPostId() {
		return postId;
	}

	public void setPostId(Long postId) {
		this.postId = postId;
	}

	public Long getAlbumId() {
		return albumId;
	}

	public void setAlbumId(Long albumId) {
		this.albumId = albumId;
	}

	public Boolean getCompleted() {
		return completed;
	}

	public void setCompleted(Boolean completed) {
		this.completed = completed;
	}
}