package com.orlandodiaz.ticketing.notificaciones.infrastructure;

import com.orlandodiaz.ticketing.catalogo.application.ListarParques;
import com.orlandodiaz.ticketing.notificaciones.application.NombresDeParques;

/** ADAPTADOR: pregunta al catalogo el nombre del parque. Es el unico punto donde notificaciones conoce al catalogo. */
public class NombresDeParquesDelCatalogo implements NombresDeParques {

    private final ListarParques listarParques;

    public NombresDeParquesDelCatalogo(ListarParques listarParques) {
        this.listarParques = listarParques;
    }

    @Override
    public String nombreDe(String parqueId) {
        return listarParques.ejecutar().stream()
                .filter(p -> p.id().valor().equals(parqueId))
                .map(p -> p.nombre())
                .findFirst()
                .orElse(parqueId);
    }
}
