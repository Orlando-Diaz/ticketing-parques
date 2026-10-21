package com.orlandodiaz.ticketing.ventas.application;

import static com.orlandodiaz.ticketing.ventas.application.Escenario.PARQUE_A;
import static com.orlandodiaz.ticketing.ventas.application.Escenario.VISITA;
import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.ventas.domain.EstadoOrden;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;
import com.orlandodiaz.ticketing.ventas.domain.OrdenNoCancelableException;
import com.orlandodiaz.ticketing.ventas.domain.Visitante;
import com.orlandodiaz.ticketing.ventas.infrastructure.RepositorioEnMemoriaDeOrdenes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Dos cancelaciones simultaneas de la MISMA orden: el cupo debe devolverse una sola vez.
 * Antes de proteger la orden con version, las dos leian CONFIRMADA, las dos cancelaban y las dos devolvian
 * el cupo (el parque terminaba con mas cupo del que tiene).
 */
class CancelarOrdenConcurrenteTest {

    /** Decorador de prueba: las proximas N lecturas se esperan entre si antes de devolver su resultado. */
    private static class LecturasSincronizadas implements RepositorioDeOrdenes {
        private final RepositorioDeOrdenes real;
        private final CyclicBarrier barrera = new CyclicBarrier(2);
        private final AtomicInteger lecturasPorSincronizar = new AtomicInteger(0);

        LecturasSincronizadas(RepositorioDeOrdenes real) {
            this.real = real;
        }

        void sincronizarLasProximas(int lecturas) {
            lecturasPorSincronizar.set(lecturas);
        }

        @Override
        public void guardar(Orden orden) {
            real.guardar(orden);
        }

        @Override
        public Optional<Orden> buscarPorId(OrdenId id) {
            var leida = real.buscarPorId(id);
            if (lecturasPorSincronizar.getAndDecrement() > 0) {
                try {
                    barrera.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Las dos cancelaciones no coincidieron", e);
                }
            }
            return leida;
        }
    }

    private static List<Throwable> correrJuntos(int hilos, Callable<Throwable> tarea) throws Exception {
        var pool = Executors.newFixedThreadPool(hilos);
        try {
            var largada = new CountDownLatch(1);
            List<Future<Throwable>> futuros = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return tarea.call();
                }));
            }
            largada.countDown();
            List<Throwable> resultados = new ArrayList<>();
            for (var f : futuros) {
                resultados.add(f.get(30, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    private static Throwable intentarCancelar(CancelarOrden cancelar, OrdenId id) {
        try {
            cancelar.ejecutar(id);
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    @Test
    void dosCancelacionesALaVezDevuelvenElCupoUnaSolaVez() throws Exception {
        var ordenes = new LecturasSincronizadas(new RepositorioEnMemoriaDeOrdenes());
        var escenario = new Escenario(ordenes);
        Orden orden = escenario.comprar.ejecutar(new ComprarEntradasComando(PARQUE_A, VISITA,
                List.of(new Visitante(30, false), new Visitante(31, false), new Visitante(8, false))));
        assertThat(escenario.consultarCupo.ejecutar(PARQUE_A, VISITA)).isEqualTo(97);

        ordenes.sincronizarLasProximas(2); // las dos cancelaciones leen la orden CONFIRMADA antes de guardar
        var resultados = correrJuntos(2, () -> intentarCancelar(escenario.cancelar, orden.id()));

        assertThat(resultados).filteredOn(t -> t == null).as("cancelaciones exitosas").hasSize(1);
        assertThat(resultados).filteredOn(t -> t != null).singleElement()
                .isInstanceOf(OrdenNoCancelableException.class);
        assertThat(escenario.consultarCupo.ejecutar(PARQUE_A, VISITA)).as("cupo").isEqualTo(100);
        assertThat(ordenes.buscarPorId(orden.id()).orElseThrow().estado()).isEqualTo(EstadoOrden.CANCELADA);
    }

    @Test
    void muchasCancelacionesALaVezSoloUnaGana() throws Exception {
        var escenario = new Escenario(new RepositorioEnMemoriaDeOrdenes());
        Orden orden = escenario.comprar.ejecutar(new ComprarEntradasComando(PARQUE_A, VISITA,
                List.of(new Visitante(30, false), new Visitante(31, false))));

        var resultados = correrJuntos(20, () -> intentarCancelar(escenario.cancelar, orden.id()));

        assertThat(resultados).filteredOn(t -> t == null).as("cancelaciones exitosas").hasSize(1);
        assertThat(resultados).filteredOn(t -> t != null)
                .allSatisfy(t -> assertThat(t).isInstanceOf(OrdenNoCancelableException.class));
        assertThat(escenario.consultarCupo.ejecutar(PARQUE_A, VISITA)).as("cupo").isEqualTo(100);
    }
}
