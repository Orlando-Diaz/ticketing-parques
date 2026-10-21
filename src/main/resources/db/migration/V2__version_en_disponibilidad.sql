-- Bloqueo optimista del cupo (ver docs/adr/0004).
-- "version" es el numero de edicion de cada fila: JPA lo sube en 1 en cada UPDATE y solo deja actualizar
-- si la version sigue siendo la que se leyo. Si otro comprador se adelanto, el UPDATE no afecta ninguna fila
-- y la aplicacion se entera (y reintenta) en lugar de pisar su venta en silencio.
alter table disponibilidad_diaria add column version bigint not null default 0;
