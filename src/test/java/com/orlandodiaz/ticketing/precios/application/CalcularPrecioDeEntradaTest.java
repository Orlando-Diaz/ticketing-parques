package com.orlandodiaz.ticketing.precios.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.precios.domain.CategoriaVisitante;
import com.orlandodiaz.ticketing.precios.domain.ConfiguracionDePrecios;
import com.orlandodiaz.ticketing.precios.domain.DatosDeVisita;
import com.orlandodiaz.ticketing.precios.domain.TarifaDeCategoria;
import com.orlandodiaz.ticketing.precios.domain.TarifaPorCategoria;
import com.orlandodiaz.ticketing.precios.domain.TarifaUnica;
import com.orlandodiaz.ticketing.precios.domain.reglas.DescuentoResidente;
import com.orlandodiaz.ticketing.precios.infrastructure.RepositorioEnMemoriaDeConfiguracionDePrecios;
import com.orlandodiaz.ticketing.shared.domain.Dinero;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CalcularPrecioDeEntradaTest {

    private static final ParqueId PARQUE_A = new ParqueId("parque-a");
    private static final ParqueId PARQUE_B = new ParqueId("parque-b");
    private static final LocalDate MIERCOLES = LocalDate.of(2026, 10, 14);

    private CalcularPrecioDeEntrada calcularPrecio;

    @BeforeEach
    void preparar() {
        var repositorio = new RepositorioEnMemoriaDeConfiguracionDePrecios();

        // Parque A: precios por categoria + descuento de residente del 20 %
        repositorio.guardar(new ConfiguracionDePrecios(PARQUE_A,
                new TarifaPorCategoria(List.of(
                        new TarifaDeCategoria(new CategoriaVisitante("Nino", 3, 10), Dinero.pesos(25_000)),
                        new TarifaDeCategoria(CategoriaVisitante.desde("Adulto", 11), Dinero.pesos(50_000)))),
                List.of(new DescuentoResidente(20))));

        // Parque B: tarifa unica y sin reglas extra
        repositorio.guardar(new ConfiguracionDePrecios(PARQUE_B,
                new TarifaUnica(Dinero.pesos(40_000)), List.of()));

        calcularPrecio = new CalcularPrecioDeEntrada(repositorio);
    }

    @Test
    void usaLaConfiguracionDelParquePedido() {
        var visita = new DatosDeVisita(30, true, 1, MIERCOLES);

        assertThat(calcularPrecio.ejecutar(PARQUE_A, visita).total()).isEqualTo(Dinero.pesos(40_000)); // 50.000 - 20 %
        assertThat(calcularPrecio.ejecutar(PARQUE_B, visita).total()).isEqualTo(Dinero.pesos(40_000)); // tarifa unica
    }

    @Test
    void laMismaVisitaCuestaDistintoSegunElParque() {
        var nino = new DatosDeVisita(8, false, 1, MIERCOLES);

        assertThat(calcularPrecio.ejecutar(PARQUE_A, nino).total()).isEqualTo(Dinero.pesos(25_000));
        assertThat(calcularPrecio.ejecutar(PARQUE_B, nino).total()).isEqualTo(Dinero.pesos(40_000));
    }

    @Test
    void fallaSiElParqueNoTieneConfiguracion() {
        var visita = new DatosDeVisita(30, false, 1, MIERCOLES);

        assertThatThrownBy(() -> calcularPrecio.ejecutar(new ParqueId("desconocido"), visita))
                .isInstanceOf(ParqueSinConfiguracionDePreciosException.class)
                .hasMessageContaining("desconocido");
    }
}
