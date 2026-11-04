import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-admin',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <main class="pagina pagina-admin">
      <nav class="subnav" aria-label="Administración">
        <a routerLink="parques" routerLinkActive="activo">Parques y precios</a>
        <a routerLink="reportes" routerLinkActive="activo">Reportes</a>
        <a routerLink="usuarios" routerLinkActive="activo">Usuarios</a>
      </nav>
      <router-outlet />
    </main>
  `
})
export class AdminPage {}
