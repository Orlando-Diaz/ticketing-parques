package com.orlandodiaz.ticketing.shared.infrastructure.outbox;

import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Lee los eventos pendientes del outbox y los entrega al bus, del mas viejo al mas nuevo.
 *
 * Garantia: AL MENOS UNA VEZ. Si el proceso muere despues de entregar y antes de marcar la fila, el evento se
 * entrega otra vez al reiniciar; por eso todos los suscriptores son idempotentes.
 * Si la entrega falla, se anota el intento y se reintenta en la siguiente pasada del programador; tras
 * {@code maximoDeIntentos} la fila queda como fallida (para que una persona la revise) y no bloquea a las demas.
 *
 * Una sola pasada a la vez dentro del proceso. Con VARIAS instancias de la app habria que reclamar las filas
 * (SELECT ... FOR UPDATE SKIP LOCKED en PostgreSQL); aqui no hace falta porque la entrega es idempotente.
 */
public class DespachadorDeOutbox {

    private static final int TAMANO_DE_LOTE = 50;

    private final AlmacenDeOutbox outbox;
    private final SerializadorDeEventos serializador;
    private final BusDeEventosEnMemoria bus;
    private final int maximoDeIntentos;
    private final ReentrantLock candado = new ReentrantLock();

    public DespachadorDeOutbox(AlmacenDeOutbox outbox, SerializadorDeEventos serializador,
                               BusDeEventosEnMemoria bus, int maximoDeIntentos) {
        if (maximoDeIntentos < 1) {
            throw new IllegalArgumentException("maximoDeIntentos debe ser al menos 1");
        }
        this.outbox = outbox;
        this.serializador = serializador;
        this.bus = bus;
        this.maximoDeIntentos = maximoDeIntentos;
    }

    /** Entrega lo pendiente. Nunca lanza: un fallo aqui no debe romper a quien lo disparo. Devuelve cuantos entrego. */
    public int despacharPendientes() {
        // Si ya hay una pasada en curso (en este hilo o en otro) no hacemos otra: la que corre recoge lo nuevo.
        if (candado.isHeldByCurrentThread() || !candado.tryLock()) {
            return 0;
        }
        try {
            int entregados = 0;
            long cursor = 0;
            while (true) {
                List<MensajeDeOutbox> lote = outbox.pendientes(cursor, TAMANO_DE_LOTE);
                if (lote.isEmpty()) {
                    return entregados;
                }
                for (MensajeDeOutbox mensaje : lote) {
                    cursor = mensaje.id(); // avanzar aunque falle: el reintento es de la proxima pasada
                    if (entregar(mensaje)) {
                        entregados++;
                    }
                }
            }
        } catch (RuntimeException e) {
            System.err.println("Fallo el despacho del outbox: " + e.getMessage());
            return 0;
        } finally {
            candado.unlock();
        }
    }

    private boolean entregar(MensajeDeOutbox mensaje) {
        try {
            EventoDeDominio evento = serializador.desdeJson(mensaje.tipo(), mensaje.payload());
            bus.entregar(evento);
            outbox.marcarPublicado(mensaje.id(), LocalDateTime.now());
            return true;
        } catch (RuntimeException e) {
            outbox.registrarFallo(mensaje.id(), e.getMessage(), maximoDeIntentos);
            return false;
        }
    }
}
