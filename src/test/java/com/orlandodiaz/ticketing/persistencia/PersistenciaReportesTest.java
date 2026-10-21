package com.orlandodiaz.ticketing.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.reportes.application.AlmacenDeReportes;
import com.orlandodiaz.ticketing.reportes.application.ConsultarReportes;
import com.orlandodiaz.ticketing.reportes.domain.IngresoReportado;
import com.orlandodiaz.ticketing.reportes.domain.OrdenReportada;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** El almacen de reportes contra una base real (H2 con las migraciones de Flyway). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class PersistenciaReportesTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 20);
    private static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 10, 19, 10, 30);

    @Autowired AlmacenDeReportes almacen;
    @Autowired ConsultarReportes consultar;

    private static ParqueId parqueUnico() {
        return new ParqueId("rep-" + UUID.randomUUID());
    }

    private static OrdenReportada orden(String ref, ParqueId parque, LocalDate dia, int entradas, long total) {
        return new OrdenReportada(ref, parque, dia, entradas, Dinero.pesos(total), false, MOMENTO);
    }

    @Test
    void losHechosSeGuardanYSeConsultanPorParqueYRango() {
        ParqueId parque = parqueUnico();
        ParqueId otro = parqueUnico();
        almacen.registrarOrden(orden("o-" + parque.valor() + "-1", parque, LUNES, 3, 125_000));
        almacen.registrarOrden(orden("o-" + parque.valor() + "-2", parque, MARTES, 2, 80_000));
        almacen.registrarOrden(orden("o-" + otro.valor() + "-1", otro, LUNES, 9, 900_000));
        almacen.registrarIngreso(new IngresoReportado("o-" + parque.valor() + "-1", 0, parque, LUNES, 2, "norte", MOMENTO));

        assertThat(almacen.ordenesEntre(parque, LUNES, MARTES)).hasSize(2);
        assertThat(almacen.ordenesEntre(parque, MARTES, MARTES)).hasSize(1);
        assertThat(almacen.ordenesEntre(parque, LUNES, LUNES).get(0).total()).isEqualTo(Dinero.pesos(125_000));
        assertThat(almacen.ordenesEntre(parque, LUNES, LUNES).get(0).registradaEn()).isEqualTo(MOMENTO);
        assertThat(almacen.ingresosEntre(parque, LUNES, MARTES)).singleElement()
                .satisfies(i -> {
                    assertThat(i.personas()).isEqualTo(2);
                    assertThat(i.puerta()).isEqualTo("norte");
                    assertThat(i.momento()).isEqualTo(MOMENTO);
                });
        assertThat(almacen.ordenesEntre(otro, LUNES, MARTES)).hasSize(1); // cada parque ve solo lo suyo
    }

    @Test
    void registrarDosVecesElMismoHechoSeIgnora() {
        ParqueId parque = parqueUnico();
        String ref = "o-" + parque.valor();
        var ingreso = new IngresoReportado(ref, 0, parque, LUNES, 2, "norte", MOMENTO);

        almacen.registrarOrden(orden(ref, parque, LUNES, 3, 125_000));
        almacen.registrarOrden(orden(ref, parque, LUNES, 3, 125_000));
        almacen.registrarIngreso(ingreso);
        almacen.registrarIngreso(ingreso);

        assertThat(almacen.ordenesEntre(parque, LUNES, LUNES)).hasSize(1);
        assertThat(almacen.ingresosEntre(parque, LUNES, LUNES)).hasSize(1);
    }

    @Test
    void cancelarMarcaLaOrdenYUnaConfirmacionRepetidaNoLaRevive() {
        ParqueId parque = parqueUnico();
        String ref = "o-" + parque.valor();
        almacen.registrarOrden(orden(ref, parque, LUNES, 3, 125_000));

        almacen.marcarOrdenCancelada(ref);
        almacen.marcarOrdenCancelada(ref);
        almacen.marcarOrdenCancelada("orden-que-no-existe");
        almacen.registrarOrden(orden(ref, parque, LUNES, 3, 125_000)); // evento repetido

        var resumen = consultar.resumenDelDia(parque, LUNES);
        assertThat(resumen.entradasVendidas()).isZero();
        assertThat(resumen.ordenesCanceladas()).isEqualTo(1);
        assertThat(resumen.entradasCanceladas()).isEqualTo(3);
    }
}
