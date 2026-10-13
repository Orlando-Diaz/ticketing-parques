package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.shared.application.PublicadorDeEventos;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;

/** CASO DE USO: cancelar una orden, devolver el cupo que ocupaba y avisar con un evento. */
public class CancelarOrden {

    private final RepositorioDeOrdenes ordenes;
    private final CupoDeParque cupo;
    private final Reloj reloj;
    private final PublicadorDeEventos eventos;

    public CancelarOrden(RepositorioDeOrdenes ordenes, CupoDeParque cupo, Reloj reloj, PublicadorDeEventos eventos) {
        this.ordenes = ordenes;
        this.cupo = cupo;
        this.reloj = reloj;
        this.eventos = eventos;
    }

    public Orden ejecutar(OrdenId id) {
        Orden orden = ordenes.buscarPorId(id).orElseThrow(() -> new OrdenNoEncontradaException(id));
        orden.cancelar(reloj.ahora()); // la orden decide si se puede cancelar
        cupo.liberar(orden.parqueId(), orden.fechaVisita(), orden.cantidadEntradas());
        ordenes.guardar(orden);
        eventos.publicarTodos(orden.sacarEventos());
        return orden;
    }
}
