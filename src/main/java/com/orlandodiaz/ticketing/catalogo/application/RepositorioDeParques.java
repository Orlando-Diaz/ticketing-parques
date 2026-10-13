package com.orlandodiaz.ticketing.catalogo.application;

import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.util.Optional;

/** Puerto de salida: como obtiene la aplicacion los parques (sin saber donde se guardan). */
public interface RepositorioDeParques {

    Optional<Parque> buscarPorId(ParqueId id);

    void guardar(Parque parque);
}
