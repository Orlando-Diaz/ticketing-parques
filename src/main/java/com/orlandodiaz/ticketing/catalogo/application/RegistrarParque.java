package com.orlandodiaz.ticketing.catalogo.application;

import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;

/** CASO DE USO: dar de alta un parque nuevo en el sistema. */
public class RegistrarParque {

    private final RepositorioDeParques parques;

    public RegistrarParque(RepositorioDeParques parques) {
        this.parques = parques;
    }

    public Parque ejecutar(ParqueId id, String nombre, int cupoDiario) {
        if (parques.buscarPorId(id).isPresent()) {
            throw new ParqueYaExisteException(id);
        }
        Parque parque = new Parque(id, nombre, cupoDiario);
        parques.guardar(parque);
        return parque;
    }
}
