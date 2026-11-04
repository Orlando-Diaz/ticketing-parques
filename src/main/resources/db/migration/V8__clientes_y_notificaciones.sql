-- Cuentas de clientes: el nombre de usuario (clave primaria) puede ser ahora un correo, asi que se amplia.
alter table usuario alter column nombre set data type varchar(120);
alter table usuario add column nombre_visible varchar(100);

-- Quien hizo la compra: el nombre de usuario de su cuenta (para un cliente, su correo). Null en compras anteriores.
alter table orden add column cliente varchar(120);
create index idx_orden_cliente on orden (cliente, creada_en);

-- Correos ya enviados (una fila por aviso): la entrega de eventos es "al menos una vez" y no queremos mandar dos correos.
create table notificacion (
    clave     varchar(150) primary key,
    creada_en timestamp    not null
);
