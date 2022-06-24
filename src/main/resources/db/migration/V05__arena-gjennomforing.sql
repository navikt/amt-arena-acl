CREATE TABLE arena_gjennomforing
(
        id                      UUID PRIMARY KEY        NOT NULL,
        tiltak_id               UUID                    NOT NULL,
        tiltak_navn             VARCHAR                 NOT NULL,
        tiltak_kode             VARCHAR                 NOT NULL,
        virksomhetsnummer       VARCHAR                 NOT NULL,
        navn                    VARCHAR                 NOT NULL,
        start_dato              DATE,
        slutt_dato              DATE,
        registrert_dato         TIMESTAMP WITH TIME ZONE NOT NULL,
        fremmote_dato           TIMESTAMP WITH TIME ZONE,
        status                  VARCHAR NOT NULL,
        ansvarlig_nav_enhetId   VARCHAR,
        opprettet_aar           INT,
        lopenr                  INT,
        arena_sak_id            BIGINT
);

