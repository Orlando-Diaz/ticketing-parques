# ADR 0002 - Tarifas configurables por parque

- Fecha: 2026-10-09
- Estado: aceptada

## Contexto
La primera version tenia descuentos fijos en el codigo (nino 50 %, adulto mayor 30 %).
Pero cada parque cobra distinto: algunos tienen un precio unico y otros precios por
categoria (nino, adulto...), y cada uno define sus propios rangos de edad y porcentajes.

## Decision
- El precio base viene de un `EsquemaDeTarifa` (interfaz) con dos implementaciones:
  `TarifaUnica` y `TarifaPorCategoria`.
- Las categorias (`CategoriaVisitante`) y sus precios las define cada parque.
- Las reglas de ajuste (residente, grupo, fin de semana) siguen siendo `ReglaDePrecio`,
  pero reciben sus porcentajes por constructor y cada parque activa las que quiere.
- Un `CalculadorDePrecio` se arma por parque con su esquema y sus reglas.
- Se eliminan `DescuentoNino` y `DescuentoAdultoMayor`: las categorias los reemplazan.

## Consecuencias
- Cambiar precios o activar reglas es configuracion, no cambio de codigo.
- Falta guardar la configuracion de cada parque (siguiente paso: puerto de repositorio
  en la capa de aplicacion y adaptador de persistencia).
- Si un parque no cubre una edad con ninguna categoria, el calculo falla con
  `SinTarifaParaEdadException`.
