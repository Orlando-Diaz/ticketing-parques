import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, mensajeDeError } from '../../api.service';
import { Parque } from '../../modelos';

/** "Parque Agropecuario Nacional" -> "parque-agropecuario-nacional" (sin tildes ni símbolos). */
export function identificadorDesde(nombre: string): string {
  return nombre.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
}

@Component({
  selector: 'app-parques-admin',
  imports: [RouterLink],
  templateUrl: './parques.html'
})
export class ParquesAdmin implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly parques = signal<Parque[]>([]);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly exito = signal<string | null>(null);
  protected readonly identificador = signal('');
  private identificadorTocado = false;

  async ngOnInit(): Promise<void> {
    await this.cargar();
  }

  private async cargar(): Promise<void> {
    try {
      this.parques.set(await this.api.parques());
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  protected escribioNombre(nombre: string): void {
    if (!this.identificadorTocado) {
      this.identificador.set(identificadorDesde(nombre));
    }
  }

  protected escribioIdentificador(valor: string): void {
    this.identificadorTocado = true;
    this.identificador.set(identificadorDesde(valor));
  }

  protected async crear(evento: Event, nombre: string, cupo: string): Promise<void> {
    evento.preventDefault();
    const cupoDiario = Number(cupo);
    const id = this.identificador();
    this.error.set(null);
    this.exito.set(null);
    if (!nombre.trim() || !id) {
      this.error.set('Escribe el nombre del parque.');
      return;
    }
    if (!Number.isInteger(cupoDiario) || cupoDiario < 1) {
      this.error.set('El cupo diario debe ser un número entero mayor que cero.');
      return;
    }
    this.guardando.set(true);
    try {
      const creado = await this.api.crearParque(id, nombre.trim(), cupoDiario);
      this.exito.set(`Parque "${creado.nombre}" creado. Ahora configura sus precios.`);
      this.identificador.set('');
      this.identificadorTocado = false;
      (evento.target as HTMLFormElement).reset();
      await this.cargar();
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.guardando.set(false);
    }
  }
}
