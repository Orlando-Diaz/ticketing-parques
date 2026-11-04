package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.ventas.domain.Orden;
import java.util.List;

/** CASO DE USO: las compras de un cliente (su perfil), las mas recientes primero. */
public class ListarMisOrdenes {

    private final RepositorioDeOrdenes ordenes;

    public ListarMisOrdenes(RepositorioDeOrdenes ordenes) {
        this.ordenes = ordenes;
    }

    public List<Orden> ejecutar(String cliente) {
        if (cliente == null || cliente.isBlank()) {
            return List.of();
        }
        return ordenes.buscarPorCliente(cliente);
    }
}
