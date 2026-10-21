-- Bloqueo optimista de la orden (ver docs/adr/0004): evita que dos cancelaciones simultaneas de la misma
-- orden tengan exito las dos (y devuelvan el cupo dos veces).
alter table orden add column version bigint not null default 0;
