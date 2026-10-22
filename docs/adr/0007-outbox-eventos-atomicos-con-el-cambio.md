# ADR 0007 - Outbox: los eventos se guardan junto con el cambio y se entregan despues

- Fecha: 2026-10-21
- Estado: aceptada

## Contexto
Antes, un caso de uso guardaba la orden (o el pase) y DESPUES publicaba el evento al bus en memoria. Si el proceso
moria entre las dos cosas, la venta existia pero nadie se enteraba: no habia factura, ni pase, ni reporte, y no
quedaba rastro de que faltaba algo. Es el problema de la "doble escritura": no se puede escribir en la base y en el
bus a la vez de forma atomica.

## Decision
- **Patron outbox.** El evento se anota en la tabla `outbox` DENTRO de la misma transaccion que guarda el agregado:
  o quedan los dos o no queda ninguno. Un despachador lee la tabla y entrega los eventos al bus.
- **Puerto `EjecutorTransaccional`** (en `shared.application`, sin Spring): los casos de uso lo usan para envolver
  `guardar(...)` + `publicarTodos(...)`. Su implementacion (`EjecutorTransaccionalSpring`) usa `TransactionTemplate`.
  Sin base de datos (tests en memoria) se usa `SIN_TRANSACCION`, que ejecuta el trabajo tal cual.
- **`PublicadorDeEventos` no cambia**: el caso de uso sigue diciendo "publicar". Lo que cambia es el adaptador:
  `PublicadorConOutbox` anota en la tabla y se niega a funcionar fuera de una transaccion (fallar fuerte es mejor
  que perder la garantia sin darse cuenta).
- **La transaccion va dentro de cada intento del reintento por conflicto**, nunca alrededor: despues de un conflicto
  de version la sesion de Hibernate queda inservible y hay que empezar con una transaccion nueva.
- **Entrega inmediata + red de seguridad.** Al terminar la transaccion, `EjecutorTransaccionalSpring` despierta al
  despachador y el evento sale al instante (la API se comporta igual que antes). Ademas un programador
  (`@Scheduled`, cada 5 s) recoge lo que se haya quedado: caidas del proceso o suscriptores que fallaron.
- **Garantia: al menos una vez.** Un evento puede entregarse mas de una vez (p. ej. el proceso muere tras entregar y
  antes de marcar la fila). Por eso todos los suscriptores ya son idempotentes (ADR 0005 y 0006).
- **Entrega estricta para el despachador.** `BusDeEventosEnMemoria.entregar` hace llegar el evento a todos los
  suscriptores y, si alguno fallo, lanza al final; asi la fila sigue pendiente y se reintenta. `publicar` conserva su
  comportamiento (un suscriptor caido no rompe a los demas ni al publicador).
- **Filas venenosas.** Cada fallo suma un intento; tras `ticketing.outbox.maximo-de-intentos` (10) la fila pasa a
  `fallido = true`, deja de reintentarse y no bloquea a las demas. Alguien debe revisarla (hoy: consulta SQL).
- **Serializacion en JSON** (`SerializadorDeEventos`, Jackson). Cada tipo de evento se registra en `EventosConfig` con
  su nombre simple, que es lo que se guarda en la tabla. Un evento nuevo = una linea de registro + un test de ida y
  vuelta (`SerializadorDeEventosTest`).

## Consecuencias
- Ganamos: ningun evento se pierde por una caida; el dominio y los casos de uso siguen sin conocer Spring.
- El orden de entrega es el de insercion, pero NO esta garantizado entre transacciones concurrentes (la que obtiene
  el id mas bajo puede confirmar despues). Los suscriptores actuales no dependen del orden entre ordenes distintas.
- Con varias instancias de la aplicacion dos despachadores podrian entregar la misma fila: es seguro por la
  idempotencia, pero para evitar el trabajo doble habria que reclamar filas con `FOR UPDATE SKIP LOCKED` (PostgreSQL).
- `CancelarOrden` aun devuelve el cupo DESPUES de la transaccion: si eso falla, el cupo queda ocupado (lado seguro).
  Para cerrarlo del todo, "devolver cupo" tambien seria un mensaje del outbox con su suscriptor idempotente.
- Una tabla que crece: falta una limpieza periodica de filas publicadas antiguas.
