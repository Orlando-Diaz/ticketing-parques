package com.orlandodiaz.ticketing.reportes.application;

import com.orlandodiaz.ticketing.reportes.domain.IngresoReportado;
import com.orlandodiaz.ticketing.reportes.domain.OrdenReportada;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.List;

/**
 * Puerto de salida: donde se guardan los hechos que alimentan los reportes.
 *
 * CONTRATO: registrarOrden() y registrarIngreso() son IDEMPOTENTES (si el hecho ya existe, no hacen nada),
 * porque un evento puede entregarse dos veces. marcarOrdenCancelada() tambien: cancelar dos veces es lo mismo
 * que una, y cancelar una orden desconocida no hace nada.
 */
public interface AlmacenDeReportes {

    void registrarOrden(OrdenReportada orden);

    void marcarOrdenCancelada(String ordenRef);

    void registrarIngreso(IngresoReportado ingreso);

    /** Ordenes (vigentes y canceladas) de un parque cuya fecha de visita esta entre {@code desde} y {@code hasta}. */
    List<OrdenReportada> ordenesEntre(ParqueId parqueId, LocalDate desde, LocalDate hasta);

    List<IngresoReportado> ingresosEntre(ParqueId parqueId, LocalDate desde, LocalDate hasta);
}
