const moneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

/** 50000 -> "$ 50.000" */
export function pesos(valor: number): string {
  return moneda.format(valor);
}

/** "2026-12-20" -> "sábado, 20 de diciembre de 2026" (sin que la zona horaria corra el día) */
export function fechaLarga(iso: string): string {
  const [anio, mes, dia] = iso.split('-').map(Number);
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'full' }).format(new Date(anio, mes - 1, dia));
}

/** La fecha de hoy como YYYY-MM-DD en la hora local del visitante. */
export function hoyIso(): string {
  const d = new Date();
  const dos = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}`;
}
