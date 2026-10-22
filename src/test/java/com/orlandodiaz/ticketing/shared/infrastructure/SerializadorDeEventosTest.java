package com.orlandodiaz.ticketing.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.accesos.domain.IngresoRegistrado;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.shared.infrastructure.outbox.SerializadorDeEventos;
import com.orlandodiaz.ticketing.ventas.domain.LineaDeOrden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenCancelada;
import com.orlandodiaz.ticketing.ventas.domain.OrdenConfirmada;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Lo que se guarda en el outbox tiene que volver igualito: si no, los suscriptores recibirian datos distintos. */
class SerializadorDeEventosTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 13, 10, 30, 15);
    private static final LocalDate VISITA = LocalDate.of(2026, 10, 20);

    private final SerializadorDeEventos serializador = new SerializadorDeEventos()
            .registrar(OrdenConfirmada.class)
            .registrar(OrdenCancelada.class)
            .registrar(IngresoRegistrado.class);

    private EventoDeDominio ida(EventoDeDominio evento) {
        return serializador.desdeJson(serializador.nombreDe(evento), serializador.aJson(evento));
    }

    @Test
    void ordenConfirmadaVuelveIgual() {
        var evento = new OrdenConfirmada(new OrdenId("o-1"), new ParqueId("parque-a"), VISITA,
                List.of(new LineaDeOrden(8, true, Dinero.pesos(25_000)),
                        new LineaDeOrden(35, false, Dinero.pesos(50_000))),
                Dinero.pesos(75_000), AHORA);

        assertThat(ida(evento)).isEqualTo(evento);
    }

    @Test
    void ordenCanceladaVuelveIgual() {
        var evento = new OrdenCancelada(new OrdenId("o-2"), new ParqueId("parque-a"), VISITA, Dinero.pesos(50_000), AHORA);

        assertThat(ida(evento)).isEqualTo(evento);
    }

    @Test
    void ingresoRegistradoVuelveIgual() {
        var evento = new IngresoRegistrado("o-3", "pase-1", new ParqueId("parque-b"), VISITA, 2, 3, "norte", AHORA);

        assertThat(ida(evento)).isEqualTo(evento);
    }

    @Test
    void elNombreGuardadoEsElNombreSimpleDeLaClase() {
        var evento = new OrdenCancelada(new OrdenId("o-2"), new ParqueId("p"), VISITA, Dinero.CERO, AHORA);

        assertThat(serializador.nombreDe(evento)).isEqualTo("OrdenCancelada");
    }

    @Test
    void unEventoSinRegistrarSeRechazaDeEntrada() {
        var sinRegistrar = new SerializadorDeEventos();
        var evento = new OrdenCancelada(new OrdenId("o-2"), new ParqueId("p"), VISITA, Dinero.CERO, AHORA);

        assertThatThrownBy(() -> sinRegistrar.aJson(evento)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unTipoDesconocidoNoSeDeserializa() {
        assertThatThrownBy(() -> serializador.desdeJson("EventoQueNoExiste", "{}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
