CREATE INDEX arena_data_status_table_operation_idx
    ON arena_data (ingest_status, arena_table_name, operation_pos);