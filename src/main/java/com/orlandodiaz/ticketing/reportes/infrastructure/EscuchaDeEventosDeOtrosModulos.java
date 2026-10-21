package com.orlandodiaz.ticketing.reportes.infrastructure;

import com.orlandodiaz.ticketing.accesos.domain.IngresoRegistrado;
import com.orlandodiaz.ticketing.reportes.application.ActualizarReportes;
import com.orlandodiaz.ticketing.reportes.domain.IngresoReportado;
import com.orlandodiaz.ticketing.reportes.domain.OrdenReportada;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import com.orlandodiaz.ticketing.ventas.domain.OrdenCancelada;
import com.orlandodiaz.ticketing.ventas.domain.OrdenConfirmada;

/**
 * ADAPTADOR de entrada: escucha los eventos de ventas y accesos y los traduce a hechos de reportes.
 * Es el UNICO lugar de reportes que conoce a esos modulos; ellos no saben que reportes existe.
 */
public class EscuchaDeEventosDeOtrosModulos {

    private final ActualizarReportes actualizar;

    public EscuchaDeEventosDeOtrosModulos(ActualizarReportes actualizar) {
        this.actualizar = actualizar;
    }

    public void registrarEn(BusDeEventosEnMemoria bus) {
        bus.suscribir(OrdenConfirmada.class, this::alConfirmarseUnaOrden);
        bus.suscribir(OrdenCancelada.class, this::alCancelarseUnaOrden);
        bus.suscribir(IngresoRegistrado.class, this::alRegistrarseUnIngreso);
    }

    private void alConfirmarseUnaOrden(OrdenConfirmada e) {
        actualizar.alConfirmarseUnaOrden(new OrdenReportada(e.ordenId().valor(), e.parqueId(), e.fechaVisita(),
                e.lineas().size(), e.total(), false, e.ocurridoEn()));
    }

    private void alCancelarseUnaOrden(OrdenCancelada e) {
        actualizar.alCancelarseUnaOrden(e.ordenId().valor());
    }

    private void alRegistrarseUnIngreso(IngresoRegistrado e) {
        actualizar.alRegistrarseUnIngreso(new IngresoReportado(e.ordenRef(), e.numeroDeIngreso(), e.parqueId(),
                e.fechaVisita(), e.personas(), e.puerta(), e.ocurridoEn()));
    }
}
