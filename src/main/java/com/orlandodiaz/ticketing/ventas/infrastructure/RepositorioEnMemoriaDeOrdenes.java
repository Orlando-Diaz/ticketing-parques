package com.orlandodiaz.ticketing.ventas.infrastructure;

import com.orlandodiaz.ticketing.ventas.application.RepositorioDeOrdenes;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Adaptador en memoria (desarrollo y pruebas). */
public class RepositorioEnMemoriaDeOrdenes implements RepositorioDeOrdenes {

    private final Map<OrdenId, Orden> almacen = new ConcurrentHashMap<>();

    @Override
    public void guardar(Orden orden) {
        almacen.put(orden.id(), orden);
    }

    @Override
    public Optional<Orden> buscarPorId(OrdenId id) {
        return Optional.ofNullable(almacen.get(id));
    }

    /** Solo para pruebas; no forma parte del puerto. */
    public int cantidad() {
        return almacen.size();
    }
}
