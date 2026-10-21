package com.orlandodiaz.ticketing.accesos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.accesos.domain.EntradasDelPaseAgotadasException;
import com.orlandodiaz.ticketing.accesos.domain.FechaDePaseIncorrectaException;
import com.orlandodiaz.ticketing.accesos.infrastructure.RepositorioEnMemoriaDePases;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidarIngresoTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 20);

    private RepositorioEnMemoriaDePases pases;
    private AtomicReference<LocalDateTime> ahora;
    private ValidarIngreso validarIngreso;
    private ConsultarPase consultarPase;

    @BeforeEach
    void preparar() {
        pases = new RepositorioEnMemoriaDePases();
        ahora = new AtomicReference<>(DIA.atTime(10, 0));
        validarIngreso = new ValidarIngreso(pases, ahora::get);
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
    void sePuedeConsultarElPaseDeUnaOrden() {
        assertThat(consultarPase.porOrden("orden-1").codigo()).isEqualTo("QR-ABC");
        assertThatThrownBy(() -> consultarPase.porOrden("otra-orden"))
                .isInstanceOf(PaseNoEncontradoException.class);
    }
}
