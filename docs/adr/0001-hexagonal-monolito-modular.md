# ADR 0001 - Arquitectura hexagonal en un monolito modular

- Fecha: 2026-10-09
- Estado: aceptada

## Contexto
El proyecto sirve para practicar SOLID y arquitectura. El dominio (venta de entradas
para parques y eventos) tiene reglas que cambian: precios, aforo, validacion de accesos.

## Decision
- **Monolito modular**: un solo proyecto Maven con un paquete por modulo
  (`catalogo`, `precios`, `ventas`, `accesos`, `facturacion`, `reportes`) y `shared`.
- Dentro de cada modulo, **arquitectura hexagonal**:
  - `domain`: reglas de negocio puras (sin Spring ni JPA).
  - `application`: casos de uso y puertos (interfaces).
  - `infrastructure`: adaptadores (REST, JPA, correo, pasarela de pago).
- La regla de dependencia siempre apunta hacia adentro:
  `infrastructure -> application -> domain`.
- Se verifica con **ArchUnit** (`ArquitecturaTest`).

## Consecuencias
- El dominio se prueba rapido con tests unitarios, sin levantar Spring.
- Cambiar de base de datos o pasarela de pago solo toca los adaptadores.
- Mas clases e interfaces que un CRUD en capas: el costo es deliberado, es parte del aprendizaje.
