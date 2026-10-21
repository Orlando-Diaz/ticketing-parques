package com.orlandodiaz.ticketing.reportes.infrastructure.web;

import com.orlandodiaz.ticketing.reportes.application.ConsultarReportes;
import com.orlandodiaz.ticketing.reportes.domain.ResumenDelDia;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes/parques/{parqueId}")
public class ReportesController {

    public record ResumenResponse(String parqueId, LocalDate fecha, int ordenesVigentes, int entradasVendidas,
                                  long ventas, int ordenesCanceladas, int entradasCanceladas, int ingresaron,
                                  int sinIngresar, Map<String, Integer> ingresosPorPuerta) {

        static ResumenResponse desde(ResumenDelDia r) {
            return new ResumenResponse(r.parqueId().valor(), r.fecha(), r.ordenesVigentes(), r.entradasVendidas(),
                    r.ventas().pesos(), r.ordenesCanceladas(), r.entradasCanceladas(), r.ingresaron(),
                    r.sinIngresar(), r.ingresosPorPuerta());
        }
    }

    private final ConsultarReportes consultarReportes;

    public ReportesController(ConsultarReportes consultarReportes) {
        this.consultarReportes = consultarReportes;
    }

    /** Resumen de un dia de visita: ventas, cancelaciones, quienes ya entraron y quienes faltan. */
    @GetMapping("/dia")
    public ResumenResponse resumenDelDia(
            @PathVariable("parqueId") String parqueId,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResumenResponse.desde(consultarReportes.resumenDelDia(new ParqueId(parqueId), fecha));
    }

    /** Un resumen por cada dia con actividad dentro del rango (maximo 366 dias). */
    @GetMapping("/rango")
    public List<ResumenResponse> resumenPorDia(
            @PathVariable("parqueId") String parqueId,
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return consultarReportes.resumenPorDia(new ParqueId(parqueId), desde, hasta).stream()
                .map(ResumenResponse::desde).toList();
    }
}
