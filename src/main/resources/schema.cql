CREATE KEYSPACE IF NOT EXISTS jsonplaceholder
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS jsonplaceholder.resources (
	resource text,
	id bigint,
	payload text,
	PRIMARY KEY (resource, id)
);

-- Migration for existing keyspaces: the former filter columns and their indexes are no longer read or
-- written. Their values are fully contained in payload, so dropping them needs no backfill. Relation
-- queries use resource_relations, which ResourceRelationBackfill can rebuild from payload at any time.
DROP INDEX IF EXISTS jsonplaceholder.resources_user_id_sai;
DROP INDEX IF EXISTS jsonplaceholder.resources_post_id_sai;
DROP INDEX IF EXISTS jsonplaceholder.resources_album_id_sai;
DROP INDEX IF EXISTS jsonplaceholder.resources_completed_sai;
ALTER TABLE jsonplaceholder.resources DROP IF EXISTS user_id;
ALTER TABLE jsonplaceholder.resources DROP IF EXISTS post_id;
ALTER TABLE jsonplaceholder.resources DROP IF EXISTS album_id;
ALTER TABLE jsonplaceholder.resources DROP IF EXISTS completed;

CREATE TABLE IF NOT EXISTS jsonplaceholder.resource_relations (
	relationship text,
	parent_id bigint,
	resource text,
	id bigint,
	payload text,
	PRIMARY KEY ((relationship, parent_id, resource), id)
);

CREATE TABLE IF NOT EXISTS jsonplaceholder.auth_accounts_by_email (
	email text PRIMARY KEY,
	account_id uuid,
	password_hash text,
	role text,
	enabled boolean,
	created_at timestamp
);

-- Refresh tokens: only the SHA-256 hash of a token is stored. Rows are written with a TTL equal to the token
-- lifetime, so expired tokens and revocations disappear without a cleanup job. New tables, no backfill needed.
CREATE TABLE IF NOT EXISTS jsonplaceholder.auth_refresh_tokens (
	token_hash text PRIMARY KEY,
	family_id uuid,
	email text,
	used boolean,
	expires_at timestamp
);

CREATE TABLE IF NOT EXISTS jsonplaceholder.auth_revoked_refresh_families (
	family_id uuid PRIMARY KEY,
	revoked_at timestamp
);
