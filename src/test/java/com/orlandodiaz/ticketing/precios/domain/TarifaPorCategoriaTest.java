package com.orlandodiaz.ticketing.precios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.shared.domain.Dinero;
import java.util.List;
import org.junit.jupiter.api.Test;

class TarifaPorCategoriaTest {

    @Test
    void noPermiteCategoriasQueSeSolapan() {
        var nino = new TarifaDeCategoria(new CategoriaVisitante("Nino", 3, 12), Dinero.pesos(25_000));
        var adulto = new TarifaDeCategoria(new CategoriaVisitante("Adulto", 10, 59), Dinero.pesos(50_000));

        assertThatThrownBy(() -> new TarifaPorCategoria(List.of(nino, adulto)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("se solapan");
    }

    @Test
    void exigeAlMenosUnaCategoria() {
        assertThatThrownBy(() -> new TarifaPorCategoria(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void loslimitesDeEdadSonInclusivos() {
        var nino = new TarifaDeCategoria(new CategoriaVisitante("Nino", 3, 10), Dinero.pesos(25_000));
        var adulto = new TarifaDeCategoria(new CategoriaVisitante("Adulto", 11, 59), Dinero.pesos(50_000));
        var esquema = new TarifaPorCategoria(List.of(nino, adulto));

        assertThat(esquema.precioPara(10)).isEqualTo(Dinero.pesos(25_000));
        assertThat(esquema.precioPara(11)).isEqualTo(Dinero.pesos(50_000));
    }

    @Test
    void rechazaRangosDeEdadInvalidos() {
        assertThatThrownBy(() -> new CategoriaVisitante("Mal", 10, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
