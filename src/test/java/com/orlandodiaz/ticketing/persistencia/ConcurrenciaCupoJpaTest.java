package com.orlandodiaz.ticketing.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.catalogo.application.ConflictoDeConcurrenciaException;
import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeParques;
import com.orlandodiaz.ticketing.catalogo.application.ReservarCupo;
import com.orlandodiaz.ticketing.catalogo.domain.CupoAgotadoException;
import com.orlandodiaz.ticketing.catalogo.domain.DisponibilidadDiaria;
import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Concurrencia del cupo contra una base real (H2 con las migraciones de Flyway). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ConcurrenciaCupoJpaTest {

    private static final LocalDate VISITA = LocalDate.of(2026, 10, 20);

    @Autowired RepositorioDeParques parques;
    @Autowired RepositorioDeDisponibilidad disponibilidades;
    @Autowired JdbcTemplate jdbc;

    private static ParqueId idUnico() {
        return new ParqueId("conc-" + UUID.randomUUID());
    }

    @Test
    void muchosCompradoresALaVezNuncaVendenMasQueElCupo() throws Exception {
        ParqueId id = idUnico();
        parques.guardar(new Parque(id, "Parque concurrente", 5));
        // intentos de sobra: con 20 compradores y 5 entradas, nadie debe quedarse sin turno por cansancio
        var reservarCupo = new ReservarCupo(parques, disponibilidades, 50);

        int compradores = 20;
        var largada = new CountDownLatch(1);
        Callable<Throwable> comprarUna = () -> {
            largada.await(); // todos arrancan juntos
            try {
                reservarCupo.ejecutar(id, VISITA, 1);
                return null;
            } catch (Throwable t) {
                return t;
            }
        };

        var pool = Executors.newFixedThreadPool(compradores);
        try {
            List<Future<Throwable>> futuros = new ArrayList<>();
            for (int i = 0; i < compradores; i++) {
                futuros.add(pool.submit(comprarUna));
            }
            largada.countDown();

            int exitos = 0;
            int agotados = 0;
            List<Throwable> inesperados = new ArrayList<>();
            for (var futuro : futuros) {
                Throwable resultado = futuro.get(60, TimeUnit.SECONDS);
                if (resultado == null) {
                    exitos++;
                } else if (resultado instanceof CupoAgotadoException) {
                    agotados++;
                } else {
                    inesperados.add(resultado);
                }
            }

            assertThat(inesperados).as("errores inesperados").isEmpty();
            assertThat(exitos).as("compras exitosas").isEqualTo(5);
            assertThat(agotados).as("rechazadas por cupo agotado").isEqualTo(15);
            assertThat(disponibilidades.buscar(id, VISITA).orElseThrow().vendidas())
                    .as("vendidas guardadas en la base").isEqualTo(5);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void guardarConUnaLecturaVencidaEsRechazado() {
        ParqueId id = idUnico();
        parques.guardar(new Parque(id, "Parque", 10));
        var inicial = DisponibilidadDiaria.nueva(id, VISITA, 10);
        inicial.reservar(1);
        disponibilidades.guardar(inicial);

        // dos compradores leen el mismo cupo (misma version)...
        var copiaA = disponibilidades.buscar(id, VISITA).orElseThrow();
        var copiaB = disponibilidades.buscar(id, VISITA).orElseThrow();

        copiaA.reservar(1);
        disponibilidades.guardar(copiaA); // ...el primero guarda bien

        copiaB.reservar(1); // el segundo trabajo con datos viejos
        assertThatThrownBy(() -> disponibilidades.guardar(copiaB))
                .isInstanceOf(ConflictoDeConcurrenciaException.class);
        assertThat(disponibilidades.buscar(id, VISITA).orElseThrow().vendidas()).isEqualTo(2); // no se pisaron
    }

    @Test
    void siDosCreanElMismoDiaALaVezSoloUnoGana() {
        ParqueId id = idUnico();
        parques.guardar(new Parque(id, "Parque", 10));
        var primero = DisponibilidadDiaria.nueva(id, VISITA, 10);
        var segundo = DisponibilidadDiaria.nueva(id, VISITA, 10);
        primero.reservar(3);
        segundo.reservar(4);

        disponibilidades.guardar(primero);
        assertThatThrownBy(() -> disponibilidades.guardar(segundo))
                .isInstanceOf(ConflictoDeConcurrenciaException.class);
        assertThat(disponibilidades.buscar(id, VISITA).orElseThrow().vendidas()).isEqualTo(3);
    }

    @Test
    void laBaseDeDatosEsLaUltimaDefensaContraLaSobreventa() {
        ParqueId id = idUnico();
        parques.guardar(new Parque(id, "Parque", 10));
        disponibilidades.guardar(DisponibilidadDiaria.nueva(id, VISITA, 10));

        // aunque un bug de la aplicacion se saltara todas las reglas, el CHECK de la tabla lo frena
        assertThatThrownBy(() -> jdbc.update(
                "update disponibilidad_diaria set vendidas = cupo_maximo + 1 where parque_id = ?", id.valor()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
