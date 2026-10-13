package com.orlandodiaz.ticketing.catalogo.infrastructure;

import com.orlandodiaz.ticketing.catalogo.application.RepositorioDeParques;
import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Adaptador en memoria (desarrollo y pruebas). */
public class RepositorioEnMemoriaDeParques implements RepositorioDeParques {

    private final Map<ParqueId, Parque> almacen = new ConcurrentHashMap<>();

    @Override
    public Optional<Parque> buscarPorId(ParqueId id) {
        return Optional.ofNullable(almacen.get(id));
    }

    @Override
    public void guardar(Parque parque) {
        almacen.put(parque.id(), parque);
    }
}
