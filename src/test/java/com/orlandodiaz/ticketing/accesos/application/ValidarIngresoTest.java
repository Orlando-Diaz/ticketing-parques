package com.orlandodiaz.ticketing.accesos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.accesos.domain.EntradasDelPaseAgotadasException;
import com.orlandodiaz.ticketing.accesos.domain.FechaDePaseIncorrectaException;
import com.orlandodiaz.ticketing.accesos.infrastructure.RepositorioEnMemoriaDePases;
import com.orlandodiaz.ticketing.accesos.domain.IngresoRegistrado;
import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidarIngresoTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 20);

    private RepositorioEnMemoriaDePases pases;
    private AtomicReference<LocalDateTime> ahora;
    private ValidarIngreso validarIngreso;
    private ConsultarPase consultarPase;
    private List<EventoDeDominio> eventos;

    @BeforeEach
    void preparar() {
        pases = new RepositorioEnMemoriaDePases();
        ahora = new AtomicReference<>(DIA.atTime(10, 0));
        eventos = new ArrayList<>();
        validarIngreso = new ValidarIngreso(pases, ahora::get, eventos::add);
        consultarPase = new ConsultarPase(pases);
        new EmitirPase(pases, () -> "QR-ABC").ejecutar(new EmitirPaseComando(
                "orden-1", new ParqueId("parque-a"), DIA, 4, DIA.minusDays(3).atTime(9, 0)));
    }

    @Test
    void elIngresoQuedaRegistradoYDescuentaEntradas() {
        var pase = validarIngreso.ejecutar("QR-ABC", 3, "norte");

        assertThat(pase.entradasRestantes()).isEqualTo(1);
        var guardado = consultarPase.porCodigo("QR-ABC");
        assertThat(guardado.entradasUsadas()).isEqualTo(3);
        assertThat(guardado.ingresos()).hasSize(1);
        assertThat(guardado.ingresos().get(0).puerta()).isEqualTo("norte");
        assertThat(guardado.ingresos().get(0).momento()).isEqualTo(DIA.atTime(10, 0));
    }

    @Test
    void unCodigoDesconocidoSeRechazaSinDarPistas() {
        assertThatThrownBy(() -> validarIngreso.ejecutar("QR-NO-EXISTE", 1, null))
                .isInstanceOf(PaseNoEncontradoException.class)
                .hasMessage("Codigo de pase no valido");
    }

    @Test
    void unIngresoRechazadoNoCambiaElPase() {
        validarIngreso.ejecutar("QR-ABC", 3, null);

        assertThatThrownBy(() -> validarIngreso.ejecutar("QR-ABC", 2, null))
                .isInstanceOf(EntradasDelPaseAgotadasException.class);
        assertThat(consultarPase.porCodigo("QR-ABC").entradasUsadas()).isEqualTo(3);
    }

    @Test
    void elPaseNoValeOtroDia() {
        ahora.set(DIA.plusDays(1).atTime(9, 0));

        assertThatThrownBy(() -> validarIngreso.ejecutar("QR-ABC", 1, null))
                .isInstanceOf(FechaDePaseIncorrectaException.class);
    }

    @Test
    void cadaIngresoExitosoPublicaUnEventoConSuNumero() {
        validarIngreso.ejecutar("QR-ABC", 2, "norte");
        validarIngreso.ejecutar("QR-ABC", 1, "sur");

        assertThat(eventos).hasSize(2).allSatisfy(e -> assertThat(e).isInstanceOf(IngresoRegistrado.class));
        var primero = (IngresoRegistrado) eventos.get(0);
        var segundo = (IngresoRegistrado) eventos.get(1);
        assertThat(primero.ordenRef()).isEqualTo("orden-1");
        assertThat(primero.numeroDeIngreso()).isZero();
        assertThat(primero.personas()).isEqualTo(2);
        assertThat(primero.puerta()).isEqualTo("norte");
        assertThat(segundo.numeroDeIngreso()).isEqualTo(1);
        assertThat(segundo.puerta()).isEqualTo("sur");
    }

    @Test
    void unIngresoRechazadoNoPublicaNada() {
        validarIngreso.ejecutar("QR-ABC", 4, null);
        eventos.clear();

        assertThatThrownBy(() -> validarIngreso.ejecutar("QR-ABC", 1, null))
                .isInstanceOf(EntradasDelPaseAgotadasException.class);

        assertThat(eventos).isEmpty();
    }

    @Test
    void sePuedeConsultarElPaseDeUnaOrden() {
        assertThat(consultarPase.porOrden("orden-1").codigo()).isEqualTo("QR-ABC");
        assertThatThrownBy(() -> consultarPase.porOrden("otra-orden"))
                .isInstanceOf(PaseNoEncontradoException.class);
    }
}
