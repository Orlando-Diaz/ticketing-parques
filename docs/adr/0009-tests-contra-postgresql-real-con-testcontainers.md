# ADR 0009 - Los tests de base de datos tambien corren contra PostgreSQL real (Testcontainers)

- Fecha: 2026-10-21
- Estado: aceptada

## Contexto
Los tests de persistencia, concurrencia, outbox y API usan H2 en memoria con `MODE=PostgreSQL`. Es rapido y no pide
nada instalado, pero H2 no es PostgreSQL: el bloqueo optimista, las restricciones CHECK, el SQL escrito a mano
(outbox, reportes, usuarios) y la traduccion de errores de llave duplicada pueden comportarse distinto. Lo que se
prueba en H2 no demuestra del todo lo que pasara en produccion.

## Decision
- **Se mantienen los tests con H2** como base rapida (corren siempre, sin Docker).
- **Los mismos tests se repiten contra PostgreSQL real**: por cada clase hay una subclase `*PostgresTest` marcada con
  `@ConPostgresReal` que hereda todos los `@Test` sin copiarlos. La anotacion levanta un contenedor
  `postgres:18-alpine` con Testcontainers (uno solo por ejecucion, compartido) y apunta la aplicacion a el; Flyway
  aplica las migraciones sobre esa base.
- **Se omiten solos si no hay Docker** (`@EnabledIf`), para que `mvn test` siga funcionando en cualquier maquina. En
  GitHub Actions (ubuntu) Docker existe, asi que alli SI corren.
- Se usa `GenericContainer` y no `PostgreSQLContainer`/`@ServiceConnection`: la API de esas clases cambio entre
  versiones de Testcontainers y asi el montaje no depende de ello.

## Consecuencias
- Los tests clave (nunca se vende mas que el cupo, nunca entran mas personas que las compradas, el outbox, los CHECK)
  quedan demostrados contra el motor real.
- La primera ejecucion descarga la imagen (~100 MB) y cada ejecucion suma unos segundos de arranque.
- Si un test pasa en H2 y falla en PostgreSQL, el que manda es PostgreSQL: es la diferencia que se queria descubrir.
