package com.orlandodiaz.ticketing.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeParques;
import com.orlandodiaz.ticketing.catalogo.application.ReservarCupo;
import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.shared.application.ConflictoDeConcurrenciaException;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.ventas.application.CancelarOrden;
import com.orlandodiaz.ticketing.ventas.application.RepositorioDeOrdenes;
import com.orlandodiaz.ticketing.ventas.domain.EstadoOrden;
import com.orlandodiaz.ticketing.ventas.domain.LineaDeOrden;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenNoCancelableException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Concurrencia de las ordenes contra una base real (H2 con las migraciones de Flyway). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ConcurrenciaOrdenJpaTest {

    // lejos en el futuro para que la cancelacion siempre sea valida sin importar el dia en que corra el test
    private static final LocalDate VISITA = LocalDate.now().plusDays(30);

    @Autowired RepositorioDeParques parques;
    @Autowired RepositorioDeDisponibilidad disponibilidades;
    @Autowired RepositorioDeOrdenes ordenes;
    @Autowired ReservarCupo reservarCupo;
    @Autowired CancelarOrden cancelarOrden;

    private Orden ordenGuardada(ParqueId parque, int entradas) {
        List<LineaDeOrden> lineas = new ArrayList<>();
        for (int i = 0; i < entradas; i++) {
            lineas.add(new LineaDeOrden(30, false, Dinero.pesos(50_000)));
        }
        Orden orden = Orden.crear(parque, VISITA, lineas, LocalDateTime.now());
        ordenes.guardar(orden);
        return orden;
    }

    @Test
    void guardarUnaOrdenConUnaLecturaVencidaEsRechazado() {
        ParqueId parque = new ParqueId("ord-" + UUID.randomUUID());
        parques.guardar(new Parque(parque, "Parque", 10));
        Orden creada = ordenGuardada(parque, 2);

        var copiaA = ordenes.buscarPorId(creada.id()).orElseThrow();
        var copiaB = ordenes.buscarPorId(creada.id()).orElseThrow();
        copiaA.cancelar(LocalDateTime.now());
        ordenes.guardar(copiaA); // la primera cancelacion guarda bien

        copiaB.cancelar(LocalDateTime.now()); // la segunda trabajo con datos viejos
        assertThatThrownBy(() -> ordenes.guardar(copiaB)).isInstanceOf(ConflictoDeConcurrenciaException.class);
        assertThat(ordenes.buscarPorId(creada.id()).orElseThrow().estado()).isEqualTo(EstadoOrden.CANCELADA);
    }

    @Test
    void variasCancelacionesALaVezDevuelvenElCupoUnaSolaVez() throws Exception {
        ParqueId parque = new ParqueId("ord-" + UUID.randomUUID());
        parques.guardar(new Parque(parque, "Parque", 10));
        reservarCupo.ejecutar(parque, VISITA, 3); // la compra ocupo 3 entradas
        Orden orden = ordenGuardada(parque, 3);

        int cancelaciones = 10;
        var largada = new CountDownLatch(1);
        Callable<Throwable> cancelar = () -> {
            largada.await();
            try {
                cancelarOrden.ejecutar(orden.id());
                return null;
            } catch (Throwable t) {
                return t;
            }
        };
        var pool = Executors.newFixedThreadPool(cancelaciones);
        try {
            List<Future<Throwable>> futuros = new ArrayList<>();
            for (int i = 0; i < cancelaciones; i++) {
                futuros.add(pool.submit(cancelar));
            }
            largada.countDown();

            int exitos = 0;
            List<Throwable> inesperados = new ArrayList<>();
            for (var futuro : futuros) {
                Throwable resultado = futuro.get(60, TimeUnit.SECONDS);
                if (resultado == null) {
                    exitos++;
                } else if (!(resultado instanceof OrdenNoCancelableException)) {
                    inesperados.add(resultado);
                }
            }

            assertThat(inesperados).as("errores inesperados").isEmpty();
            assertThat(exitos).as("cancelaciones exitosas").isEqualTo(1);
            assertThat(disponibilidades.buscar(parque, VISITA).orElseThrow().vendidas())
                    .as("vendidas despues de cancelar (el cupo se devolvio una sola vez)").isZero();
        } finally {
            pool.shutdownNow();
        }
    }
}
