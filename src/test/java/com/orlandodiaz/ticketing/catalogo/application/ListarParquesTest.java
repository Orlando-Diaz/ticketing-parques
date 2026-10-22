package com.orlandodiaz.ticketing.catalogo.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.catalogo.infrastructure.RepositorioEnMemoriaDeParques;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import org.junit.jupiter.api.Test;

class ListarParquesTest {

    @Test
    void sinParquesDevuelveUnaListaVacia() {
        assertThat(new ListarParques(new RepositorioEnMemoriaDeParques()).ejecutar()).isEmpty();
    }

    @Test
    void devuelveTodosOrdenadosPorNombre() {
        var repositorio = new RepositorioEnMemoriaDeParques();
        repositorio.guardar(new Parque(new ParqueId("z"), "Zoologico", 100));
        repositorio.guardar(new Parque(new ParqueId("a"), "Acuario", 50));

        assertThat(new ListarParques(repositorio).ejecutar())
                .extracting(Parque::nombre).containsExactly("Acuario", "Zoologico");
    }
}
