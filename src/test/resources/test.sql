begin transaction;

insert into arena_tiltak (id, kode, navn)
values ('5af397ee-d0ff-4a92-8725-4a7bc4ec70ef',
        'WOLOLO',
        'WOLOLO');

commit;

begin transaction;

DELETE
FROM arena_tiltak;

delete
from arena_data_id_translation;

delete
from arena_data;

truncate table arena_tiltak cascade;
truncate table arena_data_id_translation cascade;
truncate table arena_data cascade;

commit;
