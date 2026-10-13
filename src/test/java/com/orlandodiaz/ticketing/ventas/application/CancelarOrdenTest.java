package com.orlandodiaz.ticketing.ventas.application;

import static com.orlandodiaz.ticketing.ventas.application.Escenario.PARQUE_A;
import static com.orlandodiaz.ticketing.ventas.application.Escenario.VISITA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.ventas.domain.EstadoOrden;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;
import com.orlandodiaz.ticketing.ventas.domain.OrdenNoCancelableException;
import com.orlandodiaz.ticketing.ventas.domain.Visitante;
import com.orlandodiaz.ticketing.ventas.infrastructure.RepositorioEnMemoriaDeOrdenes;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CancelarOrdenTest {

    private Escenario escenario;
    private Orden orden;

    @BeforeEach
    void preparar() {
        escenario = new Escenario(new RepositorioEnMemoriaDeOrdenes());
        orden = escenario.comprar.ejecutar(new ComprarEntradasComando(PARQUE_A, VISITA,
                List.of(new Visitante(30, false), new Visitante(31, false), new Visitante(8, false))));
    }

    @Test
    void cancelarMarcaLaOrdenYDevuelveElCupo() {
        assertThat(escenario.consultarCupo.ejecutar(PARQUE_A, VISITA)).isEqualTo(97);

        Orden cancelada = escenario.cancelar.ejecutar(orden.id());

        assertThat(cancelada.estado()).isEqualTo(EstadoOrden.CANCELADA);
        assertThat(escenario.consultarCupo.ejecutar(PARQUE_A, VISITA)).isEqualTo(100);
    }

    @Test
    void noSePuedeCancelarDosVeces() {
        escenario.cancelar.ejecutar(orden.id());

        assertThatThrownBy(() -> escenario.cancelar.ejecutar(orden.id()))
                .isInstanceOf(OrdenNoCancelableException.class);
        assertThat(escenario.consultarCupo.ejecutar(PARQUE_A, VISITA)).isEqualTo(100); // no se libero de mas
    }

    @Test
    void fallaSiLaOrdenNoExiste() {
        assertThatThrownBy(() -> escenario.cancelar.ejecutar(new OrdenId("no-existe")))
                .isInstanceOf(OrdenNoEncontradaException.class);
    }
}
