package com.orlandodiaz.ticketing.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DineroTest {

    @Test
    void sumaYRestaDevuelvenDineroNuevo() {
        Dinero a = Dinero.pesos(50_000);
        assertThat(a.sumar(Dinero.pesos(5_000))).isEqualTo(Dinero.pesos(55_000));
        assertThat(a.restar(Dinero.pesos(20_000))).isEqualTo(Dinero.pesos(30_000));
        assertThat(a).isEqualTo(Dinero.pesos(50_000)); // el original no cambia
    }

    @Test
    void calculaPorcentajes() {
        assertThat(Dinero.pesos(50_000).porcentaje(30)).isEqualTo(Dinero.pesos(15_000));
    }

    @Test
    void noPermiteDineroNegativo() {
        assertThatThrownBy(() -> Dinero.pesos(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Dinero.pesos(100).restar(Dinero.pesos(200)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noPermitePorcentajeFueraDeRango() {
        assertThatThrownBy(() -> Dinero.pesos(100).porcentaje(101))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
