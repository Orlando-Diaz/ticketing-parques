import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService, mensajeDeError } from '../api.service';
import { fechaLarga, hoyIso, pesos } from '../formato';
import { Cotizacion, Parque } from '../modelos';

/** Un tipo de entrada del parque (una categoría por edad, o la entrada única) y cuántas lleva la persona. */
interface Linea {
  nombre: string;
  edadMinima: number;
  edadMaxima: number | null;
  precio: number;
  cantidad: number;
  residente: boolean;
  cotizacion: Cotizacion | null;
  error: string | null;
}

@Component({
  selector: 'app-comprar',
  templateUrl: './comprar.html'
})
export class Comprar implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

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

  protected readonly lineas = signal<Linea[]>([]);
  protected readonly hayDescuentoResidente = signal(false);
  protected readonly cargandoEntradas = signal(false);
  protected readonly errorEntradas = signal<string | null>(null);

  protected readonly comprando = signal(false);
  protected readonly errorCompra = signal<string | null>(null);

  protected readonly parque = computed(() => this.parques().find(p => p.id === this.parqueId()) ?? null);
  protected readonly personas = computed(() => this.lineas().reduce((n, l) => n + l.cantidad, 0));
  protected readonly total = computed(() =>
    this.lineas().reduce((suma, l) => suma + l.cantidad * (l.cotizacion?.total ?? 0), 0));
  protected readonly fechaValida = computed(() => this.fecha() >= this.hoy);
  protected readonly hayCupo = computed(() => {
    const d = this.disponibles();
    return d === null || d >= this.personas();
  });
  protected readonly puedeComprar = computed(() =>
    this.parque() !== null && this.fechaValida() && this.personas() > 0 && this.hayCupo() && !this.comprando()
    && this.lineas().every(l => l.cantidad === 0 || l.cotizacion !== null));

  /** Cada vez que algo cambia se vuelve a calcular; solo vale la respuesta de la última petición. */
  private version = 0;

  async ngOnInit(): Promise<void> {
    try {
      const lista = await this.api.parques();
      this.parques.set(lista);
      const pedido = this.ruta.snapshot.queryParamMap.get('parque'); // viene de la pantalla de inicio
      const elegido = lista.find(p => p.id === pedido) ?? (lista.length === 1 ? lista[0] : null);
      if (elegido) {
        await this.elegirParque(elegido.id);
      }
    } catch (e) {
      this.errorParques.set(mensajeDeError(e));
    } finally {
      this.cargandoParques.set(false);
    }
  }

  protected async elegirParque(id: string): Promise<void> {
    this.parqueId.set(id);
    this.lineas.set([]);
    this.errorEntradas.set(null);
    this.cargandoEntradas.set(true);
    try {
      const precios = await this.api.preciosDelParque(id);
      if (this.parqueId() !== id) {
        return; // eligió otro parque mientras tanto
      }
      const categorias = precios.esquema.tipo === 'UNICA'
        ? [{ nombre: 'Entrada', edadMinima: 0, edadMaxima: null, precio: precios.esquema.precio ?? 0 }]
        : (precios.esquema.categorias ?? []);
      this.hayDescuentoResidente.set(precios.reglas.some(r => r.tipo === 'DESCUENTO_RESIDENTE'));
      this.lineas.set(categorias.map(c => ({
        ...c, cantidad: 0, residente: false, cotizacion: null, error: null
      })));
    } catch (e) {
      this.errorEntradas.set(mensajeDeError(e));
    } finally {
      this.cargandoEntradas.set(false);
    }
    await this.actualizar();
  }

  protected cambiarFecha(valor: string): void {
    this.fecha.set(valor);
    void this.actualizar();
  }

  protected cambiarCantidad(indice: number, delta: number): void {
    const disponibles = this.disponibles();
    this.lineas.update(ls => ls.map((l, i) => {
      if (i !== indice) {
        return l;
      }
      const maximo = disponibles === null ? 50 : Math.max(0, disponibles - (this.personas() - l.cantidad));
      return { ...l, cantidad: Math.max(0, Math.min(maximo, l.cantidad + delta)) };
    }));
    void this.actualizar(); // el tamaño del grupo puede cambiar el precio
  }

  protected cambiarResidente(indice: number, residente: boolean): void {
    this.lineas.update(ls => ls.map((l, i) => (i === indice ? { ...l, residente, cotizacion: null } : l)));
    void this.actualizar();
  }

  private async actualizar(): Promise<void> {
    const mia = ++this.version;
    const parqueId = this.parqueId();
    const fecha = this.fecha();
    this.errorCompra.set(null);
    if (!parqueId || !fecha) {
      return;
    }

    const grupo = Math.max(1, this.personas());
    const [disponibilidad, cotizaciones] = await Promise.all([
      this.api.disponibilidad(parqueId, fecha).then(d => ({ ok: d.disponibles }), e => ({ error: mensajeDeError(e) })),
      Promise.all(this.lineas().map(l =>
        this.api.cotizar(parqueId, l.edadMinima, l.residente, grupo, fecha)
          .then(c => ({ cotizacion: c, error: null }), e => ({ cotizacion: null, error: mensajeDeError(e) })))
      )
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
    this.lineas.update(ls => ls.map((l, i) => ({ ...l, ...(cotizaciones[i] ?? {}) })));
  }

  protected edades(l: Linea): string {
    if (l.edadMaxima === null) {
      return l.edadMinima === 0 ? 'Para todas las edades' : `${l.edadMinima} años o más`;
    }
    return `De ${l.edadMinima} a ${l.edadMaxima} años`;
  }

  protected async comprar(): Promise<void> {
    const parqueId = this.parqueId();
    if (!parqueId || !this.puedeComprar()) {
      return;
    }
    this.comprando.set(true);
    this.errorCompra.set(null);
    try {
      // Cada entrada se compra con la edad más baja de su categoría: el precio es el mismo para toda la categoría.
      const visitantes = this.lineas().flatMap(l =>
        Array.from({ length: l.cantidad }, () => ({ edad: l.edadMinima, residente: l.residente })));
      const orden = await this.api.comprar(parqueId, this.fecha(), visitantes);
      await this.router.navigate(['/orden', orden.id]);
    } catch (e) {
      this.errorCompra.set(mensajeDeError(e));
      void this.actualizar(); // el cupo pudo cambiar mientras se decidía
    } finally {
      this.comprando.set(false);
    }
  }
}
