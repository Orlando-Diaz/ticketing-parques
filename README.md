# ticketing-parques

Sistema de venta de entradas y control de acceso para parques y eventos.
Proyecto para practicar **principios SOLID** y **arquitectura hexagonal** con Java 21.

## Estado
Fase 1 en curso: dominio puro. Motor de precios con tarifas configurables por parque
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

## Ejecutar los tests
```
mvn test
```

## Decisiones de arquitectura
Ver [docs/adr](docs/adr).
