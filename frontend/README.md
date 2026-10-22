# Frontend (Angular)

Aplicación web de la boletería: el visitante elige parque, fecha y personas, ve el precio al instante, compra y recibe su boleto con código QR.

## Requisitos
Node.js 20.19+, 22.12+ o 24+ (Angular 21) y la API corriendo en `http://localhost:8080` (ver README de la raíz).

## Comandos
```powershell
npm install
npm start      # http://localhost:4200
npm run build  # genera dist/frontend
```
La dirección de la API se cambia en `src/app/config.ts`. Si la web se sirve desde otro origen que `http://localhost:4200`, ese origen debe estar en la variable `CORS_ORIGENES` de la API.

## Estructura
- `src/app/comprar`: formulario de compra con la cotización en vivo y el boleto "en construcción".
- `src/app/orden`: el boleto comprado (QR, detalle, cancelar).
- `src/app/api.service.ts`: todas las llamadas HTTP.
