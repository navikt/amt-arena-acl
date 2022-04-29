CREATE TABLE arena_sak
(
    id                 SERIAL PRIMARY KEY       NOT NULL,
    arena_sak_id       BIGINT UNIQUE            NOT NULL,
    aar                INT                      NOT NULL,
    lopenr             INT                      NOT NULL,
    ansvarlig_enhet_id VARCHAR                  NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);