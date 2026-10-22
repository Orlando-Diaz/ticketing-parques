package com.orlandodiaz.ticketing.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BusDeEventosEnMemoriaTest {

    record EventoA(String texto, LocalDateTime ocurridoEn) implements EventoDeDominio {
    }

    record EventoB(LocalDateTime ocurridoEn) implements EventoDeDominio {
    }

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 13, 10, 0);

    @Test
    void entregaElEventoSoloAQuienSeSuscribioASuTipo() {
        var bus = new BusDeEventosEnMemoria();
        List<String> recibidosA = new ArrayList<>();
        List<String> recibidosB = new ArrayList<>();
        bus.suscribir(EventoA.class, e -> recibidosA.add(e.texto()));
        bus.suscribir(EventoB.class, e -> recibidosB.add("b"));

        bus.publicar(new EventoA("hola", AHORA));

        assertThat(recibidosA).containsExactly("hola");
        assertThat(recibidosB).isEmpty();
    }

    @Test
    void publicarSinSuscriptoresNoHaceNada() {
        new BusDeEventosEnMemoria().publicar(new EventoB(AHORA)); // no debe lanzar nada
    }

    @Test
    void unSuscriptorQueFallaNoImpideQueLosDemasRecibanElEvento() {
        List<String> errores = new ArrayList<>();
        var bus = new BusDeEventosEnMemoria((evento, error) -> errores.add(error.getMessage()));
        List<String> recibidos = new ArrayList<>();
        bus.suscribir(EventoA.class, e -> {
            throw new IllegalStateException("fallo el primero");
        });
        bus.suscribir(EventoA.class, e -> recibidos.add(e.texto()));

        bus.publicar(new EventoA("hola", AHORA));

        assertThat(recibidos).containsExactly("hola");
        assertThat(errores).containsExactly("fallo el primero");
    }

    @Test
    void entregarEsEstrictoPeroTodosRecibenElEvento() {
        var bus = new BusDeEventosEnMemoria();
        List<String> recibidos = new ArrayList<>();
        bus.suscribir(EventoA.class, e -> {
            throw new IllegalStateException("fallo el primero");
        });
        bus.suscribir(EventoA.class, e -> recibidos.add(e.texto()));

        assertThatThrownBy(() -> bus.entregar(new EventoA("hola", AHORA)))
                .isInstanceOf(FalloDeEntregaDeEventoException.class)
                .hasRootCauseMessage("fallo el primero");
        assertThat(recibidos).containsExactly("hola"); // el segundo recibio igual
    }

    @Test
    void entregarSinFallosNoLanzaNada() {
        var bus = new BusDeEventosEnMemoria();
        bus.suscribir(EventoA.class, e -> { });

        bus.entregar(new EventoA("hola", AHORA));
    }
}
