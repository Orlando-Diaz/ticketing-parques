package com.orlandodiaz.ticketing.reportes.application;

import com.orlandodiaz.ticketing.reportes.domain.IngresoReportado;
import com.orlandodiaz.ticketing.reportes.domain.OrdenReportada;

/**
 * CASO DE USO: mantener al dia el modelo de lectura. Son tres operaciones muy pequenas y cohesivas (todas
 * "anotan un hecho"), por eso van juntas; cada una es idempotente gracias al contrato del almacen.
 */
public class ActualizarReportes {

    private final AlmacenDeReportes almacen;

    public ActualizarReportes(AlmacenDeReportes almacen) {
        this.almacen = almacen;
    }

    public void alConfirmarseUnaOrden(OrdenReportada orden) {
        almacen.registrarOrden(orden);
    }

    public void alCancelarseUnaOrden(String ordenRef) {
        almacen.marcarOrdenCancelada(ordenRef);
    }

    public void alRegistrarseUnIngreso(IngresoReportado ingreso) {
        almacen.registrarIngreso(ingreso);
    }
}
