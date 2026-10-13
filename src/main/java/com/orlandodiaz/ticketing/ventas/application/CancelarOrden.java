package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;

/** CASO DE USO: cancelar una orden y devolver el cupo que ocupaba. */
public class CancelarOrden {

    private final RepositorioDeOrdenes ordenes;
    private final CupoDeParque cupo;
    private final Reloj reloj;

    public CancelarOrden(RepositorioDeOrdenes ordenes, CupoDeParque cupo, Reloj reloj) {
        this.ordenes = ordenes;
        this.cupo = cupo;
        this.reloj = reloj;
    }

    public Orden ejecutar(OrdenId id) {
        Orden orden = ordenes.buscarPorId(id).orElseThrow(() -> new OrdenNoEncontradaException(id));
        orden.cancelar(reloj.ahora().toLocalDate()); // la orden decide si se puede cancelar
        cupo.liberar(orden.parqueId(), orden.fechaVisita(), orden.cantidadEntradas());
        ordenes.guardar(orden);
        return orden;
    }
}
