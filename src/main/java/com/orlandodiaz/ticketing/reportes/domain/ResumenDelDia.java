package com.orlandodiaz.ticketing.reportes.domain;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Resumen de un parque para un dia de VISITA. Se calcula a partir de los hechos (ordenes e ingresos), no se guarda.
 *
 * - Las ordenes canceladas no cuentan como venta: se informan aparte.
 * - {@code ingresaron} cuenta a todas las personas que pasaron por la puerta, aunque despues se cancelara su
 *   orden (ya entraron). Por eso {@code sinIngresar} nunca baja de cero.
 * - {@code sinIngresar} = entradas vendidas que todavia no se usaron. De un dia pasado, son los "no-show";
 *   del dia de hoy, son los que aun pueden llegar.
 */
public record ResumenDelDia(
        ParqueId parqueId,
        LocalDate fecha,
        int ordenesVigentes,
        int entradasVendidas,
        Dinero ventas,
        int ordenesCanceladas,
        int entradasCanceladas,
        int ingresaron,
        int sinIngresar,
        Map<String, Integer> ingresosPorPuerta) {

    public ResumenDelDia {
        ingresosPorPuerta = Collections.unmodifiableMap(new TreeMap<>(ingresosPorPuerta)); // ordenado por puerta
    }

    /** Calcula el resumen de un dia con los hechos de ese dia (los de otros dias se ignoran). */
    public static ResumenDelDia de(ParqueId parqueId, LocalDate fecha, Collection<OrdenReportada> ordenes,
                                   Collection<IngresoReportado> ingresos) {
        int vigentes = 0;
        int vendidas = 0;
        Dinero ventas = Dinero.CERO;
        int canceladas = 0;
        int entradasCanceladas = 0;
        for (OrdenReportada o : ordenes) {
            if (!o.parqueId().equals(parqueId) || !o.fechaVisita().equals(fecha)) {
                continue;
            }
            if (o.cancelada()) {
                canceladas++;
                entradasCanceladas += o.entradas();
            } else {
                vigentes++;
                vendidas += o.entradas();
                ventas = ventas.sumar(o.total());
            }
        }

        int ingresaron = 0;
        Map<String, Integer> porPuerta = new TreeMap<>();
        for (IngresoReportado i : ingresos) {
            if (!i.parqueId().equals(parqueId) || !i.fechaVisita().equals(fecha)) {
                continue;
            }
            ingresaron += i.personas();
            porPuerta.merge(i.puerta(), i.personas(), Integer::sum);
        }

        return new ResumenDelDia(parqueId, fecha, vigentes, vendidas, ventas, canceladas, entradasCanceladas,
                ingresaron, Math.max(0, vendidas - ingresaron), porPuerta);
    }
}
