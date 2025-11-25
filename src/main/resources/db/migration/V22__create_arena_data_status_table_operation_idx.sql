DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_class c
                     JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = 'arena_data_status_table_operation_idx'
              AND c.relkind = 'i'
        ) THEN
            EXECUTE 'CREATE INDEX CONCURRENTLY arena_data_status_table_operation_idx
                ON arena_data (ingest_status, arena_table_name, operation_pos)';
        END IF;
    END$$;