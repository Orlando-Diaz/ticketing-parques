package com.orlandodiaz.ticketing.ventas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrdenEventosTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 13, 10, 0);

    private Orden nuevaOrden() {
        return Orden.crear(new ParqueId("parque-a"), LocalDate.of(2026, 10, 20),
                List.of(new LineaDeOrden(30, false, Dinero.pesos(50_000))), AHORA);
    }

    @Test
    void crearUnaOrdenRegistraElEventoOrdenConfirmada() {
        Orden orden = nuevaOrden();

        List<EventoDeDominio> eventos = orden.sacarEventos();

        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0)).isInstanceOf(OrdenConfirmada.class);
        var confirmada = (OrdenConfirmada) eventos.get(0);
        assertThat(confirmada.ordenId()).isEqualTo(orden.id());
        assertThat(confirmada.total()).isEqualTo(Dinero.pesos(50_000));
    }

    @Test
    void losEventosSeEntreganUnaSolaVez() {
        Orden orden = nuevaOrden();
        orden.sacarEventos();

        assertThat(orden.sacarEventos()).isEmpty();
    }

    @Test
    void cancelarRegistraElEventoOrdenCancelada() {
        Orden orden = nuevaOrden();
        orden.sacarEventos();

        orden.cancelar(AHORA.plusHours(1));

        assertThat(orden.sacarEventos()).singleElement().isInstanceOf(OrdenCancelada.class);
    }
}
