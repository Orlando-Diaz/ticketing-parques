# ticketing-parques

[![CI](https://github.com/Orlando-Diaz/ticketing-parques/actions/workflows/ci.yml/badge.svg)](https://github.com/Orlando-Diaz/ticketing-parques/actions/workflows/ci.yml)

Sistema de venta de entradas y control de acceso para parques y eventos.
Proyecto para practicar **principios SOLID** y **arquitectura hexagonal** con Java 21.

## Estado
Modulos de precios, catalogo, ventas, facturacion (por eventos) y accesos (pase con QR por compra), con API REST en Spring Boot y persistencia en PostgreSQL (Flyway + JPA). El cupo y las cancelaciones son seguros ante compras simultaneas (bloqueo optimista + reintentos). Reportes por eventos (modelo de lectura). Los eventos se entregan con el patron outbox (no se pierden si el proceso se cae). Cada push corre ~150 tests en GitHub Actions. API protegida con JWT y roles (ADMIN, PUERTA).

Detalle del dominio: Motor de precios con tarifas configurables por parque
(tarifa unica o por categoria de visitante) y reglas de ajuste opcionales.

## Modulos
| Modulo | Responsabilidad |
|---|---|
| catalogo | Tipos de entrada, fechas y aforo |
| precios | Motor de reglas de precio (descuentos y recargos) |
| ventas | Carrito, orden y pago |
| accesos | Un pase (QR) por compra y validacion en la puerta |
| facturacion | Facturas a partir de las ventas |
| reportes | Modelo de lectura alimentado por eventos: ventas, cancelaciones e ingresos por dia |

## Ejecutar
Necesita PostgreSQL con una base de datos vacia llamada `ticketing_parques` (Flyway crea las tablas al arrancar).

```powershell
# PowerShell: las credenciales van en variables de entorno, nunca en el codigo
$env:DB_PASSWORD = "tu_contrasena"
$env:JWT_SECRET = "una-cadena-aleatoria-de-al-menos-32-caracteres"   # firma los tokens (obligatoria)
$env:ADMIN_PASSWORD = "clave-de-admin-de-10+-caracteres"       # crea al usuario "admin" la primera vez
# opcionales (valores por defecto): DB_USER=postgres, DB_URL=jdbc:postgresql://localhost:5432/ticketing_parques

mvn test                 # unitarios, arquitectura, persistencia y API (usan H2 en memoria, no tocan tu PostgreSQL)
mvn spring-boot:run      # levanta la API en http://localhost:8080
```
Ejemplos de peticiones en [docs/api.http](docs/api.http).

## API
| Metodo | Ruta | Que hace |
|---|---|---|
| POST | /api/parques | Registra un parque |
| PUT | /api/parques/{id}/precios | Configura tarifas y reglas del parque |
| GET | /api/parques/{id}/precios/cotizacion | Cotiza una entrada |
| GET | /api/parques/{id}/disponibilidad?fecha= | Cupo disponible |
| POST | /api/ordenes | Compra entradas |
| GET | /api/ordenes/{id} | Consulta una orden |
| POST | /api/ordenes/{id}/cancelacion | Cancela una orden |
| GET | /api/ordenes/{id}/factura | Factura de la orden |
| GET | /api/ordenes/{id}/pase | Pase de la orden (el codigo es el QR) |
| POST | /api/accesos/ingresos | La puerta escanea el QR y deja entrar a N personas |
| GET | /api/accesos/pases/{codigo} | Estado de un pase y su historial de ingresos |
| GET | /api/reportes/parques/{id}/dia?fecha= | Resumen de un dia de visita: ventas, cancelaciones, ingresos |
| GET | /api/reportes/parques/{id}/rango?desde=&hasta= | Un resumen por cada dia con actividad (maximo 366 dias) |

## Decisiones de arquitectura
Ver [docs/adr](docs/adr). Incluye como se evita vender mas entradas que el cupo cuando varios compran a la vez ([ADR 0004](docs/adr/0004-concurrencia-del-cupo-bloqueo-optimista.md)) el modulo de control de acceso ([ADR 0005](docs/adr/0005-modulo-accesos-un-pase-por-compra.md)) los reportes ([ADR 0006](docs/adr/0006-reportes-modelo-de-lectura-por-eventos.md)), el outbox ([ADR 0007](docs/adr/0007-outbox-eventos-atomicos-con-el-cambio.md)) y la seguridad ([ADR 0008](docs/adr/0008-seguridad-jwt-y-roles.md)).
