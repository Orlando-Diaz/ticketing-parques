import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, mensajeDeError } from '../api.service';
import { AuthService } from '../auth.service';
import { fechaLarga, hoyIso, pesos } from '../formato';
import { Orden, Parque } from '../modelos';

/** El perfil del cliente: sus datos y el historial de sus compras (próximas primero). */
@Component({
  selector: 'app-perfil',
  imports: [RouterLink],
  templateUrl: './perfil.html'
})
export class PerfilPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly pesos = pesos;
  protected readonly fechaLarga = fechaLarga;

  protected readonly ordenes = signal<Orden[]>([]);
  private readonly parques = signal<Parque[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  private readonly hoy = hoyIso();
  protected readonly proximas = computed(() => this.ordenes()
    .filter(o => o.estado === 'CONFIRMADA' && o.fechaVisita >= this.hoy)
    .sort((a, b) => a.fechaVisita.localeCompare(b.fechaVisita)));
  protected readonly anteriores = computed(() => this.ordenes()
    .filter(o => !(o.estado === 'CONFIRMADA' && o.fechaVisita >= this.hoy)));

  async ngOnInit(): Promise<void> {
    try {
      const [ordenes, parques] = await Promise.all([this.api.misOrdenes(), this.api.parques().catch(() => [])]);
      this.ordenes.set(ordenes);
      this.parques.set(parques);
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  protected nombreDelParque(id: string): string {
    return this.parques().find(p => p.id === id)?.nombre ?? id;
  }

  protected entradas(o: Orden): string {
    return o.lineas.length === 1 ? '1 entrada' : `${o.lineas.length} entradas`;
  }
}
