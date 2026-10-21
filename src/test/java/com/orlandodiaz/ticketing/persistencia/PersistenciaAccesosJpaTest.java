package com.orlandodiaz.ticketing.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.accesos.application.RepositorioDePases;
import com.orlandodiaz.ticketing.accesos.application.ValidarIngreso;
import com.orlandodiaz.ticketing.accesos.domain.EntradasDelPaseAgotadasException;
import com.orlandodiaz.ticketing.accesos.domain.EstadoPase;
import com.orlandodiaz.ticketing.accesos.domain.Pase;
import com.orlandodiaz.ticketing.shared.application.ConflictoDeConcurrenciaException;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

/** Pases e ingresos contra una base real (H2 con las migraciones de Flyway), incluida la concurrencia. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class PersistenciaAccesosJpaTest {

    private static final ParqueId PARQUE = new ParqueId("parque-accesos");
    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));

    @Autowired RepositorioDePases pases;
    @Autowired ValidarIngreso validarIngreso;
    @Autowired JdbcTemplate jdbc;

    private static String unico() {
        return UUID.randomUUID().toString();
    }

    private Pase paseGuardado(int entradas) {
        Pase pase = Pase.emitir("cod-" + unico(), "orden-" + unico(), PARQUE, HOY, entradas, LocalDateTime.now());
        pases.guardar(pase);
        return pase;
    }

    @Test
    void paseConSusIngresosSeGuardaYSeRecupera() {
        Pase nuevo = paseGuardado(4);

        var leido = pases.buscarPorCodigo(nuevo.codigo()).orElseThrow();
        assertThat(leido.version()).isZero();
        leido.registrarIngreso(2, "norte", HOY.atTime(10, 0));
        pases.guardar(leido);

        var releido = pases.buscarPorOrden(nuevo.ordenRef()).orElseThrow();
        releido.registrarIngreso(1, "sur", HOY.atTime(11, 30));
        pases.guardar(releido);

        var finalmente = pases.buscarPorCodigo(nuevo.codigo()).orElseThrow();
        assertThat(finalmente.entradasUsadas()).isEqualTo(3);
        assertThat(finalmente.entradasRestantes()).isEqualTo(1);
        assertThat(finalmente.estado()).isEqualTo(EstadoPase.ACTIVO);
        assertThat(finalmente.version()).isEqualTo(2);
        assertThat(finalmente.ingresos()).hasSize(2);
        assertThat(finalmente.ingresos().get(0).puerta()).isEqualTo("norte");
        assertThat(finalmente.ingresos().get(0).momento()).isEqualTo(HOY.atTime(10, 0));
        assertThat(finalmente.ingresos().get(1).personas()).isEqualTo(1);
    }

    @Test
    void anularElPaseSeConserva() {
        Pase nuevo = paseGuardado(2);
        var leido = pases.buscarPorCodigo(nuevo.codigo()).orElseThrow();
        leido.anular();
        pases.guardar(leido);

        assertThat(pases.buscarPorCodigo(nuevo.codigo()).orElseThrow().estado()).isEqualTo(EstadoPase.ANULADO);
    }

    @Test
    void dosPuertasConLaMismaLecturaSoloUnaGuarda() {
        Pase nuevo = paseGuardado(3);
        var puertaA = pases.buscarPorCodigo(nuevo.codigo()).orElseThrow();
        var puertaB = pases.buscarPorCodigo(nuevo.codigo()).orElseThrow();

        puertaA.registrarIngreso(2, "norte", HOY.atTime(10, 0));
        pases.guardar(puertaA);

        puertaB.registrarIngreso(2, "sur", HOY.atTime(10, 0)); // para ella aun "quedaban 3"
        assertThatThrownBy(() -> pases.guardar(puertaB)).isInstanceOf(ConflictoDeConcurrenciaException.class);
        assertThat(pases.buscarPorCodigo(nuevo.codigo()).orElseThrow().entradasUsadas()).isEqualTo(2);
    }

    @Test
    void noSePuedeRepetirNiElCodigoNiLaOrden() {
        Pase existente = paseGuardado(2);

        var mismoCodigo = Pase.emitir(existente.codigo(), "orden-" + unico(), PARQUE, HOY, 2, LocalDateTime.now());
        var mismaOrden = Pase.emitir("cod-" + unico(), existente.ordenRef(), PARQUE, HOY, 2, LocalDateTime.now());

        assertThatThrownBy(() -> pases.guardar(mismoCodigo)).isInstanceOf(ConflictoDeConcurrenciaException.class);
        assertThatThrownBy(() -> pases.guardar(mismaOrden)).isInstanceOf(ConflictoDeConcurrenciaException.class);
    }

    @Test
    void laBaseDeDatosEsLaUltimaDefensaContraElSobreingreso() {
        Pase nuevo = paseGuardado(2);

        assertThatThrownBy(() -> jdbc.update(
                "update pase set entradas_usadas = entradas_totales + 1 where id = ?", nuevo.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void variasPuertasALaVezNuncaDejanEntrarMasDeLoComprado() throws Exception {
        Pase nuevo = paseGuardado(5);

        int puertas = 15;
        var largada = new CountDownLatch(1);
        Callable<Throwable> escanear = () -> {
            largada.await();
            try {
                validarIngreso.ejecutar(nuevo.codigo(), 1, "norte");
                return null;
            } catch (Throwable t) {
                return t;
            }
        };
        var pool = Executors.newFixedThreadPool(puertas);
        try {
            List<Future<Throwable>> futuros = new ArrayList<>();
            for (int i = 0; i < puertas; i++) {
                futuros.add(pool.submit(escanear));
            }
            largada.countDown();

            int exitos = 0;
            List<Throwable> inesperados = new ArrayList<>();
            for (var futuro : futuros) {
                Throwable resultado = futuro.get(60, TimeUnit.SECONDS);
                if (resultado == null) {
                    exitos++;
                } else if (!(resultado instanceof EntradasDelPaseAgotadasException)) {
                    inesperados.add(resultado);
                }
            }

            assertThat(inesperados).as("errores inesperados").isEmpty();
            assertThat(exitos).as("ingresos exitosos").isEqualTo(5);
            var guardado = pases.buscarPorCodigo(nuevo.codigo()).orElseThrow();
            assertThat(guardado.entradasUsadas()).isEqualTo(5);
            assertThat(guardado.ingresos()).hasSize(5);
        } finally {
            pool.shutdownNow();
        }
    }
}
