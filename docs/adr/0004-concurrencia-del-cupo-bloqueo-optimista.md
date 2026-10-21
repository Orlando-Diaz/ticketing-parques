# ADR 0004 - Concurrencia del cupo con bloqueo optimista y reintentos

- Fecha: 2026-10-21
- Estado: aceptada

## Contexto
`ReservarCupo` hacia leer -> modificar -> guardar. Si dos compradores pedian la ultima entrada al mismo
tiempo, ambos leian "queda 1", ambos reservaban con exito y el segundo guardado pisaba al primero
(*lost update*): se vendia una entrada que no existia. La regla "no pasarse del cupo" vive en
`DisponibilidadDiaria`, pero una regla en memoria no protege de otro hilo que tiene su propia copia.

## Decision
- **Bloqueo optimista.** La tabla `disponibilidad_diaria` tiene una columna `version` (migracion V2).
  JPA la marca con `@Version`: cada UPDATE sube la version y agrega `WHERE version = <la que se leyo>`.
  Si otro guardo antes, el UPDATE no afecta ninguna fila y se lanza una excepcion.
- **El puerto define el contrato**, no JPA: `RepositorioDeDisponibilidad.guardar()` lanza
  `ConflictoDeConcurrenciaException` si alguien se adelanto. El adaptador JPA traduce
  `ConcurrencyFailureException` (version vieja) y `DataIntegrityViolationException` (dos crean el mismo
  dia a la vez: gana la llave primaria) a esa excepcion. El repositorio en memoria cumple el mismo contrato.
- **La version viaja dentro de `DisponibilidadDiaria`** (campo opaco, `null` = nueva) para que el
  adaptador sepa con que version se leyo. El dominio no la usa para ninguna regla.
- **Reintento en la capa de aplicacion** (`ReintentoPorConflicto`, 10 intentos por defecto). Cada intento
  repite TODO el flujo, incluida la lectura, asi el perdedor ve el cupo actualizado y recibe
  `CupoAgotadoException` si ya no alcanza. Si se agotan los reintentos, la API responde 409.
- **El `CHECK (vendidas <= cupo_maximo)` de la base sigue siendo la ultima defensa**, por si algun bug
  futuro se salta todo lo anterior.

## Por que optimista y no pesimista (`SELECT ... FOR UPDATE`)
- Los conflictos reales son raros (solo cuando se compra el mismo parque y dia a la vez), asi que casi
  nunca se reintenta y no se mantienen filas bloqueadas mientras se piensa.
- Mantiene los casos de uso sin depender de transacciones ni de Spring.
- Contra: bajo muchisima competencia por la ultima entrada hay reintentos. Un conflicto solo ocurre cuando
  otra compra logro guardar, y con N entradas hay como maximo N guardados, asi que los reintentos terminan.
  Si algun dia un evento masivo lo exige, se puede pasar a un UPDATE atomico condicional
  (`SET vendidas = vendidas + n WHERE vendidas + n <= cupo_maximo`) o a una cola.

## Como se prueba
- `ReservarCupoConcurrenteTest`: una barrera obliga a que dos compradores lean "queda 1" antes de guardar
  (determinista, sin depender de la suerte del reloj).
- `ConcurrenciaCupoJpaTest`: 20 hilos contra 5 entradas sobre la base real (H2 + Flyway); contrato del
  repositorio (version vieja rechazada, doble creacion rechazada) y el `CHECK` como ultima defensa.

## Pendiente relacionado
- Cancelar la misma orden dos veces a la vez puede liberar el cupo dos veces (la orden no tiene version).
