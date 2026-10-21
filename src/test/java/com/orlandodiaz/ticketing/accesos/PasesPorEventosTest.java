package com.orlandodiaz.ticketing.accesos;

import static com.orlandodiaz.ticketing.ventas.application.Escenario.PARQUE_A;
import static com.orlandodiaz.ticketing.ventas.application.Escenario.VISITA;
import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.accesos.application.AnularPaseDeOrden;
import com.orlandodiaz.ticketing.accesos.application.EmitirPase;
import com.orlandodiaz.ticketing.accesos.domain.EstadoPase;
import com.orlandodiaz.ticketing.accesos.infrastructure.EscuchaDeEventosDeVentas;
import com.orlandodiaz.ticketing.accesos.infrastructure.RepositorioEnMemoriaDePases;
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

/** Prueba de punta a punta: ventas publica eventos y accesos reacciona, sin conocerse. */
class PasesPorEventosTest {

    private RepositorioEnMemoriaDePases pases;
    private BusDeEventosEnMemoria bus;
    private Escenario escenario;

    @BeforeEach
    void preparar() {
        pases = new RepositorioEnMemoriaDePases();
        bus = new BusDeEventosEnMemoria();
        var contador = new AtomicInteger();
        new EscuchaDeEventosDeVentas(
                new EmitirPase(pases, () -> "QR-" + contador.incrementAndGet()),
                new AnularPaseDeOrden(pases)).registrarEn(bus);

        escenario = new Escenario(new RepositorioEnMemoriaDeOrdenes(), bus);
    }

    private Orden comprarTres() {
        return escenario.comprar.ejecutar(new ComprarEntradasComando(PARQUE_A, VISITA,
                List.of(new Visitante(8, false), new Visitante(30, false), new Visitante(31, false))));
    }

    @Test
    void comprarEmiteUnPaseConTantasEntradasComoVisitantes() {
        Orden orden = comprarTres();

        var pase = pases.buscarPorOrden(orden.id().valor()).orElseThrow();
        assertThat(pase.codigo()).isEqualTo("QR-1");
        assertThat(pase.entradasTotales()).isEqualTo(3);
        assertThat(pase.fechaVisita()).isEqualTo(VISITA);
        assertThat(pase.parqueId()).isEqualTo(PARQUE_A);
        assertThat(pase.estado()).isEqualTo(EstadoPase.ACTIVO);
    }

    @Test
    void cancelarLaOrdenAnulaElPase() {
        Orden orden = comprarTres();

        escenario.cancelar.ejecutar(orden.id());

        assertThat(pases.buscarPorOrden(orden.id().valor()).orElseThrow().estado()).isEqualTo(EstadoPase.ANULADO);
    }

    @Test
    void siElMismoEventoLlegaDosVecesSoloHayUnPase() {
        Orden orden = comprarTres();
        var repetido = new OrdenConfirmada(orden.id(), orden.parqueId(), orden.fechaVisita(),
                orden.lineas(), orden.total(), orden.creadaEn());

        bus.publicar(repetido);

        assertThat(pases.buscarPorOrden(orden.id().valor()).orElseThrow().codigo()).isEqualTo("QR-1");
        assertThat(pases.buscarPorCodigo("QR-2")).isEmpty(); // no se emitio un segundo pase
    }
}
