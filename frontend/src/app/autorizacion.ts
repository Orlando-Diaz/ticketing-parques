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

/** Solo administradores; la gente de la puerta vuelve a su pantalla y quien no inició sesión va al login. */
export const soloAdmin: CanActivateFn = (_ruta, estado) => {
  const sesion = inject(AuthService).sesion();
  const router = inject(Router);
  if (sesion?.rol === 'ADMIN') {
    return true;
  }
  if (sesion) {
    return router.createUrlTree(['/puerta']);
  }
  return router.createUrlTree(['/acceso'], { queryParams: { volver: estado.url } });
};

/** Solo deja entrar al personal de la puerta (y a los administradores); el resto va al login. */
export const soloPersonal: CanActivateFn = (_ruta, estado) => {
  const sesion = inject(AuthService).sesion();
  if (sesion) {
    return true;
  }
  return inject(Router).createUrlTree(['/acceso'], { queryParams: { volver: estado.url } });
};
