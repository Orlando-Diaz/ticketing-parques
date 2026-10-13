package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;

/** CASO DE USO (consulta): obtener una orden por su id. */
public class ConsultarOrden {

    private final RepositorioDeOrdenes ordenes;

    public ConsultarOrden(RepositorioDeOrdenes ordenes) {
        this.ordenes = ordenes;
    }

    public Orden ejecutar(OrdenId id) {
        return ordenes.buscarPorId(id).orElseThrow(() -> new OrdenNoEncontradaException(id));
    }
}
