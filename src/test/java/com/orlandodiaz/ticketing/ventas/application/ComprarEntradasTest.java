package com.orlandodiaz.ticketing.ventas.application;

import static com.orlandodiaz.ticketing.ventas.application.Escenario.PARQUE_A;
import static com.orlandodiaz.ticketing.ventas.application.Escenario.PARQUE_B;
import static com.orlandodiaz.ticketing.ventas.application.Escenario.VISITA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.catalogo.domain.CupoAgotadoException;
import com.orlandodiaz.ticketing.precios.domain.SinTarifaParaEdadException;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.ventas.domain.EstadoOrden;
import com.orlandodiaz.ticketing.ventas.domain.FechaDeVisitaInvalidaException;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;
import com.orlandodiaz.ticketing.ventas.domain.Visitante;
import com.orlandodiaz.ticketing.ventas.infrastructure.RepositorioEnMemoriaDeOrdenes;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ComprarEntradasTest {

    private RepositorioEnMemoriaDeOrdenes ordenes;
    private Escenario escenario;

    @BeforeEach
    void preparar() {
        ordenes = new RepositorioEnMemoriaDeOrdenes();
        escenario = new Escenario(ordenes);
    }

    private static ComprarEntradasComando compra(com.orlandodiaz.ticketing.shared.domain.ParqueId parque,
                                                 LocalDate fecha, Visitante... visitantes) {
        return new ComprarEntradasComando(parque, fecha, List.of(visitantes));
    }

    @Test
    void compraFelizCalculaTotalReservaCupoYGuardaLaOrden() {
        Orden orden = escenario.comprar.ejecutar(
                compra(PARQUE_A, VISITA, new Visitante(8, false), new Visitante(30, false)));

        assertThat(orden.total()).isEqualTo(Dinero.pesos(75_000)); // 25.000 + 50.000
        assertThat(orden.estado()).isEqualTo(EstadoOrden.CONFIRMADA);
        assertThat(orden.lineas()).hasSize(2);
        assertThat(escenario.consultarCupo.ejecutar(PARQUE_A, VISITA)).isEqualTo(98);
        assertThat(ordenes.buscarPorId(orden.id())).isPresent();
    }

    @Test
    void noVendeSiNoHayCupoSuficienteYNoDejaOrden() {
        var tres = compra(PARQUE_B, VISITA, new Visitante(20, false), new Visitante(21, false), new Visitante(22, false));

        assertThatThrownBy(() -> escenario.comprar.ejecutar(tres)).isInstanceOf(CupoAgotadoException.class);
        assertThat(ordenes.cantidad()).isZero();
        assertThat(escenario.consultarCupo.ejecutar(PARQUE_B, VISITA)).isEqualTo(2);
    }

    @Test
    void rechazaFechasPasadasSinTocarElCupo() {
        var ayer = compra(PARQUE_A, LocalDate.of(2026, 10, 12), new Visitante(30, false));

        assertThatThrownBy(() -> escenario.comprar.ejecutar(ayer)).isInstanceOf(FechaDeVisitaInvalidaException.class);
        assertThat(escenario.consultarCupo.ejecutar(PARQUE_A, LocalDate.of(2026, 10, 12))).isEqualTo(100);
    }

    @Test
    void sePuedeComprarParaHoy() {
        Orden orden = escenario.comprar.ejecutar(compra(PARQUE_A, LocalDate.of(2026, 10, 13), new Visitante(30, false)));
        assertThat(orden.estado()).isEqualTo(EstadoOrden.CONFIRMADA);
    }

    @Test
    void siNoHayTarifaParaLaEdadNoSeReservaCupo() {
        var bebe = compra(PARQUE_A, VISITA, new Visitante(1, false)); // el parque A empieza en 3 anios

        assertThatThrownBy(() -> escenario.comprar.ejecutar(bebe)).isInstanceOf(SinTarifaParaEdadException.class);
        assertThat(escenario.consultarCupo.ejecutar(PARQUE_A, VISITA)).isEqualTo(100);
    }

    @Test
    void exigeAlMenosUnVisitante() {
        assertThatThrownBy(() -> new ComprarEntradasComando(PARQUE_A, VISITA, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void siFallaGuardarLaOrdenSeDevuelveElCupo() {
        // Repositorio que siempre falla, para simular una base de datos caida
        RepositorioDeOrdenes caido = new RepositorioDeOrdenes() {
            @Override
            public void guardar(Orden orden) {
                throw new IllegalStateException("base de datos caida");
            }

            @Override
            public Optional<Orden> buscarPorId(OrdenId id) {
                return Optional.empty();
            }
        };
        var escenarioConFalla = new Escenario(caido);

        assertThatThrownBy(() -> escenarioConFalla.comprar.ejecutar(
                compra(PARQUE_A, VISITA, new Visitante(30, false), new Visitante(31, false))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base de datos caida");

        assertThat(escenarioConFalla.consultarCupo.ejecutar(PARQUE_A, VISITA)).isEqualTo(100); // compensado
    }
}
