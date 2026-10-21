package com.orlandodiaz.ticketing.reportes;

import static com.orlandodiaz.ticketing.ventas.application.Escenario.PARQUE_A;
import static com.orlandodiaz.ticketing.ventas.application.Escenario.VISITA;
import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.accesos.application.AnularPaseDeOrden;
import com.orlandodiaz.ticketing.accesos.application.EmitirPase;
import com.orlandodiaz.ticketing.accesos.application.ValidarIngreso;
import com.orlandodiaz.ticketing.accesos.infrastructure.RepositorioEnMemoriaDePases;
import com.orlandodiaz.ticketing.reportes.application.ActualizarReportes;
import com.orlandodiaz.ticketing.reportes.application.ConsultarReportes;
import com.orlandodiaz.ticketing.reportes.infrastructure.AlmacenEnMemoriaDeReportes;
import com.orlandodiaz.ticketing.reportes.infrastructure.EscuchaDeEventosDeOtrosModulos;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import com.orlandodiaz.ticketing.ventas.application.ComprarEntradasComando;
import com.orlandodiaz.ticketing.ventas.application.Escenario;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenConfirmada;
import com.orlandodiaz.ticketing.ventas.domain.Visitante;
import com.orlandodiaz.ticketing.ventas.infrastructure.RepositorioEnMemoriaDeOrdenes;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba de punta a punta de los tres modulos conectados SOLO por eventos:
 * ventas (compra/cancela) + accesos (pases e ingresos) alimentan a reportes sin conocerlo.
 */
class ReportesPorEventosTest {

    private BusDeEventosEnMemoria bus;
    private Escenario escenario;
    private RepositorioEnMemoriaDePases pases;
    private ValidarIngreso validarIngreso;
    private ConsultarReportes reportes;

    @BeforeEach
    void preparar() {
        bus = new BusDeEventosEnMemoria();
        var contador = new AtomicInteger();

        pases = new RepositorioEnMemoriaDePases();
        new com.orlandodiaz.ticketing.accesos.infrastructure.EscuchaDeEventosDeVentas(
                new EmitirPase(pases, () -> "QR-" + contador.incrementAndGet()),
                new AnularPaseDeOrden(pases)).registrarEn(bus);
        validarIngreso = new ValidarIngreso(pases, () -> VISITA.atTime(10, 0), bus); // "hoy" es el dia de la visita

        var almacen = new AlmacenEnMemoriaDeReportes();
        new EscuchaDeEventosDeOtrosModulos(new ActualizarReportes(almacen)).registrarEn(bus);
        reportes = new ConsultarReportes(almacen);

        escenario = new Escenario(new RepositorioEnMemoriaDeOrdenes(), bus);
    }

    private Orden comprar(Visitante... visitantes) {
        return escenario.comprar.ejecutar(new ComprarEntradasComando(PARQUE_A, VISITA, List.of(visitantes)));
    }

    @Test
    void elResumenReflejaVentasCancelacionesEIngresos() {
        comprar(new Visitante(8, false), new Visitante(30, false), new Visitante(31, false)); // 25.000 + 50.000 + 50.000
        Orden segunda = comprar(new Visitante(30, false), new Visitante(31, false));           // 100.000
        escenario.cancelar.ejecutar(segunda.id());

        validarIngreso.ejecutar("QR-1", 2, "norte"); // de la primera orden entran 2 de 3

        var resumen = reportes.resumenDelDia(PARQUE_A, VISITA);
        assertThat(resumen.ordenesVigentes()).isEqualTo(1);
        assertThat(resumen.entradasVendidas()).isEqualTo(3);
        assertThat(resumen.ventas()).isEqualTo(Dinero.pesos(125_000));
        assertThat(resumen.ordenesCanceladas()).isEqualTo(1);
        assertThat(resumen.entradasCanceladas()).isEqualTo(2);
        assertThat(resumen.ingresaron()).isEqualTo(2);
        assertThat(resumen.sinIngresar()).isEqualTo(1);
        assertThat(resumen.ingresosPorPuerta()).containsExactly(java.util.Map.entry("norte", 2));
    }

    @Test
    void siTodosLosEventosLlegaranDosVecesLosTotalesNoCambian() {
        Orden orden = comprar(new Visitante(30, false), new Visitante(31, false));
        validarIngreso.ejecutar("QR-1", 1, "norte");
        var antes = reportes.resumenDelDia(PARQUE_A, VISITA);

        // reentrega del evento de la orden
        bus.publicar(new OrdenConfirmada(orden.id(), orden.parqueId(), orden.fechaVisita(), orden.lineas(),
                orden.total(), orden.creadaEn()));

        assertThat(reportes.resumenDelDia(PARQUE_A, VISITA)).isEqualTo(antes);
    }

    @Test
    void sinActividadElResumenEstaVacio() {
        var resumen = reportes.resumenDelDia(PARQUE_A, VISITA);

        assertThat(resumen.entradasVendidas()).isZero();
        assertThat(resumen.ventas()).isEqualTo(Dinero.CERO);
    }
}
