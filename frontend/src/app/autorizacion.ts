import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { tap } from 'rxjs';
import { AuthService } from './auth.service';
import { API_URL } from './config';

/** Agrega el token a las llamadas a NUESTRA API (nunca a otros sitios) y cierra la sesión si el servidor la rechaza. */
export const tokenInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const propia = peticion.url.startsWith(API_URL);
  const conToken = token && propia ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : peticion;
  return siguiente(conToken).pipe(
    tap({
      error: e => {
        if (token && propia && auth.tokenRechazado(e) && !peticion.url.endsWith('/api/auth/login')) {
          auth.cerrarSesion();
        }
      }
    }));
};

/** Deja pasar a los roles indicados; quien no inició sesión va al login y quien tiene otro rol, a su propio inicio. */
function permitir(...roles: string[]): CanActivateFn {
  return (_ruta, estado) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const sesion = auth.sesion();
    if (sesion && roles.includes(sesion.rol)) {
      return true;
    }
    if (sesion) {
      return router.parseUrl(auth.inicioDelRol());
    }
    return router.createUrlTree(['/acceso'], { queryParams: { volver: estado.url } });
  };
}

/** Solo administradores. */
export const soloAdmin = permitir('ADMIN');

/** El personal de la puerta y los administradores. */
export const soloPersonal = permitir('ADMIN', 'PUERTA');

/** Quien puede comprar y tener perfil: clientes (y administradores). */
export const soloCliente = permitir('CLIENTE', 'ADMIN');
