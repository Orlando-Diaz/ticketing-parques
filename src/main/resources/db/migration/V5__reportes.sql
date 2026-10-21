-- Modulo reportes: modelo de LECTURA alimentado por eventos. Guarda HECHOS (una fila por orden y una por ingreso),
-- no contadores: asi un evento que llega dos veces no cuenta doble (la llave primaria lo rechaza).
-- Los totales se calculan al consultar. Sin llaves foraneas hacia otros modulos.
create table reporte_orden (
    orden_ref     varchar(100) primary key,
    parque_id     varchar(100) not null,
    fecha_visita  date         not null,
    entradas      integer      not null check (entradas >= 1),
    total         bigint       not null check (total >= 0),
    cancelada     boolean      not null default false,
    registrada_en timestamp    not null
);
create index ix_reporte_orden_parque_fecha on reporte_orden (parque_id, fecha_visita);

create table reporte_ingreso (
    orden_ref    varchar(100) not null,
    numero       integer      not null,          -- posicion de la pasada en el historial del pase (0, 1, 2...)
    parque_id    varchar(100) not null,
    fecha_visita date         not null,
    personas     integer      not null check (personas >= 1),
    puerta       varchar(100) not null,
    momento      timestamp    not null,
    primary key (orden_ref, numero)
);
create index ix_reporte_ingreso_parque_fecha on reporte_ingreso (parque_id, fecha_visita);
