# amt-arena-acl
Tjeneste som leser data fra Arena, muterer det til AMT-Domene

## Hvordan reingeste data

Kjør SQL-spørringen under for å reingeste siste gjennomføring oppdatering.

```sql
update arena_data
set ingest_status   = 'RETRY',
    ingest_attempts = 0,
    last_attempted  = null
where id in (
    select ad.id
    from arena_data as ad
    where ad.id in (select id
                    from arena_data
                    where arena_id = ad.arena_id
                      and arena_table_name = 'SIAMO.TILTAKGJENNOMFORING'
                      and operation_type in ('CREATED', 'MODIFIED')
                      and ingest_status = 'HANDLED'
                    order by id desc
                    limit 1)
);
```