CREATE TABLE arena_data_hist_id_translation
(
    amt_id           UUID PRIMARY KEY NOT NULL,
    arena_hist_id    VARCHAR          NOT NULL,
    arena_id         VARCHAR
);

CREATE UNIQUE INDEX arena_data_hist_id_translation_arena_hist_id_idx on arena_data_hist_id_translation (arena_hist_id);
