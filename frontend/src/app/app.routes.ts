import { Routes } from '@angular/router';
import { soloAdmin, soloCliente, soloPersonal } from './autorizacion';

export const routes: Routes = [
  { path: '', title: 'Boletería de parques', loadComponent: () => import('./inicio/inicio').then(m => m.InicioPage) },
  { path: 'comprar', title: 'Compra tus entradas', canActivate: [soloCliente], loadComponent: () => import('./comprar/comprar').then(m => m.Comprar) },
  { path: 'perfil', title: 'Mi perfil', canActivate: [soloCliente], loadComponent: () => import('./perfil/perfil').then(m => m.PerfilPage) },
  { path: 'orden/:id', title: 'Tu boleto', loadComponent: () => import('./orden/orden').then(m => m.OrdenPage) },
  { path: 'acceso', title: 'Iniciar sesión', loadComponent: () => import('./acceso/acceso').then(m => m.AccesoPage) },
  { path: 'puerta', title: 'Puerta', canActivate: [soloPersonal], loadComponent: () => import('./puerta/puerta').then(m => m.PuertaPage) },
  {
    path: 'admin',
    canActivate: [soloAdmin],
    loadComponent: () => import('./admin/admin').then(m => m.AdminPage),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'parques' },
      { path: 'parques', title: 'Parques', loadComponent: () => import('./admin/parques/parques').then(m => m.ParquesAdmin) },
      { path: 'parques/:id/precios', title: 'Precios', loadComponent: () => import('./admin/precios/precios').then(m => m.PreciosAdmin) },
      { path: 'reportes', title: 'Reportes', loadComponent: () => import('./admin/reportes/reportes').then(m => m.ReportesAdmin) },
      { path: 'usuarios', title: 'Usuarios', loadComponent: () => import('./admin/usuarios/usuarios').then(m => m.UsuariosAdmin) }
    ]
  },
  { path: '**', redirectTo: '' }
];
