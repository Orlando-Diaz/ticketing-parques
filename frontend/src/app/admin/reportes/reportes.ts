import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ApiService, mensajeDeError } from '../../api.service';
import { hoyIso, pesos } from '../../formato';
import { Parque, ResumenDelDia } from '../../modelos';
import { GraficaBarras } from './grafica-barras';

const DIAS_MAXIMOS = 366;

function sumarDias(iso: string, dias: number): string {
  const [a, m, d] = iso.split('-').map(Number);
  const fecha = new Date(a, m - 1, d + dias);
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
}

const diasEntre = (desde: string, hasta: string): number =>
  Math.round((Date.parse(hasta) - Date.parse(desde)) / 86_400_000) + 1;

/** "2026-12-05" -> "5 dic" */
function diaCorto(iso: string): string {
  const [, m, d] = iso.split('-').map(Number);
  return `${d} ${['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'][m - 1]}`;
}

/** 1500000 -> "1,5 M", 45000 -> "45 mil" (para que el eje no se llene de ceros). */
function pesosCompactos(n: number): string {
  if (n >= 1_000_000) {
    return `${String(+(n / 1_000_000).toFixed(1)).replace('.', ',')} M`;
  }
  return n >= 1000 ? `${Math.round(n / 1000)} mil` : String(n);
}

@Component({
  selector: 'app-reportes-admin',
  imports: [GraficaBarras],
  templateUrl: './reportes.html'
})
export class ReportesAdmin implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly pesos = pesos;
  protected readonly diaCorto = diaCorto;
  protected readonly pesosCompactos = pesosCompactos;
  protected readonly numero = (n: number): string => new Intl.NumberFormat('es-CO').format(n);

  protected readonly parques = signal<Parque[]>([]);
  protected readonly parqueId = signal('');
  protected readonly desde = signal(hoyIso());
  protected readonly hasta = signal(sumarDias(hoyIso(), 30));

  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly dias = signal<ResumenDelDia[] | null>(null);

  protected readonly etiquetas = computed(() => (this.dias() ?? []).map(d => d.fecha));
  protected readonly serieVentas = computed(() => [{ nombre: 'Ventas', valores: (this.dias() ?? []).map(d => d.ventas) }]);
  protected readonly serieEntradas = computed(() => [
    { nombre: 'Vendidas', valores: (this.dias() ?? []).map(d => d.entradasVendidas) },
    { nombre: 'Ingresaron', valores: (this.dias() ?? []).map(d => d.ingresaron) }
  ]);

  protected readonly totales = computed(() => {
    const lista = this.dias() ?? [];
    const suma = (f: (d: ResumenDelDia) => number) => lista.reduce((t, d) => t + f(d), 0);
    return {
      ventas: suma(d => d.ventas),
      vendidas: suma(d => d.entradasVendidas),
      canceladas: suma(d => d.entradasCanceladas),
      ingresaron: suma(d => d.ingresaron),
      sinIngresar: suma(d => d.sinIngresar)
    };
  });

  protected readonly puertas = computed(() => {
    const acumulado = new Map<string, number>();
    for (const d of this.dias() ?? []) {
      for (const [puerta, personas] of Object.entries(d.ingresosPorPuerta ?? {})) {
        acumulado.set(puerta, (acumulado.get(puerta) ?? 0) + personas);
      }
    }
    return [...acumulado.entries()].sort((a, b) => b[1] - a[1]);
  });

  async ngOnInit(): Promise<void> {
    try {
      const lista = await this.api.parques();
      this.parques.set(lista);
      if (lista.length > 0) {
        this.parqueId.set(lista[0].id);
        await this.consultar();
      }
    } catch (e) {
      this.error.set(mensajeDeError(e));
    }
  }

  protected async consultar(): Promise<void> {
    this.error.set(null);
    const desde = this.desde();
    const hasta = this.hasta();
    if (!this.parqueId() || !desde || !hasta) {
      this.error.set('Elige un parque y las dos fechas.');
      return;
    }
    if (hasta < desde) {
      this.error.set('La fecha final no puede ser anterior a la inicial.');
      return;
    }
    if (diasEntre(desde, hasta) > DIAS_MAXIMOS) {
      this.error.set(`El rango no puede pasar de ${DIAS_MAXIMOS} días.`);
      return;
    }
    this.cargando.set(true);
    try {
      this.dias.set(await this.api.reporteDelRango(this.parqueId(), desde, hasta));
    } catch (e) {
      this.dias.set(null);
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
  }
}
