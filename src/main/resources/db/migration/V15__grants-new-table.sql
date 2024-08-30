DO
$$
BEGIN
    IF (SELECT exists(SELECT rolname FROM pg_roles WHERE rolname = 'cloudsqliamuser'))
    THEN
        grant all on all tables in schema public to cloudsqliamuser;
    END IF;
END
$$;