# ADR 0005 - Modulo accesos: un pase (un QR) por compra

- Fecha: 2026-10-21
- Estado: aceptada

## Contexto
En la puerta del parque hay que decidir si una persona puede entrar. En un parque real el visitante llega con
un QR; hay grupos que entran por partes (parte del grupo llega despues) y hay varias puertas escaneando al mismo
tiempo.

## Decision
- **Un pase por orden, con un solo codigo (el del QR)** que sirve para las N personas de la compra. Cada pasada
  por la puerta registra un `Ingreso` (cuantas personas, cuando, por que puerta); el pase lleva el historial.
- **Las reglas de la puerta viven en `Pase.registrarIngreso()`** (dominio): solo vale el dia de la visita, no vale
  si la compra se cancelo y nunca entran mas personas que las compradas.
- **El codigo es aleatorio de 128 bits** (`SecureRandom`), no un consecutivo: quien conozca el codigo de otra
  compra podria usar sus entradas, asi que no debe poder adivinarse. Si el codigo no existe, el error no da pistas.
  Se guarda en la base (y no firmado) porque asi se puede anular al instante si la compra se cancela.
- **Accesos no conoce a ventas** (salvo su adaptador de eventos): al confirmarse una orden (`OrdenConfirmada`) se
  emite el pase; al cancelarse (`OrdenCancelada`) se anula. Ambos casos de uso son **idempotentes**, igual que
  facturacion, porque un evento puede llegar dos veces. ArchUnit lo vigila.
- **Cada modulo es dueno de sus puertos**: accesos tiene su propio `Reloj`, que decide que dia es "hoy" en la puerta
  (hora de Colombia).
- **Concurrencia**: dos puertas escaneando el mismo QR a la vez usan el mismo mecanismo del ADR 0004 (version +
  reintento). Ademas, la tabla tiene `CHECK (entradas_usadas <= entradas_totales)` como ultima defensa. La columna
  `entradas_usadas` se guarda aparte del historial para que cada ingreso modifique la fila del pase (y suba su version).

## Consecuencias
- Un QR por compra es simple para el comprador, pero quien tenga el QR puede usar todas las entradas. Un QR por
  persona seria mas seguro contra reventa, a costa de mas codigos que manejar.
- La API de la puerta (`POST /api/accesos/ingresos`) aun no tiene autenticacion: cualquiera que conozca un codigo
  puede registrar un ingreso. Pendiente: Spring Security con un rol para el personal de la puerta.
- La imagen del QR no se genera en el servidor: la API entrega el codigo y el cliente (o una libreria como ZXing,
  mas adelante) lo dibuja.
- Cada ingreso exitoso publica un evento `IngresoRegistrado` (con su numero dentro del pase) que consume `reportes` (ADR 0006).
