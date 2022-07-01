# amt-arena-acl
Tjeneste som leser data fra Arena og muterer de til AMT-Domene

## Ingest
Leser data fra arena kafka topics, lagrer i arena_data tabellen sammen med status på konverteringen til AMT domene.
Det kjøres schedulerte jobber i tabellen, som sletter eller reingester meldinger basert på statusen.


## Rekonstruere meldinger på kafka
For å rekonstruere meldinger med nye data så kan dette trigges ved å endre status på nyeste melding knyttet til arena objektet i arena_data tabellen.
Meldingene vil da plukkes opp av en schedulert jobb.
```sql
update arena_data
set ingest_status   = 'RETRY',
ingest_attempts = 0,
last_attempted  = null
where id in (
    select max(id)
    from arena_data
    where arena_table_name = 'SIAMO.TILTAKGJENNOMFORING'
    and operation_type in ('CREATED', 'MODIFIED')
    and ingest_status = 'HANDLED'
    group by arena_id
);
```
