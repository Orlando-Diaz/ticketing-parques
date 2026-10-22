package com.orlandodiaz.ticketing.shared.infrastructure;

import com.orlandodiaz.ticketing.shared.application.PublicadorDeEventos;
import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Adaptador: bus de eventos en memoria. Entrega cada evento, en el mismo hilo, a quienes se suscribieron a su tipo.
 *
 * Decision importante: si un suscriptor falla, NO se rompe la operacion que publico el evento
 * (la venta ya ocurrio) ni se impide que los demas suscriptores lo reciban. El error se reporta aparte.
 */
public class BusDeEventosEnMemoria implements PublicadorDeEventos {

    private final Map<Class<?>, List<Consumer<EventoDeDominio>>> manejadores = new ConcurrentHashMap<>();
    private final BiConsumer<EventoDeDominio, RuntimeException> alFallar;

    public BusDeEventosEnMemoria() {
        this((evento, error) -> System.err.println(
                "Fallo al manejar " + evento.getClass().getSimpleName() + ": " + error.getMessage()));
    }

    public BusDeEventosEnMemoria(BiConsumer<EventoDeDominio, RuntimeException> alFallar) {
        this.alFallar = alFallar;
    }

    public <E extends EventoDeDominio> void suscribir(Class<E> tipo, Consumer<? super E> manejador) {
        manejadores.computeIfAbsent(tipo, t -> new CopyOnWriteArrayList<>())
                .add(evento -> manejador.accept(tipo.cast(evento)));
    }

    @Override
    public void publicar(EventoDeDominio evento) {
        for (Consumer<EventoDeDominio> manejador : manejadores.getOrDefault(evento.getClass(), List.of())) {
            try {
                manejador.accept(evento);
            } catch (RuntimeException e) {
                alFallar.accept(evento, e);
            }
        }
    }

    /**
     * Entrega ESTRICTA, para quien puede reintentar (el despachador del outbox): todos los suscriptores reciben el
     * evento aunque alguno falle, y al final, si hubo fallos, se lanza {@link FalloDeEntregaDeEventoException}.
     * Como los suscriptores son idempotentes, volver a entregar el evento a los que ya lo procesaron es seguro.
     */
    public void entregar(EventoDeDominio evento) {
        FalloDeEntregaDeEventoException fallo = null;
        for (Consumer<EventoDeDominio> manejador : manejadores.getOrDefault(evento.getClass(), List.of())) {
            try {
                manejador.accept(evento);
            } catch (RuntimeException e) {
                if (fallo == null) {
                    fallo = new FalloDeEntregaDeEventoException(evento, e);
                } else {
                    fallo.addSuppressed(e);
                }
            }
        }
        if (fallo != null) {
            throw fallo;
        }
    }
}
