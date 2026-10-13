package com.orlandodiaz.ticketing.catalogo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DisponibilidadDiariaTest {

    private static final ParqueId PARQUE = new ParqueId("parque-a");
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);

    private DisponibilidadDiaria conCupo(int cupo) {
        return DisponibilidadDiaria.nueva(PARQUE, FECHA, cupo);
    }

    @Test
    void reservarDescuentaDelCupo() {
        var d = conCupo(100);
        d.reservar(30);
        assertThat(d.vendidas()).isEqualTo(30);
        assertThat(d.disponibles()).isEqualTo(70);
    }

    @Test
    void sePuedeReservarJustoHastaElLimite() {
        var d = conCupo(10);
        d.reservar(10);
        assertThat(d.disponibles()).isZero();
    }

    @Test
    void noSePuedeVenderMasQueElCupo() {
        var d = conCupo(10);
        d.reservar(8);

        assertThatThrownBy(() -> d.reservar(3)).isInstanceOf(CupoAgotadoException.class);
        assertThat(d.vendidas()).isEqualTo(8); // una reserva fallida no cambia nada
    }

    @Test
    void liberarDevuelveCupo() {
        var d = conCupo(10);
        d.reservar(6);
        d.liberar(4);
        assertThat(d.disponibles()).isEqualTo(8);
    }

    @Test
    void noSePuedeLiberarMasDeLoVendido() {
        var d = conCupo(10);
        d.reservar(2);
        assertThatThrownBy(() -> d.liberar(3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laCantidadDebeSerPositiva() {
        var d = conCupo(10);
        assertThatThrownBy(() -> d.reservar(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> d.reservar(-5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noSePuedeBajarElCupoPorDebajoDeLoVendido() {
        var d = conCupo(100);
        d.reservar(60);

        assertThatThrownBy(() -> d.ajustarCupo(50)).isInstanceOf(IllegalArgumentException.class);
        d.ajustarCupo(60);
        assertThat(d.disponibles()).isZero();
    }
}
