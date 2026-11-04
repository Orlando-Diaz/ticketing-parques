import { Component, inject, signal } from '@angular/core';
import { ApiService, mensajeDeError } from '../../api.service';

@Component({
  selector: 'app-usuarios-admin',
  templateUrl: './usuarios.html'
})
export class UsuariosAdmin {
  private readonly api = inject(ApiService);

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly creado = signal<string | null>(null);

  protected async crear(evento: Event, nombre: string, clave: string, rol: string): Promise<void> {
    evento.preventDefault();
    this.error.set(null);
    this.creado.set(null);
    if (clave.length < 10) {
      this.error.set('La clave debe tener al menos 10 caracteres.');
      return;
    }
    this.guardando.set(true);
    try {
      const usuario = await this.api.crearUsuario(nombre.trim(), clave, rol);
      this.creado.set(`Usuario "${usuario.nombre}" creado con rol ${usuario.rol === 'PUERTA' ? 'puerta' : 'administrador'}.`);
      (evento.target as HTMLFormElement).reset();
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
}
