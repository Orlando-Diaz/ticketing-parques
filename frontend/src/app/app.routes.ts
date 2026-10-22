import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', title: 'Compra tus entradas', loadComponent: () => import('./comprar/comprar').then(m => m.Comprar) },
  { path: 'orden/:id', title: 'Tu boleto', loadComponent: () => import('./orden/orden').then(m => m.OrdenPage) },
  { path: '**', redirectTo: '' }
];
