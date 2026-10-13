package com.orlandodiaz.ticketing.facturacion;

import static com.orlandodiaz.ticketing.ventas.application.Escenario.PARQUE_A;
import static com.orlandodiaz.ticketing.ventas.application.Escenario.VISITA;
import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.facturacion.application.AnularFacturaDeOrden;
import com.orlandodiaz.ticketing.facturacion.application.EmitirFactura;
import com.orlandodiaz.ticketing.facturacion.domain.EstadoFactura;
import com.orlandodiaz.ticketing.facturacion.domain.Factura;
import com.orlandodiaz.ticketing.facturacion.infrastructure.EscuchaDeEventosDeVentas;
import com.orlandodiaz.ticketing.facturacion.infrastructure.NumeradorEnMemoria;
import com.orlandodiaz.ticketing.facturacion.infrastructure.RepositorioEnMemoriaDeFacturas;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import com.orlandodiaz.ticketing.ventas.application.ComprarEntradasComando;
import com.orlandodiaz.ticketing.ventas.application.Escenario;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenConfirmada;
import com.orlandodiaz.ticketing.ventas.domain.Visitante;
import com.orlandodiaz.ticketing.ventas.infrastructure.RepositorioEnMemoriaDeOrdenes;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Prueba de punta a punta: ventas publica eventos y facturacion reacciona, sin conocerse. */
class FacturacionPorEventosTest {

    private RepositorioEnMemoriaDeFacturas facturas;
    private BusDeEventosEnMemoria bus;
    private List<String> erroresDelBus;
    private Escenario escenario;

    @BeforeEach
    void preparar() {
        erroresDelBus = new ArrayList<>();
        bus = new BusDeEventosEnMemoria((evento, error) -> erroresDelBus.add(error.getMessage()));
        facturas = new RepositorioEnMemoriaDeFacturas();
        new EscuchaDeEventosDeVentas(
                new EmitirFactura(facturas, new NumeradorEnMemoria()),
                new AnularFacturaDeOrden(facturas)).registrarEn(bus);

        escenario = new Escenario(new RepositorioEnMemoriaDeOrdenes(), bus);
    }

    private Orden comprarNinoYAdulto() {
        return escenario.comprar.ejecutar(new ComprarEntradasComando(PARQUE_A, VISITA,
                List.of(new Visitante(8, false), new Visitante(30, false))));
    }

    @Test
    void comprarGeneraLaFacturaConElTotalYLasLineas() {
        Orden orden = comprarNinoYAdulto();

        Factura factura = facturas.buscarPorOrden(orden.id().valor()).orElseThrow();
        assertThat(factura.numero()).isEqualTo("FAC-000001");
        assertThat(factura.total()).isEqualTo(Dinero.pesos(75_000));
        assertThat(factura.lineas()).hasSize(2);
        assertThat(factura.estado()).isEqualTo(EstadoFactura.EMITIDA);
    }

    @Test
    void cancelarLaOrdenAnulaLaFactura() {
        Orden orden = comprarNinoYAdulto();

        escenario.cancelar.ejecutar(orden.id());

        assertThat(facturas.buscarPorOrden(orden.id().valor()).orElseThrow().estado())
                .isEqualTo(EstadoFactura.ANULADA);
    }

    @Test
    void siUnSuscriptorFallaLaCompraSigueSiendoValida() {
        bus.suscribir(OrdenConfirmada.class, e -> {
            throw new IllegalStateException("el servicio de correo esta caido");
        });

        Orden orden = comprarNinoYAdulto(); // no lanza

        assertThat(escenario.consultarCupo.ejecutar(PARQUE_A, VISITA)).isEqualTo(98);
        assertThat(facturas.buscarPorOrden(orden.id().valor())).isPresent();
        assertThat(erroresDelBus).containsExactly("el servicio de correo esta caido");
    }

    @Test
    void siElMismoEventoLlegaDosVecesSoloSeEmiteUnaFactura() {
        Orden orden = comprarNinoYAdulto();
        var repetido = new OrdenConfirmada(orden.id(), orden.parqueId(), orden.fechaVisita(),
                orden.lineas(), orden.total(), orden.creadaEn());

        bus.publicar(repetido);

        assertThat(facturas.cantidad()).isEqualTo(1);
    }
}
