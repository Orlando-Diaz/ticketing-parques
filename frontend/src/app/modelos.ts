export interface Parque {
  id: string;
  nombre: string;
  cupoDiario: number;
}

export interface Ajuste {
  tipo: string;
  motivo: string;
  monto: number;
}

export interface Cotizacion {
  precioBase: number;
  ajustes: Ajuste[];
  total: number;
}

export interface LineaDeOrden {
  edadVisitante: number;
  residente: boolean;
  precio: number;
}

export interface Orden {
  id: string;
  parqueId: string;
  fechaVisita: string;
  estado: string;
  total: number;
  lineas: LineaDeOrden[];
}

export interface Ingreso {
  personas: number;
  puerta: string;
  momento: string;
}

export interface Pase {
  id: string;
  codigo: string;
  ordenId: string;
  parqueId: string;
  fechaVisita: string;
  estado: string;
  entradasTotales: number;
  entradasUsadas: number;
  entradasRestantes: number;
  ingresos: Ingreso[];
}

export interface VisitanteNuevo {
  edad: number;
  residente: boolean;
}
