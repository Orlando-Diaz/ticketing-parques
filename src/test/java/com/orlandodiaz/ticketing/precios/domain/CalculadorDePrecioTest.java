package com.orlandodiaz.ticketing.precios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.precios.domain.reglas.DescuentoGrupo;
import com.orlandodiaz.ticketing.precios.domain.reglas.DescuentoResidente;
import com.orlandodiaz.ticketing.precios.domain.reglas.RecargoFinDeSemana;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadorDePrecioTest {

    private static final LocalDate MIERCOLES = LocalDate.of(2026, 10, 14);
    private static final LocalDate SABADO = LocalDate.of(2026, 10, 17);

    private static DatosDeVisita visita(int edad, boolean residente, int grupo, LocalDate fecha) {
        return new DatosDeVisita(edad, residente, grupo, fecha);
    }

    /** Parque A: niño, adulto y adulto mayor con precios distintos. */
    private static EsquemaDeTarifa tarifaPorCategoria() {
        return new TarifaPorCategoria(List.of(
                new TarifaDeCategoria(new CategoriaVisitante("Nino", 3, 10), Dinero.pesos(25_000)),
                new TarifaDeCategoria(new CategoriaVisitante("Adulto", 11, 59), Dinero.pesos(50_000)),
                new TarifaDeCategoria(CategoriaVisitante.desde("Adulto mayor", 60), Dinero.pesos(35_000))));
    }

    @Test
    void parqueConTarifaUnicaCobraLoMismoATodos() {
        var calculador = new CalculadorDePrecio(new TarifaUnica(Dinero.pesos(40_000)), List.of());

        assertThat(calculador.calcular(visita(5, false, 1, MIERCOLES)).total()).isEqualTo(Dinero.pesos(40_000));
        assertThat(calculador.calcular(visita(35, false, 1, MIERCOLES)).total()).isEqualTo(Dinero.pesos(40_000));
        assertThat(calculador.calcular(visita(70, false, 1, MIERCOLES)).total()).isEqualTo(Dinero.pesos(40_000));
    }

    @Test
    void parqueConCategoriasCobraSegunLaEdad() {
        var calculador = new CalculadorDePrecio(tarifaPorCategoria(), List.of());

        assertThat(calculador.calcular(visita(8, false, 1, MIERCOLES)).total()).isEqualTo(Dinero.pesos(25_000));
        assertThat(calculador.calcular(visita(30, false, 1, MIERCOLES)).total()).isEqualTo(Dinero.pesos(50_000));
        assertThat(calculador.calcular(visita(65, false, 1, MIERCOLES)).total()).isEqualTo(Dinero.pesos(35_000));
    }

    @Test
    void dosParquesConLaMismaVisitaPagaranDistinto() {
        var visita = visita(30, false, 1, MIERCOLES);
        var parqueA = new CalculadorDePrecio(tarifaPorCategoria(), List.of());
        var parqueB = new CalculadorDePrecio(new TarifaUnica(Dinero.pesos(40_000)), List.of());

        assertThat(parqueA.calcular(visita).total()).isEqualTo(Dinero.pesos(50_000));
        assertThat(parqueB.calcular(visita).total()).isEqualTo(Dinero.pesos(40_000));
    }

    @Test
    void fallaSiElParqueNoTieneCategoriaParaLaEdad() {
        var calculador = new CalculadorDePrecio(tarifaPorCategoria(), List.of());

        assertThatThrownBy(() -> calculador.calcular(visita(2, false, 1, MIERCOLES)))
                .isInstanceOf(SinTarifaParaEdadException.class);
    }

    @Test
    void losDescuentosNoSeAcumulanSeAplicaElMayor() {
        var calculador = new CalculadorDePrecio(tarifaPorCategoria(),
                List.of(new DescuentoResidente(20), new DescuentoGrupo(10, 15)));

        // adulto 50.000, residente (20% = 10.000) y grupo (15% = 7.500) -> solo el mayor
        DesglosePrecio r = calculador.calcular(visita(30, true, 12, MIERCOLES));
        assertThat(r.total()).isEqualTo(Dinero.pesos(40_000));
        assertThat(r.ajustesAplicados()).hasSize(1);
    }

    @Test
    void elRecargoDeFinDeSemanaSeSumaAlPrecio() {
        var calculador = new CalculadorDePrecio(tarifaPorCategoria(), List.of(new RecargoFinDeSemana(10)));

        assertThat(calculador.calcular(visita(30, false, 1, SABADO)).total()).isEqualTo(Dinero.pesos(55_000));
        assertThat(calculador.calcular(visita(30, false, 1, MIERCOLES)).total()).isEqualTo(Dinero.pesos(50_000));
    }

    @Test
    void cadaParqueConfiguraSusPropiosPorcentajes() {
        var conRecargo10 = new CalculadorDePrecio(new TarifaUnica(Dinero.pesos(40_000)),
                List.of(new RecargoFinDeSemana(10)));
        var conRecargo25 = new CalculadorDePrecio(new TarifaUnica(Dinero.pesos(40_000)),
                List.of(new RecargoFinDeSemana(25)));

        assertThat(conRecargo10.calcular(visita(30, false, 1, SABADO)).total()).isEqualTo(Dinero.pesos(44_000));
        assertThat(conRecargo25.calcular(visita(30, false, 1, SABADO)).total()).isEqualTo(Dinero.pesos(50_000));
    }
}
