package com.orlandodiaz.ticketing.catalogo.application;

import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import java.util.List;

/** CASO DE USO: los parques disponibles, para que el visitante elija a cual ir (solo consulta). */
public class ListarParques {

    private final RepositorioDeParques parques;

    public ListarParques(RepositorioDeParques parques) {
        this.parques = parques;
    }

    public List<Parque> ejecutar() {
        return parques.listar();
    }
}
