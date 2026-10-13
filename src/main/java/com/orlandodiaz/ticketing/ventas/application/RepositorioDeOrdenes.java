package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;
import java.util.Optional;

public interface RepositorioDeOrdenes {

    void guardar(Orden orden);

    Optional<Orden> buscarPorId(OrdenId id);
}
