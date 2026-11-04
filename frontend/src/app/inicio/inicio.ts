import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, mensajeDeError } from '../api.service';
import { AuthService } from '../auth.service';
import { Parque } from '../modelos';

/** La puerta de entrada de la app: quien llega ve los parques y puede crear su cuenta o iniciar sesión. */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink],
  templateUrl: './inicio.html'
})
export class InicioPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly parques = signal<Parque[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      this.parques.set(await this.api.parques());
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
  }
}
