package gmbh.tantive.jsonplaceholder_crud.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRecord;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRecordKey;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRecordRepository;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationKey;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationRecord;
import gmbh.tantive.jsonplaceholder_crud.persistence.ResourceRelationRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CassandraPostServiceTest {

	@Mock
	private ResourceRecordRepository repository;

	@Mock
	private ResourceRelationRecordRepository relationRepository;

	@Test
	void importsUpstreamIdsAndUsesTheMaximumForNewPosts() {
		PostService service = service(JsonMapper.builder().build());
		Post first = post(11L, 4L, "First", "First body");
		Post second = post(12L, 4L, "Second", "Second body");

		assertEquals(2, service.importAll(List.of(first, second)));
		assertEquals(13L, service.create(post(null, 4L, "Third", "Third body")).getId());

		verify(repository).saveAll(argThat(savedRecords -> {
			List<ResourceRecord> saved = StreamSupport.stream(savedRecords.spliterator(), false).toList();
			return saved.size() == 2
					&& saved.get(0).getKey().getId() == 11L
					&& saved.get(1).getKey().getId() == 12L
					&& saved.get(0).getUserId() == 4L;
		}));
		verify(relationRepository).saveAll(argThat(savedRelations -> {
			List<ResourceRelationRecord> saved = StreamSupport.stream(savedRelations.spliterator(), false).toList();
			return saved.size() == 2
					&& saved.get(0).getKey().getRelationship().equals("user")
					&& saved.get(0).getKey().getParentId() == 4L
					&& saved.get(0).getKey().getId() == 11L
					&& saved.get(1).getKey().getId() == 12L;
		}));
	}

	@Test
	void readsMaximumIdOnceForMultipleCreates() {
		ResourceRecord existing = new ResourceRecord(new ResourceRecordKey("posts", 42L), "{}", 4L, null, null,
				null);
		when(repository.findLatestByResource("posts")).thenReturn(Optional.of(existing));
		PostService service = service(JsonMapper.builder().build());

		Post first = service.create(post(null, 4L, "First", "Body"));
		Post second = service.create(post(null, 4L, "Second", "Body"));

		assertEquals(43L, first.getId());
		assertEquals(44L, second.getId());
		verify(repository, times(1)).findLatestByResource("posts");
	}

	@Test
	void patchPreservesUnspecifiedFields() throws Exception {
		ObjectMapper objectMapper = JsonMapper.builder().build();
		Post existing = post(1L, 7L, "Original title", "Keep this body");
		ResourceRecord stored = new ResourceRecord(new ResourceRecordKey("posts", 1L),
				objectMapper.writeValueAsString(existing), 7L, null, null, null);
		when(repository.findById(new ResourceRecordKey("posts", 1L))).thenReturn(Optional.of(stored));
		Post changes = new Post();
		changes.setTitle("Updated title");
		PostService service = service(objectMapper);

		Post result = service.update(1L, changes);

		assertEquals(1L, result.getId());
		assertEquals("Updated title", result.getTitle());
		assertEquals("Keep this body", result.getBody());
		assertEquals(7L, result.getUserId());
		ArgumentCaptor<ResourceRecord> saved = ArgumentCaptor.forClass(ResourceRecord.class);
		verify(repository).save(saved.capture());
		Post persisted = objectMapper.readValue(saved.getValue().getPayload(), Post.class);
		assertEquals("Keep this body", persisted.getBody());
	}

	@Test
	void returnsNotFoundForUnknownPost() {
		PostService service = service(JsonMapper.builder().build());

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> service.findById(99L));

		assertEquals(404, exception.getStatusCode().value());
	}

	@Test
	void loadsOnlyTheRequestedCollectionWindowFromCassandra() {
		List<ResourceRecord> firstFifteen = java.util.stream.LongStream.rangeClosed(1, 15)
				.mapToObj(id -> record(id, 7L))
				.toList();
		when(repository.findPageByResource("posts", 15)).thenReturn(firstFifteen);
		PostService service = service(JsonMapper.builder().build());

		List<Post> page = service.findAll(new CollectionPage(10, 5));

		assertEquals(List.of(11L, 12L, 13L, 14L, 15L), page.stream().map(post -> post.getId()).toList());
		verify(repository).findPageByResource("posts", 15);
	}

	@Test
	void filtersPostsThroughTheUserRelationPartition() {
		ResourceRelationRecord relation = new ResourceRelationRecord(
				new ResourceRelationKey("user", 7L, "posts", 8L),
				"{\"id\":8,\"userId\":7,\"title\":\"Title\",\"body\":\"Body\"}");
		when(relationRepository.findPage("user", 7L, "posts", 10)).thenReturn(List.of(relation));
		PostService service = service(JsonMapper.builder().build());

		List<Post> posts = service.findByUserId(7L, new CollectionPage(0, 10));

		assertEquals(List.of(8L), posts.stream().map(post -> post.getId()).toList());
		verify(relationRepository).findPage("user", 7L, "posts", 10);
	}

	@Test
	void loadsTheNextKeysetBackfillPage() {
		when(repository.findPageAfterId("posts", 5L, 2)).thenReturn(List.of(record(6L, 7L), record(7L, 7L)));
		PostService service = service(JsonMapper.builder().build());

		List<Post> page = service.findAfterId(5L, 2);

		assertEquals(List.of(6L, 7L), page.stream().map(post -> post.getId()).toList());
		verify(repository).findPageAfterId("posts", 5L, 2);
	}

	@Test
	void movesRelationEntryWhenPostUserChanges() throws Exception {
		ObjectMapper objectMapper = JsonMapper.builder().build();
		Post existing = post(1L, 7L, "Title", "Body");
		ResourceRecord stored = new ResourceRecord(new ResourceRecordKey("posts", 1L),
				objectMapper.writeValueAsString(existing), 7L, null, null, null);
		when(repository.findById(new ResourceRecordKey("posts", 1L))).thenReturn(Optional.of(stored));
		Post changes = new Post();
		changes.setUserId(8L);
		PostService service = service(objectMapper);

		service.update(1L, changes);

		verify(relationRepository).deleteById(new ResourceRelationKey("user", 7L, "posts", 1L));
		verify(relationRepository).saveAll(argThat(records -> records.iterator().next().getKey()
				.equals(new ResourceRelationKey("user", 8L, "posts", 1L))));
	}

	@Test
	void deletesPostRelationEntry() throws Exception {
		ObjectMapper objectMapper = JsonMapper.builder().build();
		Post existing = post(1L, 7L, "Title", "Body");
		ResourceRecord stored = new ResourceRecord(new ResourceRecordKey("posts", 1L),
				objectMapper.writeValueAsString(existing), 7L, null, null, null);
		when(repository.findById(new ResourceRecordKey("posts", 1L))).thenReturn(Optional.of(stored));
		PostService service = service(objectMapper);

		service.delete(1L);

		verify(repository).deleteById(new ResourceRecordKey("posts", 1L));
		verify(relationRepository).deleteById(new ResourceRelationKey("user", 7L, "posts", 1L));
	}

	private PostService service(ObjectMapper objectMapper) {
		return new CassandraPostService(repository, relationRepository, objectMapper);
	}

	private Post post(Long id, Long userId, String title, String body) {
		Post post = new Post();
		post.setId(id);
		post.setUserId(userId);
		post.setTitle(title);
		post.setBody(body);
		return post;
	}

	private ResourceRecord record(long id, long userId) {
		String payload = "{\"id\":" + id + ",\"userId\":" + userId
				+ ",\"title\":\"Title\",\"body\":\"Body\"}";
		return new ResourceRecord(new ResourceRecordKey("posts", id), payload, userId, null, null, null);
	}
}