package com.orlandodiaz.ticketing.ventas.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.shared.application.EjecutorTransaccional;
import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import com.orlandodiaz.ticketing.ventas.domain.Visitante;
import com.orlandodiaz.ticketing.ventas.infrastructure.RepositorioEnMemoriaDeOrdenes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Guardar la orden y publicar sus eventos deben ocurrir DENTRO de la transaccion (asi el outbox queda atomico
 * con la venta). Un ejecutor que anota cuando esta "abierto" lo comprueba sin necesitar base de datos.
 */
class PublicacionTransaccionalTest {

    private final List<String> bitacora = new ArrayList<>();
    private boolean dentro = false;

    private final EjecutorTransaccional ejecutor = new EjecutorTransaccional() {
        @Override
        public <T> T ejecutar(Supplier<T> trabajo) {
            dentro = true;
            try {
                return trabajo.get();
            } finally {
                dentro = false;
            }
        }
    };

    private void publicar(EventoDeDominio evento) {
        bitacora.add((dentro ? "dentro:" : "fuera:") + evento.getClass().getSimpleName());
    }

    @Test
    void laCompraPublicaDentroDeLaTransaccion() {
        var escenario = new Escenario(new RepositorioEnMemoriaDeOrdenes(), this::publicar, ejecutor);

        escenario.comprar.ejecutar(new ComprarEntradasComando(Escenario.PARQUE_A, Escenario.VISITA,
                List.of(new Visitante(30, false))));

        assertThat(bitacora).containsExactly("dentro:OrdenConfirmada");
    }

    @Test
    void laCancelacionPublicaDentroDeLaTransaccion() {
        var escenario = new Escenario(new RepositorioEnMemoriaDeOrdenes(), this::publicar, ejecutor);
        var orden = escenario.comprar.ejecutar(new ComprarEntradasComando(Escenario.PARQUE_A, Escenario.VISITA,
                List.of(new Visitante(30, false))));
        bitacora.clear();

        escenario.cancelar.ejecutar(orden.id());

        assertThat(bitacora).containsExactly("dentro:OrdenCancelada");
    }

    @Test
    void siGuardarFallaNoSePublicaNada() {
        var ordenes = new RepositorioEnMemoriaDeOrdenes();
        var escenario = new Escenario(ordenes, this::publicar, ejecutor);
        var orden = escenario.comprar.ejecutar(new ComprarEntradasComando(Escenario.PARQUE_A, Escenario.VISITA,
                List.of(new Visitante(30, false))));
        bitacora.clear();

        // un ejecutor que falla al abrir la transaccion: la cancelacion no llega a publicar
        EjecutorTransaccional roto = new EjecutorTransaccional() {
            @Override
            public <T> T ejecutar(Supplier<T> trabajo) {
                throw new IllegalStateException("la base se cayo");
            }
        };
        var otro = new Escenario(ordenes, this::publicar, roto);
        try {
            otro.cancelar.ejecutar(orden.id());
        } catch (IllegalStateException esperado) {
            // lo importante es lo que NO paso
        }

        assertThat(bitacora).isEmpty();
    }
}
