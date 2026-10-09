package com.orlandodiaz.ticketing.precios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.precios.domain.reglas.DescuentoAdultoMayor;
import com.orlandodiaz.ticketing.precios.domain.reglas.DescuentoGrupo;
import com.orlandodiaz.ticketing.precios.domain.reglas.DescuentoNino;
import com.orlandodiaz.ticketing.precios.domain.reglas.DescuentoResidente;
import com.orlandodiaz.ticketing.precios.domain.reglas.RecargoFinDeSemana;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadorDePrecioTest {

    private static final Dinero BASE = Dinero.pesos(50_000);
    private static final LocalDate MIERCOLES = LocalDate.of(2026, 10, 14);
    private static final LocalDate SABADO = LocalDate.of(2026, 10, 17);

    private final CalculadorDePrecio calculador = new CalculadorDePrecio(List.of(
            new DescuentoNino(),
            new DescuentoAdultoMayor(),
            new DescuentoResidente(),
            new DescuentoGrupo(),
            new RecargoFinDeSemana()));

    private ContextoDePrecio contexto(int edad, boolean residente, int grupo, LocalDate fecha) {
        return new ContextoDePrecio(BASE, edad, residente, grupo, fecha);
    }

    @Test
    void adultoSinBeneficiosPagaElPrecioBase() {
        DesglosePrecio r = calculador.calcular(contexto(30, false, 1, MIERCOLES));
        assertThat(r.total()).isEqualTo(Dinero.pesos(50_000));
        assertThat(r.ajustesAplicados()).isEmpty();
    }

    @Test
    void ninoPagaLaMitad() {
        assertThat(calculador.calcular(contexto(8, false, 1, MIERCOLES)).total())
                .isEqualTo(Dinero.pesos(25_000));
    }

    @Test
    void adultoMayorTiene30PorCiento() {
        assertThat(calculador.calcular(contexto(65, false, 1, MIERCOLES)).total())
                .isEqualTo(Dinero.pesos(35_000));
    }

    @Test
    void losDescuentosNoSeAcumulanSeAplicaElMayor() {
        // nino (50%) + residente (20%) + grupo (15%) -> solo el 50%
        DesglosePrecio r = calculador.calcular(contexto(8, true, 12, MIERCOLES));
        assertThat(r.total()).isEqualTo(Dinero.pesos(25_000));
        assertThat(r.ajustesAplicados()).hasSize(1);
    }

    @Test
    void elRecargoDeFinDeSemanaSeSumaAlDescuento() {
        // nino: -25.000, sabado: +5.000 -> 30.000
        DesglosePrecio r = calculador.calcular(contexto(8, false, 1, SABADO));
        assertThat(r.total()).isEqualTo(Dinero.pesos(30_000));
        assertThat(r.ajustesAplicados()).hasSize(2);
    }
}
