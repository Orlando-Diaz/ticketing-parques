package com.orlandodiaz.ticketing.catalogo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.catalogo.infrastructure.RepositorioEnMemoriaDeDisponibilidad;
import com.orlandodiaz.ticketing.catalogo.infrastructure.RepositorioEnMemoriaDeParques;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LiberarCupoTest {

    private static final ParqueId PARQUE = new ParqueId("parque-a");
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);

    @Test
    void liberarDevuelveElCupoReservado() {
        var parques = new RepositorioEnMemoriaDeParques();
        var disponibilidades = new RepositorioEnMemoriaDeDisponibilidad();
        parques.guardar(new Parque(PARQUE, "Parque A", 100));
        var reservar = new ReservarCupo(parques, disponibilidades);
        var liberar = new LiberarCupo(disponibilidades);
        var consultar = new ConsultarCupoDisponible(parques, disponibilidades);

        reservar.ejecutar(PARQUE, FECHA, 30);
        liberar.ejecutar(PARQUE, FECHA, 10);

        assertThat(consultar.ejecutar(PARQUE, FECHA)).isEqualTo(80);
    }

    @Test
    void fallaSiNuncaSeReservoEnEsaFecha() {
        var liberar = new LiberarCupo(new RepositorioEnMemoriaDeDisponibilidad());

        assertThatThrownBy(() -> liberar.ejecutar(PARQUE, FECHA, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
