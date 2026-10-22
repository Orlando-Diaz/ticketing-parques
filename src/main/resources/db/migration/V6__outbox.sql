-- Outbox: los eventos se anotan aqui EN LA MISMA transaccion que el cambio de negocio y despues se entregan.
create table outbox (
    id           bigint generated always as identity primary key,
    tipo         varchar(100) not null,
    payload      text         not null,
    creado_en    timestamp    not null,
    publicado_en timestamp,
    intentos     integer      not null default 0,
    ultimo_error varchar(500),
    fallido      boolean      not null default false
);

create index idx_outbox_publicado_en on outbox (publicado_en);
