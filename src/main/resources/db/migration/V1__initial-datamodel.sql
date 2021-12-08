CREATE TABLE arena_data
(
    id                  SERIAL PRIMARY KEY NOT NULL,
    table_name          VARCHAR            NOT NULL,
    operation_type      VARCHAR            NOT NULL CHECK ( operation_type IN ('INSERT', 'UPDATE', 'DELETE')),
    operation_pos       BIGINT             NOT NULL,
    operation_timestamp TIMESTAMP          NOT NULL,
    ingest_status       VARCHAR            NOT NULL DEFAULT 'NEW' CHECK ( ingest_status IN ('NEW', 'INGESTED', 'RETRY', 'FAILED', 'IGNORED')),
    ingested_timestamp  TIMESTAMP,
    ingest_attempts     INT                NOT NULL DEFAULT 0,
    last_retry          TIMESTAMP,
    before              JSONB,
    after               JSONB
);

CREATE UNIQUE INDEX arena_data_table_operation_type_operation_pos_idx on arena_data (table_name, operation_type, operation_pos);

CREATE TABLE arena_data_id_translation
(
    amt_id           UUID PRIMARY KEY NOT NULL,
    arena_table_name VARCHAR          NOT NULL,
    arena_id         VARCHAR          NOT NULL,
    is_ignored       BOOLEAN          NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX arena_data_id_translation_arena_table_name_arena_id_idx on arena_data_id_translation (arena_table_name, arena_id)
