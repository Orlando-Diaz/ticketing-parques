# ticketing-parques

Sistema de venta de entradas y control de acceso para parques y eventos.
Proyecto para practicar **principios SOLID** y **arquitectura hexagonal** con Java 21.

## Estado
Dominio completo (precios, catalogo, ventas, facturacion por eventos) y API REST con Spring Boot. Persistencia en PostgreSQL con Flyway y JPA. Siguiente: concurrencia del cupo.

Detalle del dominio: Motor de precios con tarifas configurables por parque
(tarifa unica o por categoria de visitante) y reglas de ajuste opcionales.

## Modulos
| Modulo | Responsabilidad |
|---|---|
| catalogo | Tipos de entrada, fechas y aforo |
| precios | Motor de reglas de precio (descuentos y recargos) |
| ventas | Carrito, orden y pago |
| accesos | QR y validacion en puerta |
| facturacion | Facturas a partir de las ventas |
| reportes | Ventas e ingresos |

## Ejecutar
Necesita PostgreSQL con una base de datos vacia llamada `ticketing_parques` (Flyway crea las tablas al arrancar).

```powershell
# PowerShell: las credenciales van en variables de entorno, nunca en el codigo
$env:DB_PASSWORD = "tu_contrasena"
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

## Decisiones de arquitectura
Ver [docs/adr](docs/adr).
