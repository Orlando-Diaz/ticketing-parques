import { Component, computed, inject, signal } from '@angular/core';
import { ApiService, mensajeDeError } from '../api.service';
import { fechaLarga, hoyIso } from '../formato';
import { LectorQr } from '../lector-qr/lector-qr';
import { Pase } from '../modelos';

const CLAVE_PUERTA = 'boleteria.puerta';

function puertaGuardada(): string {
  try {
    return sessionStorage.getItem(CLAVE_PUERTA) || 'principal';
  } catch {
    return 'principal';
  }
}

@Component({
  selector: 'app-puerta',
  imports: [LectorQr],
  templateUrl: './puerta.html'
})
export class PuertaPage {
  private readonly api = inject(ApiService);
  protected readonly fechaLarga = fechaLarga;

  protected readonly puerta = signal(puertaGuardada());
  protected readonly escaneando = signal(false);
  protected readonly buscando = signal(false);
  protected readonly registrando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly pase = signal<Pase | null>(null);
  /** Resultado del último ingreso (null mientras no se haya registrado ninguno para el pase actual). */
  protected readonly admitidas = signal<number | null>(null);
  protected readonly personas = signal(1);

  protected readonly esDeOtroDia = computed(() => {
    const p = this.pase();
    return p !== null && p.fechaVisita !== hoyIso();
  });

  protected readonly puedeEntrar = computed(() => {
    const p = this.pase();
    return p !== null && p.estado === 'ACTIVO' && p.entradasRestantes > 0 && this.admitidas() === null;
  });

  protected cambiarPuerta(valor: string): void {
    const limpia = valor.trim() || 'principal';
    this.puerta.set(limpia);
    try {
      sessionStorage.setItem(CLAVE_PUERTA, limpia);
    } catch {
      // sin almacenamiento: vale solo mientras la página esté abierta
    }
  }

  protected escanear(): void {
    this.reiniciar();
    this.escaneando.set(true);
  }

  protected async codigoLeido(codigo: string): Promise<void> {
    this.escaneando.set(false);
    await this.buscar(codigo);
  }

  protected async buscar(codigo: string): Promise<void> {
    const limpio = codigo.trim();
    if (!limpio) {
      return;
    }
    this.reiniciar();
    this.buscando.set(true);
    try {
      const pase = await this.api.consultarPase(limpio);
      this.pase.set(pase);
      this.personas.set(1);
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.buscando.set(false);
    }
  }

  protected cambiarPersonas(delta: number): void {
    const p = this.pase();
    if (!p) {
      return;
    }
    this.personas.set(Math.max(1, Math.min(p.entradasRestantes, this.personas() + delta)));
  }

  protected async dejarEntrar(): Promise<void> {
    const p = this.pase();
    if (!p || this.registrando()) {
      return;
    }
    this.registrando.set(true);
    this.error.set(null);
    const cuantas = this.personas();
    try {
      const actualizado = await this.api.registrarIngreso(p.codigo, cuantas, this.puerta());
      this.pase.set(actualizado);
      this.admitidas.set(cuantas);
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.registrando.set(false);
    }
  }

  protected reiniciar(): void {
    this.escaneando.set(false);
    this.error.set(null);
    this.pase.set(null);
    this.admitidas.set(null);
    this.personas.set(1);
  }
}
