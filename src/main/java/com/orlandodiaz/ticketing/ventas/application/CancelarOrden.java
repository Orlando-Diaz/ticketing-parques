package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.shared.application.EjecutorTransaccional;
import com.orlandodiaz.ticketing.shared.application.PublicadorDeEventos;
import com.orlandodiaz.ticketing.shared.application.ReintentoPorConflicto;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;

/**
 * CASO DE USO: cancelar una orden, devolver el cupo que ocupaba y avisar con un evento.
 *
 * Orden de los pasos (importa):
 *  1. cancelar y GUARDAR la orden, junto con la anotacion de sus eventos en el outbox (una sola transaccion):
 *     aqui se decide quien gana. Si dos cancelaciones llegan a la vez, el repositorio deja guardar solo a una;
 *     la otra recibe un conflicto, vuelve a leer, ve la orden ya CANCELADA y falla con OrdenNoCancelableException;
 *  2. solo quien gano devuelve el cupo (asi nunca se devuelve dos veces).
 * Riesgo conocido: si la devolucion del cupo falla DESPUES de guardar, la orden queda cancelada con el cupo
 * todavia ocupado. Es el lado seguro (nunca se vende de mas). Cerrarlo del todo seria que "devolver el cupo"
 * tambien sea un mensaje del outbox (ver ADR 0007).
 */
public class CancelarOrden {

    private final RepositorioDeOrdenes ordenes;
    private final CupoDeParque cupo;
    private final Reloj reloj;
    private final PublicadorDeEventos eventos;
    private final EjecutorTransaccional transaccion;

    public CancelarOrden(RepositorioDeOrdenes ordenes, CupoDeParque cupo, Reloj reloj, PublicadorDeEventos eventos) {
        this(ordenes, cupo, reloj, eventos, EjecutorTransaccional.SIN_TRANSACCION);
    }

    public CancelarOrden(RepositorioDeOrdenes ordenes, CupoDeParque cupo, Reloj reloj, PublicadorDeEventos eventos,
                         EjecutorTransaccional transaccion) {
        this.ordenes = ordenes;
        this.cupo = cupo;
        this.reloj = reloj;
        this.eventos = eventos;
        this.transaccion = transaccion;
    }

    public Orden ejecutar(OrdenId id) {
        Orden orden = ReintentoPorConflicto.obtener(ReintentoPorConflicto.INTENTOS_POR_DEFECTO, () -> {
            Orden leida = ordenes.buscarPorId(id).orElseThrow(() -> new OrdenNoEncontradaException(id));
            leida.cancelar(reloj.ahora()); // la orden decide si se puede cancelar
            // La transaccion va DENTRO de cada intento; el reintento queda fuera de ella.
            transaccion.ejecutar(() -> {
                ordenes.guardar(leida);    // si otro se nos adelanto: conflicto -> se vuelve a leer
                eventos.publicarTodos(leida.sacarEventos());
                return null;
            });
            return leida;
        });
        cupo.liberar(orden.parqueId(), orden.fechaVisita(), orden.cantidadEntradas());
        return orden;
    }
}
