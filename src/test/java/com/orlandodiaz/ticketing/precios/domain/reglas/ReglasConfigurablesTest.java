package com.orlandodiaz.ticketing.precios.domain.reglas;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReglasConfigurablesTest {

    @Test
    void rechazanPorcentajesFueraDeRango() {
        assertThatThrownBy(() -> new DescuentoResidente(101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecargoFinDeSemana(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DescuentoGrupo(10, 150)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elDescuentoDeGrupoExigeUnTamanoMinimoValido() {
        assertThatThrownBy(() -> new DescuentoGrupo(0, 10)).isInstanceOf(IllegalArgumentException.class);
    }
}
