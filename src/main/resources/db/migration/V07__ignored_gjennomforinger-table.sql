CREATE TABLE ignored_arena_data
(
    id         uuid PRIMARY KEY references arena_data_id_translation (amt_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
)
