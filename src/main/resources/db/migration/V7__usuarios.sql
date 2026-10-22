-- Personal que puede iniciar sesion. La clave se guarda cifrada con BCrypt, nunca en claro.
create table usuario (
    nombre        varchar(50)  primary key,
    clave_cifrada varchar(100) not null,
    rol           varchar(20)  not null,
    creado_en     timestamp    not null
);
