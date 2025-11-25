CREATE INDEX CONCURRENTLY arena_data_pos_status_table_idx
    ON arena_data (operation_pos, ingest_status, arena_table_name);
