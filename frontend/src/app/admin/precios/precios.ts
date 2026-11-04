import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService, mensajeDeError } from '../../api.service';
import { hoyIso, pesos } from '../../formato';
import { ConfiguracionDePrecios, Cotizacion, Parque } from '../../modelos';

interface CategoriaForm {
  nombre: string;
  edadMinima: number | null;
  edadMaxima: number | null; // null = sin límite
  precio: number | null;
}

interface ReglasForm {
  residenteActiva: boolean; residentePorcentaje: number;
  grupoActiva: boolean; grupoTamano: number; grupoPorcentaje: number;
  findeActiva: boolean; findePorcentaje: number;
}

const numero = (valor: string): number | null => (valor.trim() === '' ? null : Number(valor));

@Component({
  selector: 'app-precios-admin',
  imports: [RouterLink],
  templateUrl: './precios.html'
})
export class PreciosAdmin implements OnInit {
  private readonly api = inject(ApiService);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly pesos = pesos;
  protected readonly parqueId = this.ruta.snapshot.paramMap.get('id') ?? '';
  protected readonly parque = signal<Parque | null>(null);

  protected readonly tipo = signal<'UNICA' | 'POR_CATEGORIA'>('POR_CATEGORIA');
  protected readonly precioUnico = signal<number | null>(null);
  protected readonly categorias = signal<CategoriaForm[]>([
    { nombre: 'Niño', edadMinima: 0, edadMaxima: 10, precio: null },
    { nombre: 'Adulto', edadMinima: 11, edadMaxima: 59, precio: null },
    { nombre: 'Adulto mayor', edadMinima: 60, edadMaxima: null, precio: null }
  ]);
  protected readonly reglas = signal<ReglasForm>({
    residenteActiva: false, residentePorcentaje: 20,
    grupoActiva: false, grupoTamano: 10, grupoPorcentaje: 15,
    findeActiva: false, findePorcentaje: 10
  });

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly exito = signal(false);

  protected readonly prueba = signal({ edad: 30, residente: false, grupo: 1, fecha: hoyIso() });
  protected readonly cotizacion = signal<Cotizacion | null>(null);
  protected readonly errorPrueba = signal<string | null>(null);

  protected readonly nombreDelParque = computed(() => this.parque()?.nombre ?? this.parqueId);

  async ngOnInit(): Promise<void> {
    try {
      this.parque.set((await this.api.parques()).find(p => p.id === this.parqueId) ?? null);
    } catch {
      // el nombre es solo un adorno: si falla, se muestra el identificador
    }
    await this.cargarConfiguracionActual();
  }

  /** Si el parque ya tiene precios, el formulario arranca con ellos; si no (404), se queda con los de ejemplo. */
  private async cargarConfiguracionActual(): Promise<void> {
    try {
      const actual = await this.api.preciosDelParque(this.parqueId);
      if (actual.esquema.tipo === 'UNICA') {
        this.tipo.set('UNICA');
        this.precioUnico.set(actual.esquema.precio ?? null);
      } else {
        this.tipo.set('POR_CATEGORIA');
        this.categorias.set((actual.esquema.categorias ?? []).map(c => ({ ...c })));
      }
      const regla = (tipo: string) => actual.reglas.find(r => r.tipo === tipo);
      const residente = regla('DESCUENTO_RESIDENTE');
      const grupo = regla('DESCUENTO_GRUPO');
      const finde = regla('RECARGO_FIN_DE_SEMANA');
      this.reglas.update(r => ({
        residenteActiva: !!residente, residentePorcentaje: residente?.porcentaje ?? r.residentePorcentaje,
        grupoActiva: !!grupo, grupoTamano: grupo?.tamanoMinimo ?? r.grupoTamano,
        grupoPorcentaje: grupo?.porcentaje ?? r.grupoPorcentaje,
        findeActiva: !!finde, findePorcentaje: finde?.porcentaje ?? r.findePorcentaje
      }));
    } catch {
      // sin configuración todavía: se usan los valores de ejemplo
    }
  }

  protected cambiarTipo(valor: string): void {
    this.tipo.set(valor === 'UNICA' ? 'UNICA' : 'POR_CATEGORIA');
  }

  protected editarCategoria(indice: number, parche: Partial<CategoriaForm>): void {
    this.categorias.update(lista => lista.map((c, i) => (i === indice ? { ...c, ...parche } : c)));
  }

  protected agregarCategoria(): void {
    this.categorias.update(lista => [...lista, { nombre: '', edadMinima: null, edadMaxima: null, precio: null }]);
  }

  protected quitarCategoria(indice: number): void {
    this.categorias.update(lista => lista.filter((_, i) => i !== indice));
  }

  protected editarReglas(parche: Partial<ReglasForm>): void {
    this.reglas.update(r => ({ ...r, ...parche }));
  }

  protected numeroDe(valor: string): number | null {
    return numero(valor);
  }

  protected editarPrueba(parche: Partial<{ edad: number; residente: boolean; grupo: number; fecha: string }>): void {
    this.prueba.update(p => ({ ...p, ...parche }));
  }

  private construir(): ConfiguracionDePrecios | string {
    const r = this.reglas();
    const reglas: ConfiguracionDePrecios['reglas'] = [];
    if (r.residenteActiva) reglas.push({ tipo: 'DESCUENTO_RESIDENTE', porcentaje: r.residentePorcentaje });
    if (r.grupoActiva) reglas.push({ tipo: 'DESCUENTO_GRUPO', tamanoMinimo: r.grupoTamano, porcentaje: r.grupoPorcentaje });
    if (r.findeActiva) reglas.push({ tipo: 'RECARGO_FIN_DE_SEMANA', porcentaje: r.findePorcentaje });

    if (this.tipo() === 'UNICA') {
      const precio = this.precioUnico();
      if (precio === null || !(precio > 0)) {
        return 'Escribe el precio de la entrada (mayor que cero).';
      }
      return { esquema: { tipo: 'UNICA', precio }, reglas };
    }
    const lista = this.categorias();
    if (lista.length === 0) {
      return 'Agrega al menos una categoría.';
    }
    for (const c of lista) {
      if (!c.nombre.trim() || c.edadMinima === null || c.precio === null || !(c.precio > 0)) {
        return 'Cada categoría necesita nombre, edad mínima y un precio mayor que cero.';
      }
      if (c.edadMaxima !== null && c.edadMaxima < c.edadMinima) {
        return `En "${c.nombre}" la edad máxima no puede ser menor que la mínima.`;
      }
    }
    return {
      esquema: {
        tipo: 'POR_CATEGORIA',
        categorias: lista.map(c => ({
          nombre: c.nombre.trim(), edadMinima: c.edadMinima as number,
          edadMaxima: c.edadMaxima, precio: c.precio as number
        }))
      },
      reglas
    };
  }

  protected async guardar(evento: Event): Promise<void> {
    evento.preventDefault();
    this.error.set(null);
    this.exito.set(false);
    const configuracion = this.construir();
    if (typeof configuracion === 'string') {
      this.error.set(configuracion);
      return;
    }
    this.guardando.set(true);
    try {
      await this.api.configurarPrecios(this.parqueId, configuracion);
      this.exito.set(true);
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async probar(): Promise<void> {
    const p = this.prueba();
    this.errorPrueba.set(null);
    this.cotizacion.set(null);
    try {
      this.cotizacion.set(await this.api.cotizar(this.parqueId, p.edad, p.residente, p.grupo, p.fecha));
    } catch (e) {
      this.errorPrueba.set(mensajeDeError(e));
    }
  }
}
