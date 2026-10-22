import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toDataURL } from 'qrcode';
import { ApiService, mensajeDeError } from '../api.service';
import { fechaLarga, pesos } from '../formato';
import { Orden, Parque, Pase } from '../modelos';

@Component({
  selector: 'app-orden',
  imports: [RouterLink],
  templateUrl: './orden.html'
})
export class OrdenPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly pesos = pesos;
  protected readonly fechaLarga = fechaLarga;

  protected readonly orden = signal<Orden | null>(null);
  protected readonly pase = signal<Pase | null>(null);
  protected readonly nombreDelParque = signal<string | null>(null);
  protected readonly qr = signal<string | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly errorCancelar = signal<string | null>(null);
  protected readonly confirmandoCancelacion = signal(false);
  protected readonly cancelando = signal(false);

  private id = '';

  async ngOnInit(): Promise<void> {
    this.id = this.ruta.snapshot.paramMap.get('id') ?? '';
    try {
      this.orden.set(await this.api.orden(this.id));
      await Promise.all([this.cargarPase(), this.cargarNombreDelParque()]);
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  /** La orden solo trae el id del parque; el nombre es un extra, si no se consigue se muestra el id. */
  private async cargarNombreDelParque(): Promise<void> {
    try {
      const parques: Parque[] = await this.api.parques();
      this.nombreDelParque.set(parques.find(p => p.id === this.orden()?.parqueId)?.nombre ?? null);
    } catch {
      this.nombreDelParque.set(null);
    }
  }

  /** El pase se emite justo después de la compra; si aún no está, se reintenta unas veces. */
  private async cargarPase(): Promise<void> {
    if (this.orden()?.estado !== 'CONFIRMADA') {
      this.pase.set(null);
      this.qr.set(null);
      return;
    }
    for (let intento = 0; intento < 6; intento++) {
      try {
        const pase = await this.api.pase(this.id);
        this.pase.set(pase);
        this.qr.set(await toDataURL(pase.codigo, { width: 280, margin: 1, errorCorrectionLevel: 'M' }));
        return;
      } catch {
        await new Promise(r => setTimeout(r, 700));
      }
    }
  }

  protected pedirConfirmacion(): void {
    this.confirmandoCancelacion.set(true);
  }

  protected conservar(): void {
    this.confirmandoCancelacion.set(false);
  }

  protected async cancelar(): Promise<void> {
    this.cancelando.set(true);
    this.errorCancelar.set(null);
    try {
      this.orden.set(await this.api.cancelar(this.id));
      this.confirmandoCancelacion.set(false);
      await this.cargarPase();
    } catch (e) {
      this.errorCancelar.set(mensajeDeError(e));
    } finally {
      this.cancelando.set(false);
    }
  }

  protected resumenDePersonas(orden: Orden): string {
    const n = orden.lineas.length;
    return `${n} ${n === 1 ? 'persona' : 'personas'}`;
  }

  protected enlace(): string {
    return window.location.href;
  }
}
