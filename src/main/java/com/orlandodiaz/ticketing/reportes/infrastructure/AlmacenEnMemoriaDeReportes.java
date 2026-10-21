package com.orlandodiaz.ticketing.reportes.infrastructure;

import com.orlandodiaz.ticketing.reportes.application.AlmacenDeReportes;
import com.orlandodiaz.ticketing.reportes.domain.IngresoReportado;
import com.orlandodiaz.ticketing.reportes.domain.OrdenReportada;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Adaptador en memoria (pruebas). Cumple el mismo contrato idempotente que el de base de datos. */
public class AlmacenEnMemoriaDeReportes implements AlmacenDeReportes {

    private record ClaveIngreso(String ordenRef, int numero) {
    }

    private final Map<String, OrdenReportada> ordenes = new HashMap<>();
    private final Map<ClaveIngreso, IngresoReportado> ingresos = new HashMap<>();

    @Override
    public synchronized void registrarOrden(OrdenReportada orden) {
        ordenes.putIfAbsent(orden.ordenRef(), orden);
    }

    @Override
    public synchronized void marcarOrdenCancelada(String ordenRef) {
        ordenes.computeIfPresent(ordenRef, (ref, o) -> new OrdenReportada(o.ordenRef(), o.parqueId(),
                o.fechaVisita(), o.entradas(), o.total(), true, o.registradaEn()));
    }

    @Override
    public synchronized void registrarIngreso(IngresoReportado ingreso) {
        ingresos.putIfAbsent(new ClaveIngreso(ingreso.ordenRef(), ingreso.numero()), ingreso);
    }

    @Override
    public synchronized List<OrdenReportada> ordenesEntre(ParqueId parqueId, LocalDate desde, LocalDate hasta) {
        return ordenes.values().stream()
                .filter(o -> o.parqueId().equals(parqueId) && !o.fechaVisita().isBefore(desde)
                        && !o.fechaVisita().isAfter(hasta))
                .toList();
    }

    @Override
    public synchronized List<IngresoReportado> ingresosEntre(ParqueId parqueId, LocalDate desde, LocalDate hasta) {
        return ingresos.values().stream()
                .filter(i -> i.parqueId().equals(parqueId) && !i.fechaVisita().isBefore(desde)
                        && !i.fechaVisita().isAfter(hasta))
                .toList();
    }
}
