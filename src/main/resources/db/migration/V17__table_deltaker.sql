CREATE TABLE deltaker
(
    id                  uuid primary key,
    arena_id            float NOT NULL,
    person_id           float NOT NULL,
    gjennomforing_id    float not null,
    dato_fra            date,
    dato_til            date,
    reg_dato            timestamp with time zone not null,
    mod_dato            timestamp with time zone not null,
    status              varchar not null,
    dato_statusendring  timestamp with time zone not null,
    ekstern_id          uuid,
    arena_source_table  varchar not null,
    created_at          timestamp with time zone default current_timestamp,
    modified_at         timestamp with time zone default current_timestamp,

    UNIQUE (arena_id, arena_source_table)
);
