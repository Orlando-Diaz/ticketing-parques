import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_URL } from './config';
import { Cotizacion, Orden, Parque, Pase, VisitanteNuevo } from './modelos';

/** Todas las llamadas a la API del visitante. Los componentes no conocen URLs. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  parques(): Promise<Parque[]> {
    return firstValueFrom(this.http.get<Parque[]>(`${API_URL}/api/parques`));
  }

  disponibilidad(parqueId: string, fecha: string): Promise<{ disponibles: number }> {
    const params = new HttpParams().set('fecha', fecha);
    return firstValueFrom(this.http.get<{ disponibles: number }>(
      `${API_URL}/api/parques/${encodeURIComponent(parqueId)}/disponibilidad`, { params }));
  }

  cotizar(parqueId: string, edad: number, residente: boolean, grupo: number, fecha: string): Promise<Cotizacion> {
    const params = new HttpParams()
      .set('edad', edad).set('residente', residente).set('grupo', grupo).set('fecha', fecha);
    return firstValueFrom(this.http.get<Cotizacion>(
      `${API_URL}/api/parques/${encodeURIComponent(parqueId)}/precios/cotizacion`, { params }));
  }

  comprar(parqueId: string, fechaVisita: string, visitantes: VisitanteNuevo[]): Promise<Orden> {
    return firstValueFrom(this.http.post<Orden>(`${API_URL}/api/ordenes`, { parqueId, fechaVisita, visitantes }));
  }

  orden(id: string): Promise<Orden> {
    return firstValueFrom(this.http.get<Orden>(`${API_URL}/api/ordenes/${encodeURIComponent(id)}`));
  }

  pase(ordenId: string): Promise<Pase> {
    return firstValueFrom(this.http.get<Pase>(`${API_URL}/api/ordenes/${encodeURIComponent(ordenId)}/pase`));
  }

  cancelar(id: string): Promise<Orden> {
    return firstValueFrom(this.http.post<Orden>(`${API_URL}/api/ordenes/${encodeURIComponent(id)}/cancelacion`, {}));
  }
}

/** Convierte un error HTTP en un mensaje para la persona: usa el detalle que manda la API cuando existe. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.';
    }
    const detalle = error.error?.detail;
    if (typeof detalle === 'string' && detalle.length > 0) {
      return detalle;
    }
    if (error.status === 404) {
      return 'No encontramos lo que buscas.';
    }
  }
  return 'Algo salió mal. Inténtalo de nuevo en un momento.';
}
