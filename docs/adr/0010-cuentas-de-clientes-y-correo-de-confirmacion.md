# ADR 0010: Cuentas de clientes y correo de confirmacion

## Contexto
Hasta ahora cualquiera compraba sin identificarse. Se quiere que cada persona tenga su cuenta, que sus compras queden guardadas en su perfil y que reciba la confirmacion por correo.

## Decision
- **Cuentas en el modulo `seguridad`:** nuevo rol `CLIENTE`. El nombre de usuario de un cliente es su correo en minusculas. `POST /api/auth/registro` crea la cuenta y devuelve el mismo token que el login. Ver parques y precios sigue siendo publico; **comprar exige cuenta** (`CLIENTE` o `ADMIN`).
- **`ventas` sigue sin conocer `seguridad`:** el controlador recibe el nombre de quien compra (`Principal`) y lo pasa como un simple texto, `cliente`, que la orden y el evento `OrdenConfirmada` llevan. Con eso `GET /api/ordenes/mias` lista las compras de esa persona.
- **Correo en un modulo nuevo, `notificaciones`:** escucha `OrdenConfirmada` (igual que facturacion), y un caso de uso arma y envia el correo por un puerto (`EnviadorDeCorreo`). Adaptadores: SMTP (Gmail u otro, por variables `MAIL_*`) y uno de consola para desarrollo, que se usa si no hay SMTP configurado. Una tabla `notificacion` evita enviar dos veces el mismo aviso (los eventos se entregan "al menos una vez").
- Los datos viejos se mantienen: `cliente` es opcional y los eventos antiguos del outbox sin ese campo siguen leyendose.

## Consecuencias y limites
- **El correo no se verifica:** quien se registra podria usar un correo ajeno. Es aceptable para un proyecto de practica; lo correcto seria un enlace de verificacion.
- **El correo no se reintenta:** el listener captura el error para que un fallo de SMTP no frene a los demas eventos del outbox (el despachador avanza por cursor). La compra nunca se deshace por un correo fallido. La mejora seria un outbox propio de notificaciones con reintentos.
- Solo hay correo al confirmar; no hay aviso de cancelacion ni recuperacion de clave.
