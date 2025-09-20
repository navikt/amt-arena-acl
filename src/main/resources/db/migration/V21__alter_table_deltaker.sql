ALTER TABLE deltaker
    ALTER COLUMN arena_id TYPE INTEGER USING arena_id::INTEGER,
    ALTER COLUMN person_id TYPE INTEGER USING person_id::INTEGER,
    ALTER COLUMN gjennomforing_id TYPE INTEGER USING gjennomforing_id::INTEGER;