# ADR 0008 - Seguridad: JWT con roles, como modulo propio

- Fecha: 2026-10-21
- Estado: aceptada

## Contexto
Hasta ahora toda la API era abierta: cualquiera podia crear parques, cambiar precios, ver reportes o "dejar entrar"
gente con un pase. El visitante, en cambio, si debe poder consultar, comprar y ver lo suyo sin cuenta.

## Decision
- **Modulo `seguridad`** con la misma estructura hexagonal: `Usuario` y `Rol` (dominio), casos de uso `CrearUsuario` e
  `IniciarSesion` con puertos (`RepositorioDeUsuarios`, `CifradorDeClaves`, `EmisorDeTokens`) y adaptadores
  (JDBC, BCrypt, JWT). El resto del negocio NO conoce este modulo y el no conoce al negocio (ArchUnit lo vigila).
- **Autenticacion con JWT firmado (HS256).** `POST /api/auth/login` cambia usuario y clave por un token que vence en
  60 minutos (configurable). Spring Security valida el token en cada peticion (modo "resource server"); no hay
  sesiones ni cookies, por eso se desactiva CSRF.
- **Roles y rutas** (todo en `SeguridadConfig`, en un solo lugar):
  - Publico: login, consultar disponibilidad y cotizacion, comprar, ver/cancelar la propia orden, su factura y su
    pase. El id de la orden es un UUID imposible de adivinar: funciona como "llave" de lo que es del visitante.
  - `PUERTA` y `ADMIN`: `/api/accesos/**` (validar QR, consultar un pase).
  - `ADMIN`: parques, precios, reportes y usuarios.
  - Cualquier ruta nueva que no se liste exige al menos estar autenticado: no queda abierta por olvido.
- **Claves**: BCrypt (hash lento con sal por clave), minimo 10 caracteres. Nunca se guardan ni se devuelven en claro.
  Usuario inexistente y clave incorrecta dan el mismo error y mensaje (no se revela que usuarios existen).
- **Secretos solo por entorno**: `JWT_SECRET` (>= 32 caracteres, obligatoria: sin ella la app no arranca) y, opcional,
  `ADMIN_USER` / `ADMIN_PASSWORD` para crear al administrador inicial al arrancar. Nada de eso va al repositorio; los
  tests usan valores de prueba propios en `application-test.yml`.

## Consecuencias
- Un token robado sirve hasta que venza; no hay revocacion ni "refresh token". Para una API real se agregaria un
  token de renovacion y una lista de revocados, o claves rotativas.
- HS256 usa un solo secreto para firmar y verificar: sirve con un unico servicio. Con varios servicios se pasaria a
  RS256 (clave privada firma, publica verifica).
- No hay limite de intentos de login (fuerza bruta): pendiente (rate limiting / bloqueo temporal).
- Los roles estan en el token: cambiar el rol de alguien se nota cuando vence su token.
- Compra y consulta publicas implican que cualquiera puede comprar (y cancelar con el id): aceptable para este
  alcance; un sistema real exigiria cuenta del comprador o un codigo de cancelacion.
