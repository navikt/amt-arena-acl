ALTER TABLE arena_data
    DROP CONSTRAINT arena_data_ingest_status_check;

ALTER TABLE arena_data
    ADD CONSTRAINT arena_data_ingest_status_check
        CHECK ( ingest_status IN
                ('NEW', 'HANDLED', 'RETRY', 'FAILED', 'IGNORED', 'INVALID', 'WAITING', 'EXTERNAL_SOURCE')
            );
