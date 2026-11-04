import { Routes } from '@angular/router';
import { soloPersonal } from './autorizacion';

export const routes: Routes = [
  { path: '', title: 'Compra tus entradas', loadComponent: () => import('./comprar/comprar').then(m => m.Comprar) },
  { path: 'orden/:id', title: 'Tu boleto', loadComponent: () => import('./orden/orden').then(m => m.OrdenPage) },
  { path: 'acceso', title: 'Acceso del personal', loadComponent: () => import('./acceso/acceso').then(m => m.AccesoPage) },
  { path: 'puerta', title: 'Puerta', canActivate: [soloPersonal], loadComponent: () => import('./puerta/puerta').then(m => m.PuertaPage) },
  { path: '**', redirectTo: '' }
];
