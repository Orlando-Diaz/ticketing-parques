package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.shared.application.PublicadorDeEventos;
import com.orlandodiaz.ticketing.shared.application.ReintentoPorConflicto;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;

/**
 * CASO DE USO: cancelar una orden, devolver el cupo que ocupaba y avisar con un evento.
 *
 * Orden de los pasos (importa):
 *  1. cancelar y GUARDAR la orden: aqui se decide quien gana. Si dos cancelaciones llegan a la vez, el
 *     repositorio deja guardar solo a una; la otra recibe un conflicto, vuelve a leer, ve la orden ya
 *     CANCELADA y falla con OrdenNoCancelableException;
 *  2. solo quien gano devuelve el cupo (asi nunca se devuelve dos veces);
 *  3. publicar los eventos.
 * Riesgo conocido: si la devolucion del cupo falla DESPUES de guardar, la orden queda cancelada con el cupo
 * todavia ocupado. Es el lado seguro (nunca se vende de mas); la solucion de fondo es el patron outbox.
 */
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
        Orden orden = ReintentoPorConflicto.obtener(ReintentoPorConflicto.INTENTOS_POR_DEFECTO, () -> {
            Orden leida = ordenes.buscarPorId(id).orElseThrow(() -> new OrdenNoEncontradaException(id));
            leida.cancelar(reloj.ahora()); // la orden decide si se puede cancelar
            ordenes.guardar(leida);        // si otro se nos adelanto: conflicto -> se vuelve a leer
            return leida;
        });
        cupo.liberar(orden.parqueId(), orden.fechaVisita(), orden.cantidadEntradas());
        eventos.publicarTodos(orden.sacarEventos());
        return orden;
    }
}
