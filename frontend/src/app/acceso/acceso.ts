import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { mensajeDeError } from '../api.service';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-acceso',
  templateUrl: './acceso.html'
})
export class AccesoPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly entrando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected async entrar(evento: Event, usuario: string, clave: string): Promise<void> {
    evento.preventDefault();
    this.entrando.set(true);
    this.error.set(null);
    try {
      await this.auth.iniciarSesion(usuario.trim(), clave);
      const volver = this.ruta.snapshot.queryParamMap.get('volver');
      // Solo rutas internas: evita que un enlace manipulado lleve a otro sitio después del login
      await this.router.navigateByUrl(volver && volver.startsWith('/') && !volver.startsWith('//') ? volver : '/puerta');
    } catch (e) {
      this.error.set(mensajeDeError(e) === 'Tu sesión venció o no es válida. Inicia sesión de nuevo.'
        ? 'Usuario o clave incorrectos.' : mensajeDeError(e));
    } finally {
      this.entrando.set(false);
    }
  }
}
