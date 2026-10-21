package com.orlandodiaz.ticketing.catalogo.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.catalogo.domain.CupoAgotadoException;
import com.orlandodiaz.ticketing.catalogo.domain.DisponibilidadDiaria;
import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.catalogo.infrastructure.RepositorioEnMemoriaDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.infrastructure.RepositorioEnMemoriaDeParques;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * El caso clasico de sobreventa, SIN depender de la suerte del reloj:
 * quedan 1 entrada y dos compradores. Una barrera obliga a que AMBOS lean "queda 1" antes de que cualquiera guarde.
 * Sin proteccion, los dos reservan con exito (se vende una entrada que no existe).
 * Con bloqueo optimista, el segundo se entera del conflicto, vuelve a leer y recibe CupoAgotadoException.
 */
class ReservarCupoConcurrenteTest {

    private static final ParqueId PARQUE = new ParqueId("parque-concurrente");
    private static final LocalDate DIA = LocalDate.of(2026, 10, 20);

    /** Decorador de prueba: deja que las proximas N lecturas se esperen entre si antes de devolver su resultado. */
    private static class LecturasSincronizadas implements RepositorioDeDisponibilidad {
        private final RepositorioDeDisponibilidad real;
        private final CyclicBarrier barrera = new CyclicBarrier(2);
        private final AtomicInteger lecturasPorSincronizar = new AtomicInteger(0);

        LecturasSincronizadas(RepositorioDeDisponibilidad real) {
            this.real = real;
        }

        void sincronizarLasProximas(int lecturas) {
            lecturasPorSincronizar.set(lecturas);
        }

        @Override
        public Optional<DisponibilidadDiaria> buscar(ParqueId parqueId, LocalDate fecha) {
            var leida = real.buscar(parqueId, fecha);
            if (lecturasPorSincronizar.getAndDecrement() > 0) {
                try {
                    barrera.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Los dos compradores no coincidieron", e);
                }
            }
            return leida;
        }

        @Override
        public void guardar(DisponibilidadDiaria disponibilidad) {
            real.guardar(disponibilidad);
        }
    }

    @Test
    void dosCompradoresPorLaUltimaEntradaSoloUnoLaConsigue() throws Exception {
        var parques = new RepositorioEnMemoriaDeParques();
        var disponibilidades = new LecturasSincronizadas(new RepositorioEnMemoriaDeDisponibilidad());
        parques.guardar(new Parque(PARQUE, "Parque concurrente", 10));
        var reservarCupo = new ReservarCupo(parques, disponibilidades);
        var consultarCupo = new ConsultarCupoDisponible(parques, disponibilidades);

        reservarCupo.ejecutar(PARQUE, DIA, 9); // queda exactamente 1 entrada
        disponibilidades.sincronizarLasProximas(2); // los dos compradores leen "queda 1" antes de guardar

        Callable<Throwable> comprarUna = () -> {
            try {
                reservarCupo.ejecutar(PARQUE, DIA, 1);
                return null; // exito
            } catch (Throwable t) {
                return t;
            }
        };
        var pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Throwable>> resultados = pool.invokeAll(List.of(comprarUna, comprarUna));
            var fallos = resultados.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).toList();

            assertThat(fallos).filteredOn(t -> t == null).as("compras exitosas").hasSize(1);
            assertThat(fallos).filteredOn(t -> t != null).singleElement().isInstanceOf(CupoAgotadoException.class);
            assertThat(consultarCupo.ejecutar(PARQUE, DIA)).isZero();
        } finally {
            pool.shutdownNow();
        }
    }
}
