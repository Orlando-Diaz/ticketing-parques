import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService, mensajeDeError } from '../api.service';
import { fechaLarga, hoyIso, pesos } from '../formato';
import { Cotizacion, Parque } from '../modelos';

interface Fila {
  edad: number | null;
  residente: boolean;
  cotizacion: Cotizacion | null;
  error: string | null;
}

const nuevaFila = (): Fila => ({ edad: null, residente: false, cotizacion: null, error: null });
const edadValida = (e: number | null): e is number => e !== null && Number.isInteger(e) && e >= 0 && e <= 120;

@Component({
  selector: 'app-comprar',
  templateUrl: './comprar.html'
})
export class Comprar implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  protected readonly pesos = pesos;
  protected readonly fechaLarga = fechaLarga;
  protected readonly hoy = hoyIso();

  protected readonly parques = signal<Parque[]>([]);
  protected readonly cargandoParques = signal(true);
  protected readonly errorParques = signal<string | null>(null);

  protected readonly parqueId = signal<string | null>(null);
  protected readonly fecha = signal(hoyIso());
  protected readonly disponibles = signal<number | null>(null);
  protected readonly errorDisponibilidad = signal<string | null>(null);
  protected readonly filas = signal<Fila[]>([nuevaFila()]);

  protected readonly comprando = signal(false);
  protected readonly errorCompra = signal<string | null>(null);

  protected readonly parque = computed(() => this.parques().find(p => p.id === this.parqueId()) ?? null);
  protected readonly total = computed(() => this.filas().reduce((suma, f) => suma + (f.cotizacion?.total ?? 0), 0));
  protected readonly fechaValida = computed(() => this.fecha() >= this.hoy);
  protected readonly hayCupo = computed(() => {
    const d = this.disponibles();
    return d === null || d >= this.filas().length;
  });
  protected readonly puedeComprar = computed(() =>
    this.parque() !== null && this.fechaValida() && this.hayCupo() && !this.comprando()
    && this.filas().every(f => f.cotizacion !== null));

  /** Cada vez que algo cambia se vuelve a calcular; solo vale la respuesta de la última petición. */
  private version = 0;
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  async ngOnInit(): Promise<void> {
    try {
      const lista = await this.api.parques();
      this.parques.set(lista);
      if (lista.length === 1) {
        this.parqueId.set(lista[0].id);
        void this.actualizar();
      }
    } catch (e) {
      this.errorParques.set(mensajeDeError(e));
    } finally {
      this.cargandoParques.set(false);
    }
  }

  protected elegirParque(id: string): void {
    this.parqueId.set(id);
    void this.actualizar();
  }

  protected cambiarFecha(valor: string): void {
    this.fecha.set(valor);
    void this.actualizar();
  }

  protected cambiarEdad(indice: number, texto: string): void {
    const edad = texto === '' ? null : Number(texto);
    this.filas.update(fs => fs.map((f, i) => i === indice ? { ...f, edad, cotizacion: null, error: null } : f));
    this.programarActualizacion(); // espera a que termine de escribir
  }

  protected cambiarResidente(indice: number, residente: boolean): void {
    this.filas.update(fs => fs.map((f, i) => i === indice ? { ...f, residente, cotizacion: null, error: null } : f));
    void this.actualizar();
  }

  protected agregar(): void {
    this.filas.update(fs => [...fs, nuevaFila()]);
  }

  protected quitar(indice: number): void {
    this.filas.update(fs => fs.filter((_, i) => i !== indice));
    void this.actualizar(); // el tamaño del grupo puede cambiar el precio
  }

  private programarActualizacion(): void {
    clearTimeout(this.temporizador);
    this.temporizador = setTimeout(() => void this.actualizar(), 350);
  }

  private async actualizar(): Promise<void> {
    clearTimeout(this.temporizador);
    const mia = ++this.version;
    const parqueId = this.parqueId();
    const fecha = this.fecha();
    this.errorCompra.set(null);
    if (!parqueId || !fecha) {
      return;
    }

    const grupo = this.filas().length;
    const [disponibilidad, cotizaciones] = await Promise.all([
      this.api.disponibilidad(parqueId, fecha).then(d => ({ ok: d.disponibles }), e => ({ error: mensajeDeError(e) })),
      Promise.all(this.filas().map(f => edadValida(f.edad)
        ? this.api.cotizar(parqueId, f.edad, f.residente, grupo, fecha)
            .then(c => ({ cotizacion: c, error: null }), e => ({ cotizacion: null, error: mensajeDeError(e) }))
        : Promise.resolve({ cotizacion: null, error: null })))
    ]);
    if (mia !== this.version) {
      return; // llegó tarde: ya hay una petición más nueva
    }

    if ('ok' in disponibilidad) {
      this.disponibles.set(disponibilidad.ok);
      this.errorDisponibilidad.set(null);
    } else {
      this.disponibles.set(null);
      this.errorDisponibilidad.set(disponibilidad.error);
    }
    this.filas.update(fs => fs.map((f, i) => ({ ...f, ...cotizaciones[i] })));
  }

  protected async comprar(): Promise<void> {
    const parqueId = this.parqueId();
    if (!parqueId || !this.puedeComprar()) {
      return;
    }
    this.comprando.set(true);
    this.errorCompra.set(null);
    try {
      const orden = await this.api.comprar(parqueId, this.fecha(),
        this.filas().map(f => ({ edad: f.edad as number, residente: f.residente })));
      await this.router.navigate(['/orden', orden.id]);
    } catch (e) {
      this.errorCompra.set(mensajeDeError(e));
      void this.actualizar(); // el cupo pudo cambiar mientras se decidía
    } finally {
      this.comprando.set(false);
    }
  }
}
