CREATE KEYSPACE IF NOT EXISTS jsonplaceholder
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS jsonplaceholder.resources (
	resource text,
	id bigint,
	payload text,
	user_id bigint,
	post_id bigint,
	album_id bigint,
	completed boolean,
	PRIMARY KEY (resource, id)
);

ALTER TABLE jsonplaceholder.resources ADD IF NOT EXISTS completed boolean;

DROP INDEX IF EXISTS jsonplaceholder.resources_user_id_sai;
DROP INDEX IF EXISTS jsonplaceholder.resources_post_id_sai;
DROP INDEX IF EXISTS jsonplaceholder.resources_album_id_sai;
DROP INDEX IF EXISTS jsonplaceholder.resources_completed_sai;

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