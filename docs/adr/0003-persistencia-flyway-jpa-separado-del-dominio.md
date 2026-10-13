# ADR 0003 - Persistencia con Flyway y JPA, separada del dominio

- Fecha: 2026-10-13
- Estado: aceptada

## Contexto
Hasta ahora todo vivia en memoria. Hace falta PostgreSQL, sin contaminar el dominio con la base de datos.

## Decision
- **Entidades JPA separadas del dominio.** `Parque` (dominio) y `ParqueJpaEntity` (tabla) son clases distintas.
  El dominio no lleva anotaciones de JPA; un adaptador traduce en ambos sentidos.
- **Flyway** crea y versiona el esquema (`src/main/resources/db/migration`). Hibernate solo valida
  (`ddl-auto: validate`); nunca crea ni modifica tablas.
- **Cada modulo es dueno de sus tablas y no hay llaves foraneas entre modulos** (los modulos se hablan
  por la aplicacion, no por la base). Asi se podrian separar mas adelante.
- **Las reglas de precio se guardan por su descripcion** (`DescripcionDeRegla`) y se recrean con
  `FabricaDeReglas`; la base de datos y la API no conocen cada clase de regla. `EsquemaDeTarifa`
  es `sealed`: son pocos y fijos, y el compilador exige manejar todos.
- **Transacciones en los adaptadores** (`@Transactional` en los repositorios), no en los casos de uso:
  los casos de uso siguen sin depender de Spring. Las operaciones entre modulos se coordinan con
  compensacion (ver `ComprarEntradas`).
- Los **tests usan H2 en memoria** con las mismas migraciones, para correr sin PostgreSQL.
  Mejora futura: Testcontainers para probar contra PostgreSQL real.
- Las credenciales salen de variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`).

## Consecuencias
- Hay mas clases (entidad + adaptador por cada agregado), a cambio de un dominio limpio.
- La base tiene un `CHECK (vendidas <= cupo_maximo)` como ultima defensa del cupo.
- Pendiente: la concurrencia del cupo (dos compras simultaneas) todavia puede vender de mas.
