import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { mensajeDeError } from '../api.service';
import { AuthService } from '../auth.service';

/** Una sola pantalla para todos: clientes (iniciar sesión o crear cuenta), administradores y personal de la puerta. */
@Component({
  selector: 'app-acceso',
  templateUrl: './acceso.html'
})
export class AccesoPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly modo = signal<'entrar' | 'crear'>('entrar');
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    if (this.ruta.snapshot.queryParamMap.has('crear')) {
      this.modo.set('crear');
    }
    if (this.auth.sesion()) {
      void this.irAlDestino(); // ya tiene sesión: no tiene sentido ver el formulario
    }
  }

  protected cambiarModo(modo: 'entrar' | 'crear'): void {
    this.modo.set(modo);
    this.error.set(null);
  }

  protected async entrar(evento: Event, usuario: string, clave: string): Promise<void> {
    evento.preventDefault();
    await this.enviar(
      () => this.auth.iniciarSesion(usuario.trim(), clave),
      'Usuario o clave incorrectos.');
  }

  protected async crear(evento: Event, nombre: string, correo: string, clave: string, repetida: string): Promise<void> {
    evento.preventDefault();
    if (clave !== repetida) {
      this.error.set('Las dos claves no coinciden.');
      return;
    }
    await this.enviar(() => this.auth.registrar(nombre.trim(), correo.trim(), clave), null);
  }

  private async enviar(accion: () => Promise<void>, errorDeCredenciales: string | null): Promise<void> {
    this.enviando.set(true);
    this.error.set(null);
    try {
      await accion();
      await this.irAlDestino();
    } catch (e) {
      const mensaje = mensajeDeError(e);
      this.error.set(errorDeCredenciales && mensaje === 'Tu sesión venció o no es válida. Inicia sesión de nuevo.'
        ? errorDeCredenciales : mensaje);
    } finally {
      this.enviando.set(false);
    }
  }

  private async irAlDestino(): Promise<void> {
    const volver = this.ruta.snapshot.queryParamMap.get('volver');
    // Solo rutas internas: evita que un enlace manipulado lleve a otro sitio después del login
    const valido = volver && volver.startsWith('/') && !volver.startsWith('//');
    await this.router.navigateByUrl(valido ? volver : this.auth.inicioDelRol());
  }
}
