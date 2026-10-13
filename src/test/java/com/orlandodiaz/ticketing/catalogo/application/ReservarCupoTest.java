package com.orlandodiaz.ticketing.catalogo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.catalogo.domain.CupoAgotadoException;
import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.catalogo.infrastructure.RepositorioEnMemoriaDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.infrastructure.RepositorioEnMemoriaDeParques;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservarCupoTest {

    private static final ParqueId PARQUE = new ParqueId("parque-a");
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 20);

    private ReservarCupo reservarCupo;
    private ConsultarCupoDisponible consultarCupo;

    @BeforeEach
    void preparar() {
        var parques = new RepositorioEnMemoriaDeParques();
        var disponibilidades = new RepositorioEnMemoriaDeDisponibilidad();
        parques.guardar(new Parque(PARQUE, "Parque A", 100));

        reservarCupo = new ReservarCupo(parques, disponibilidades);
        consultarCupo = new ConsultarCupoDisponible(parques, disponibilidades);
    }

    @Test
    void sinReservasElCupoEsElPorDefectoDelParque() {
        assertThat(consultarCupo.ejecutar(PARQUE, LUNES)).isEqualTo(100);
    }

    @Test
    void lasReservasSeAcumulanEnLaMismaFecha() {
        reservarCupo.ejecutar(PARQUE, LUNES, 40);
        reservarCupo.ejecutar(PARQUE, LUNES, 25);

        assertThat(consultarCupo.ejecutar(PARQUE, LUNES)).isEqualTo(35);
    }

    @Test
    void cadaFechaTieneSuPropioCupo() {
        reservarCupo.ejecutar(PARQUE, LUNES, 100);

        assertThat(consultarCupo.ejecutar(PARQUE, LUNES)).isZero();
        assertThat(consultarCupo.ejecutar(PARQUE, MARTES)).isEqualTo(100);
    }

    @Test
    void noSePuedeReservarMasDelCupo() {
        reservarCupo.ejecutar(PARQUE, LUNES, 90);

        assertThatThrownBy(() -> reservarCupo.ejecutar(PARQUE, LUNES, 11))
                .isInstanceOf(CupoAgotadoException.class);
        assertThat(consultarCupo.ejecutar(PARQUE, LUNES)).isEqualTo(10);
    }

    @Test
    void fallaSiElParqueNoExiste() {
        assertThatThrownBy(() -> reservarCupo.ejecutar(new ParqueId("fantasma"), LUNES, 1))
                .isInstanceOf(ParqueNoEncontradoException.class);
    }
}
