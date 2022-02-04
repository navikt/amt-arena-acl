CREATE TABLE arena_data
(
    id                  SERIAL PRIMARY KEY NOT NULL,
    arena_table_name    VARCHAR            NOT NULL,
    arena_id            VARCHAR            NOT NULL,
    operation_type      VARCHAR            NOT NULL CHECK ( operation_type IN ('CREATED', 'MODIFIED', 'DELETED')),
    operation_pos       VARCHAR            NOT NULL,
    operation_timestamp TIMESTAMP          NOT NULL,
    ingest_status       VARCHAR            NOT NULL DEFAULT 'NEW' CHECK ( ingest_status IN ('NEW', 'HANDLED', 'RETRY', 'FAILED', 'IGNORED')),
    ingested_timestamp  TIMESTAMP,
    ingest_attempts     INT                NOT NULL DEFAULT 0,
    last_attempted      TIMESTAMP,
    before              JSONB,
    after               JSONB,
    note                VARCHAR
);

CREATE UNIQUE INDEX arena_data_table_operation_type_operation_pos_idx on arena_data (arena_table_name, operation_type, operation_pos);

CREATE TABLE arena_data_id_translation
(
    amt_id           UUID PRIMARY KEY NOT NULL,
    arena_table_name VARCHAR          NOT NULL,
    arena_id         VARCHAR          NOT NULL,
    is_ignored       BOOLEAN          NOT NULL,
    current_hash     VARCHAR          NOT NULL
);

CREATE UNIQUE INDEX arena_data_id_translation_arena_table_name_arena_id_idx on arena_data_id_translation (arena_table_name, arena_id);

CREATE TABLE arena_tiltak
(
    id   UUID PRIMARY KEY NOT NULL,
    kode VARCHAR UNIQUE   NOT NULL,
    navn VARCHAR          NOT NULL

)
