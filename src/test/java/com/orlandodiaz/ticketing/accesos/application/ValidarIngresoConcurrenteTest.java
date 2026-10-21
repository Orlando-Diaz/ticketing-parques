package com.orlandodiaz.ticketing.accesos.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.accesos.domain.EntradasDelPaseAgotadasException;
import com.orlandodiaz.ticketing.accesos.domain.Pase;
import com.orlandodiaz.ticketing.accesos.infrastructure.RepositorioEnMemoriaDePases;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
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
 * Dos puertas escaneando el MISMO QR a la vez no pueden dejar entrar a mas gente de la que compro.
 * La barrera obliga a que las dos lean el pase (con las mismas entradas libres) antes de que cualquiera guarde.
 */
class ValidarIngresoConcurrenteTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 20);

    private static class LecturasSincronizadas implements RepositorioDePases {
        private final RepositorioDePases real;
        private final CyclicBarrier barrera = new CyclicBarrier(2);
        private final AtomicInteger lecturasPorSincronizar = new AtomicInteger(0);

        LecturasSincronizadas(RepositorioDePases real) {
            this.real = real;
        }

        void sincronizarLasProximas(int lecturas) {
            lecturasPorSincronizar.set(lecturas);
        }

        @Override
        public Optional<Pase> buscarPorCodigo(String codigo) {
            var leido = real.buscarPorCodigo(codigo);
            if (lecturasPorSincronizar.getAndDecrement() > 0) {
                try {
                    barrera.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Las dos puertas no coincidieron", e);
                }
            }
            return leido;
        }

        @Override
        public Optional<Pase> buscarPorOrden(String ordenRef) {
            return real.buscarPorOrden(ordenRef);
        }

        @Override
        public void guardar(Pase pase) {
            real.guardar(pase);
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

    private static Throwable intentarEntrar(ValidarIngreso validar, String codigo, int personas) {
        try {
            validar.ejecutar(codigo, personas, "norte");
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private static void emitir(RepositorioDePases pases, int entradas) {
        new EmitirPase(pases, () -> "QR-1").ejecutar(new EmitirPaseComando(
                "orden-1", new ParqueId("parque-a"), DIA, entradas, DIA.minusDays(2).atTime(9, 0)));
    }

    @Test
    void dosPuertasConElMismoQrSoloUnaDejaEntrarAlGrupo() throws Exception {
        var pases = new LecturasSincronizadas(new RepositorioEnMemoriaDePases());
        emitir(pases, 4);
        var validar = new ValidarIngreso(pases, () -> DIA.atTime(10, 0));

        pases.sincronizarLasProximas(2); // las dos puertas leen "quedan 4" antes de guardar
        var resultados = correrJuntos(2, () -> intentarEntrar(validar, "QR-1", 3));

        assertThat(resultados).filteredOn(t -> t == null).as("ingresos exitosos").hasSize(1);
        assertThat(resultados).filteredOn(t -> t != null).singleElement()
                .isInstanceOf(EntradasDelPaseAgotadasException.class);
        assertThat(new ConsultarPase(pases).porCodigo("QR-1").entradasUsadas()).isEqualTo(3);
    }

    @Test
    void muchasPuertasALaVezNuncaDejanEntrarMasDeLoComprado() throws Exception {
        var pases = new RepositorioEnMemoriaDePases();
        emitir(pases, 5);
        var validar = new ValidarIngreso(pases, () -> DIA.atTime(10, 0));

        var resultados = correrJuntos(20, () -> intentarEntrar(validar, "QR-1", 1));

        assertThat(resultados).filteredOn(t -> t == null).as("ingresos exitosos").hasSize(5);
        assertThat(resultados).filteredOn(t -> t != null)
                .allSatisfy(t -> assertThat(t).isInstanceOf(EntradasDelPaseAgotadasException.class));
        assertThat(new ConsultarPase(pases).porCodigo("QR-1").entradasUsadas()).isEqualTo(5);
    }
}
