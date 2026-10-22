# ADR 0006 - Reportes como modelo de lectura alimentado por eventos

- Fecha: 2026-10-21
- Estado: aceptada

## Contexto
Los reportes (ventas por dia, cancelaciones, quienes entraron y quienes faltan) necesitan datos de ventas y de
accesos. Consultar las tablas de esos modulos romperia su independencia (cada modulo es dueno de sus tablas) y
acoplaria los reportes a su estructura interna.

## Decision
- **Reportes es un modelo de LECTURA (CQRS ligero).** Escucha los eventos `OrdenConfirmada`, `OrdenCancelada` e
  `IngresoRegistrado` y guarda su propia copia de lo que necesita. Ventas y accesos no saben que reportes existe
  (ArchUnit lo vigila); solo el adaptador `EscuchaDeEventosDeOtrosModulos` conoce a los otros modulos.
- **Se guardan HECHOS, no contadores.** Una fila por orden (`reporte_orden`) y una por ingreso (`reporte_ingreso`,
  identificada por orden + numero de ingreso). Un evento que llega dos veces choca con la llave primaria y se
  ignora, asi que los totales no se duplican. Con contadores ("vendidas += 3") una reentrega falsearia los numeros.
- **Los totales se calculan al consultar**, en el dominio (`ResumenDelDia.de(...)`), no en SQL: la logica queda
  probada con tests simples. Reglas: las ordenes canceladas no cuentan como venta; "sin ingresar" nunca es negativo.
- **Cancelar es terminal**: una confirmacion repetida que llegue despues de cancelar no revive la orden (la
  insercion repetida se ignora y no pisa el estado cancelado).
- **SQL directo con `JdbcTemplate`**, sin entidades JPA: es un modelo de lectura sin reglas de negocio que
  proteger, y el SQL simple es lo mas claro.
- Los reportes son por **fecha de visita** (el dia en que el visitante va al parque), no por fecha de compra.

## Consecuencias
- **Consistencia eventual**: el reporte puede ir un instante detras de la venta. Mas importante: el bus de eventos
  es en memoria; si el proceso se cae entre guardar la venta y publicar el evento, el reporte no se entera nunca.
  Es el problema que resuelve el patron **outbox** (guardar el evento en la misma transaccion y publicarlo despues,
  con reintentos). Esta anotado como siguiente paso.
- Si un evento se pierde o se cambia la logica, habria que reconstruir el modelo reproduciendo los eventos o los
  datos de origen; hoy no existe esa herramienta.
- El rango se calcula en memoria (con tope de 366 dias). Si un parque tuviera millones de filas, convendria agregar
  en SQL (`GROUP BY`) o precalcular resumenes.
- Un `OrdenCancelada` que llegara antes que su `OrdenConfirmada` se ignoraria (el bus actual entrega en orden).
- "Ocupacion en tiempo real" hoy significa personas que **han ingresado** en el dia; no se registran salidas.

> Actualizacion: el hueco de "evento perdido si el proceso muere" se cerro con el outbox (ADR 0007).
