import { Component, computed, input, signal } from '@angular/core';

export interface SerieDeBarras {
  nombre: string;
  /** Un valor por día, en el mismo orden que `dias`. */
  valores: number[];
}

const ANCHO = 480;
const ALTO = 240;
const MARGEN = { izq: 50, der: 12, arriba: 12, abajo: 34 };
const BARRA_MAXIMA = 24;
const SEPARACION = 2;

/** Paso "redondo" (1, 2 o 5 por una potencia de 10) para que el eje marque 0, 50, 100… y no 0, 37, 74. */
function marcasDelEje(maximo: number): number[] {
  const objetivo = Math.max(maximo, 1) / 4;
  const potencia = Math.pow(10, Math.floor(Math.log10(objetivo)));
  const paso = [1, 2, 5, 10].map(m => m * potencia).find(p => p >= objetivo) ?? potencia * 10;
  const marcas: number[] = [];
  for (let v = 0; v <= maximo + paso * 0.999; v += paso) {
    marcas.push(v);
  }
  return marcas;
}

/** Barra con las dos esquinas de arriba redondeadas y la base recta, apoyada en el eje. */
function trazoDeBarra(x: number, y: number, ancho: number, alto: number): string {
  const r = Math.min(4, ancho / 2, alto);
  return `M${x},${y + alto}V${y + r}Q${x},${y} ${x + r},${y}H${x + ancho - r}Q${x + ancho},${y} ${x + ancho},${y + r}V${y + alto}Z`;
}

@Component({
  selector: 'app-grafica-barras',
  template: `
    <figure class="grafica">
      <figcaption>
        <h3>{{ titulo() }}</h3>
        @if (series().length > 1) {
          <ul class="leyenda" aria-label="Series">
            @for (s of series(); track s.nombre; let i = $index) {
              <li><span class="muestra serie-{{ i + 1 }}" aria-hidden="true"></span>{{ s.nombre }}</li>
            }
          </ul>
        }
      </figcaption>

      <svg [attr.viewBox]="'0 0 ' + ancho + ' ' + alto" role="img" [attr.aria-label]="descripcion()"
           (pointerleave)="activo.set(null)">
        @for (m of marcas(); track m.valor) {
          <line class="rejilla" [attr.x1]="margen.izq" [attr.x2]="ancho - margen.der" [attr.y1]="m.y" [attr.y2]="m.y" />
          <text class="eje" [attr.x]="margen.izq - 8" [attr.y]="m.y + 4" text-anchor="end">{{ m.etiqueta }}</text>
        }
        @for (g of grupos(); track g.indice) {
          @if (activo() === g.indice) {
            <rect class="resalte" [attr.x]="g.bandaX" [attr.y]="margen.arriba" [attr.width]="g.bandaAncho" [attr.height]="alto - margen.arriba - margen.abajo" />
          }
          @for (b of g.barras; track $index) {
            <path class="barra serie-{{ b.serie + 1 }}" [attr.d]="b.trazo" />
          }
          @if (g.mostrarEtiqueta) {
            <text class="eje" [attr.x]="g.centro" [attr.y]="alto - 11" text-anchor="middle">{{ g.etiqueta }}</text>
          }
          <rect class="zona" [attr.x]="g.bandaX" [attr.y]="margen.arriba" [attr.width]="g.bandaAncho"
                [attr.height]="alto - margen.arriba - margen.abajo" (pointerenter)="activo.set(g.indice)" />
        }
        <line class="base" [attr.x1]="margen.izq" [attr.x2]="ancho - margen.der" [attr.y1]="alto - margen.abajo" [attr.y2]="alto - margen.abajo" />
      </svg>

      <p class="lectura" aria-live="off">
        @if (lectura(); as l) {
          <strong>{{ l.dia }}</strong>
          @for (v of l.valores; track v.nombre; let i = $index) {
            <span><span class="muestra serie-{{ i + 1 }}" aria-hidden="true"></span>{{ v.nombre }}: {{ v.texto }}</span>
          }
        } @else {
          Pasa el cursor sobre un día para ver sus valores.
        }
      </p>
    </figure>
  `
})
export class GraficaBarras {
  readonly titulo = input.required<string>();
  readonly dias = input.required<string[]>();
  readonly series = input.required<SerieDeBarras[]>();
  readonly formato = input<(n: number) => string>(n => String(n));
  readonly formatoEje = input<(n: number) => string>(n => String(n));
  readonly formatoDia = input<(iso: string) => string>(d => d);

  protected readonly ancho = ANCHO;
  protected readonly alto = ALTO;
  protected readonly margen = MARGEN;
  protected readonly activo = signal<number | null>(null);

  private readonly maximo = computed(() => Math.max(0, ...this.series().flatMap(s => s.valores)));
  private readonly ejeY = computed(() => marcasDelEje(this.maximo()));
  private readonly topeY = computed(() => this.ejeY()[this.ejeY().length - 1] || 1);

  private y(valor: number): number {
    const util = ALTO - MARGEN.arriba - MARGEN.abajo;
    return ALTO - MARGEN.abajo - (valor / this.topeY()) * util;
  }

  protected readonly marcas = computed(() => this.ejeY().map(v => ({
    valor: v, y: this.y(v), etiqueta: this.formatoEje()(v)
  })));

  protected readonly grupos = computed(() => {
    const dias = this.dias();
    const series = this.series();
    const banda = (ANCHO - MARGEN.izq - MARGEN.der) / Math.max(dias.length, 1);
    const k = series.length;
    const anchoGrupo = Math.min(banda * 0.8, k * BARRA_MAXIMA + (k - 1) * SEPARACION);
    const anchoBarra = (anchoGrupo - (k - 1) * SEPARACION) / k;
    const cadaCuanto = Math.ceil(dias.length / 8);
    return dias.map((dia, i) => {
      const bandaX = MARGEN.izq + i * banda;
      const inicio = bandaX + (banda - anchoGrupo) / 2;
      return {
        indice: i,
        etiqueta: this.formatoDia()(dia),
        mostrarEtiqueta: i % cadaCuanto === 0,
        centro: bandaX + banda / 2,
        bandaX,
        bandaAncho: banda,
        barras: series.flatMap((s, j) => {
          const alto = ALTO - MARGEN.abajo - this.y(s.valores[i]);
          return alto > 0
            ? [{ serie: j, trazo: trazoDeBarra(inicio + j * (anchoBarra + SEPARACION), this.y(s.valores[i]), anchoBarra, alto) }]
            : [];
        })
      };
    });
  });

  protected readonly lectura = computed(() => {
    const i = this.activo();
    if (i === null || i >= this.dias().length) {
      return null;
    }
    return {
      dia: this.formatoDia()(this.dias()[i]),
      valores: this.series().map(s => ({ nombre: s.nombre, texto: this.formato()(s.valores[i]) }))
    };
  });

  protected readonly descripcion = computed(() =>
    `${this.titulo()}: ${this.dias().length} días, ${this.series().map(s => s.nombre).join(' y ')}. El detalle está en la tabla.`);
}
