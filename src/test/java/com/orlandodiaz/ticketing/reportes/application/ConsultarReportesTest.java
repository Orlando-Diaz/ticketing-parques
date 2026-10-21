package com.orlandodiaz.ticketing.reportes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.reportes.domain.IngresoReportado;
import com.orlandodiaz.ticketing.reportes.domain.OrdenReportada;
import com.orlandodiaz.ticketing.reportes.domain.ResumenDelDia;
import com.orlandodiaz.ticketing.reportes.infrastructure.AlmacenEnMemoriaDeReportes;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsultarReportesTest {

    private static final ParqueId PARQUE = new ParqueId("parque-a");
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 20);
    private static final LocalDate JUEVES = LocalDate.of(2026, 10, 22);

    private AlmacenEnMemoriaDeReportes almacen;
    private ActualizarReportes actualizar;
    private ConsultarReportes consultar;

    @BeforeEach
    void preparar() {
        almacen = new AlmacenEnMemoriaDeReportes();
        actualizar = new ActualizarReportes(almacen);
        consultar = new ConsultarReportes(almacen);
    }

    private void orden(String ref, LocalDate dia, int entradas, long total) {
        actualizar.alConfirmarseUnaOrden(new OrdenReportada(ref, PARQUE, dia, entradas, Dinero.pesos(total), false,
                dia.minusDays(3).atTime(9, 0)));
    }

    @Test
    void elRangoDevuelveSoloLosDiasConActividadOrdenadosPorFecha() {
        orden("o-jueves", JUEVES, 2, 80_000);
        orden("o-lunes", LUNES, 1, 40_000);
        actualizar.alRegistrarseUnIngreso(new IngresoReportado("o-martes", 0, PARQUE, MARTES, 1, "norte",
                MARTES.atTime(10, 0))); // un dia con ingresos pero sin ordenes registradas

        var resumenes = consultar.resumenPorDia(PARQUE, LUNES, JUEVES);

        assertThat(resumenes).extracting(ResumenDelDia::fecha).containsExactly(LUNES, MARTES, JUEVES);
        assertThat(resumenes.get(0).ventas()).isEqualTo(Dinero.pesos(40_000));
        assertThat(resumenes.get(1).ingresaron()).isEqualTo(1);
    }

    @Test
    void unRangoFueraDeLaActividadEstaVacio() {
        orden("o1", LUNES, 1, 40_000);

        assertThat(consultar.resumenPorDia(PARQUE, MARTES, JUEVES)).isEmpty();
    }

    @Test
    void elRangoSeValida() {
        assertThatThrownBy(() -> consultar.resumenPorDia(PARQUE, JUEVES, LUNES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consultar.resumenPorDia(PARQUE, LUNES, LUNES.plusDays(400)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("366");
        assertThat(consultar.resumenPorDia(PARQUE, LUNES, LUNES.plusDays(365))).isEmpty(); // 366 dias: permitido
    }

    @Test
    void registrarElMismoHechoDosVecesNoCuentaDoble() {
        orden("o1", LUNES, 3, 120_000);
        orden("o1", LUNES, 3, 120_000);
        var ingreso = new IngresoReportado("o1", 0, PARQUE, LUNES, 2, "norte", LUNES.atTime(10, 0));
        actualizar.alRegistrarseUnIngreso(ingreso);
        actualizar.alRegistrarseUnIngreso(ingreso);

        var resumen = consultar.resumenDelDia(PARQUE, LUNES);

        assertThat(resumen.entradasVendidas()).isEqualTo(3);
        assertThat(resumen.ingresaron()).isEqualTo(2);
    }

    @Test
    void cancelarDosVecesOUnaOrdenDesconocidaNoHaceDano() {
        orden("o1", LUNES, 3, 120_000);

        actualizar.alCancelarseUnaOrden("o1");
        actualizar.alCancelarseUnaOrden("o1");
        actualizar.alCancelarseUnaOrden("no-existe");

        var resumen = consultar.resumenDelDia(PARQUE, LUNES);
        assertThat(resumen.entradasVendidas()).isZero();
        assertThat(resumen.ordenesCanceladas()).isEqualTo(1);
    }

    @Test
    void unaConfirmacionRepetidaDespuesDeCancelarNoRevivaLaOrden() {
        orden("o1", LUNES, 3, 120_000);
        actualizar.alCancelarseUnaOrden("o1");

        orden("o1", LUNES, 3, 120_000); // el evento de confirmacion llega de nuevo

        assertThat(consultar.resumenDelDia(PARQUE, LUNES).entradasVendidas()).isZero();
    }
}
