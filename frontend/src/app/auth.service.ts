import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_URL } from './config';

export interface Sesion {
  usuario: string;
  rol: 'ADMIN' | 'PUERTA';
  expira: number; // segundos desde 1970
}

const CLAVE = 'boleteria.token';

function leerGuardado(): string | null {
  try {
    return sessionStorage.getItem(CLAVE);
  } catch {
    return null; // modo privado o almacenamiento bloqueado: la sesión dura solo mientras la página esté abierta
  }
}

/** El token es un JWT: su parte central (payload) trae usuario, rol y vencimiento. Solo se lee, la firma la valida el servidor. */
function leerSesion(token: string): Sesion | null {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = new TextDecoder().decode(Uint8Array.from(atob(base64), c => c.charCodeAt(0)));
    const datos = JSON.parse(json);
    if ((datos.rol !== 'ADMIN' && datos.rol !== 'PUERTA') || typeof datos.sub !== 'string' || typeof datos.exp !== 'number') {
      return null;
    }
    return { usuario: datos.sub, rol: datos.rol, expira: datos.exp };
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly guardado = signal<string | null>(leerGuardado());

  /** La sesión vigente, o null si no hay token o ya venció. */
  readonly sesion = computed<Sesion | null>(() => {
    const token = this.guardado();
    const sesion = token ? leerSesion(token) : null;
    return sesion && sesion.expira * 1000 > Date.now() ? sesion : null;
  });

  readonly token = computed(() => (this.sesion() ? this.guardado() : null));

  async iniciarSesion(usuario: string, clave: string): Promise<void> {
    const respuesta = await firstValueFrom(
      this.http.post<{ token: string }>(`${API_URL}/api/auth/login`, { usuario, clave }));
    try {
      sessionStorage.setItem(CLAVE, respuesta.token);
    } catch {
      // sin almacenamiento: la sesión queda solo en memoria
    }
    this.guardado.set(respuesta.token);
  }

  cerrarSesion(): void {
    try {
      sessionStorage.removeItem(CLAVE);
    } catch {
      // nada que limpiar
    }
    this.guardado.set(null);
  }

  /** El servidor rechazó el token (venció o no vale): se descarta. */
  tokenRechazado(error: unknown): boolean {
    return error instanceof HttpErrorResponse && error.status === 401;
  }
}
