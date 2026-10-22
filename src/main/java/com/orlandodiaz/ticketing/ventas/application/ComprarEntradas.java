package com.orlandodiaz.ticketing.ventas.application;

import com.orlandodiaz.ticketing.shared.application.EjecutorTransaccional;
import com.orlandodiaz.ticketing.shared.application.PublicadorDeEventos;
import com.orlandodiaz.ticketing.ventas.domain.LineaDeOrden;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CASO DE USO: comprar entradas. Orquesta, no decide reglas de negocio.
 *
 * Orden de los pasos (importa):
 *  1. validar la fecha;
 *  2. calcular los precios: si falla (ej. edad sin tarifa) todavia no tocamos el cupo;
 *  3. reservar el cupo;
 *  4. crear la orden y, en UNA transaccion, guardarla y publicar sus eventos (con el outbox, "publicar" es anotar
 *     el evento en la base: se confirma o se deshace junto con la orden). Si esto falla, DEVOLVEMOS el cupo
 *     (compensacion). Los suscriptores (ej. facturacion) corren despues; si fallan, no se deshace la compra.
 */
public class ComprarEntradas {

    private final PreciosDeEntrada precios;
    private final CupoDeParque cupo;
    private final RepositorioDeOrdenes ordenes;
    private final Reloj reloj;
    private final PublicadorDeEventos eventos;
    private final EjecutorTransaccional transaccion;

    public ComprarEntradas(PreciosDeEntrada precios, CupoDeParque cupo, RepositorioDeOrdenes ordenes,
                           Reloj reloj, PublicadorDeEventos eventos) {
        this(precios, cupo, ordenes, reloj, eventos, EjecutorTransaccional.SIN_TRANSACCION);
    }

    public ComprarEntradas(PreciosDeEntrada precios, CupoDeParque cupo, RepositorioDeOrdenes ordenes,
                           Reloj reloj, PublicadorDeEventos eventos, EjecutorTransaccional transaccion) {
        this.precios = precios;
        this.cupo = cupo;
        this.ordenes = ordenes;
        this.reloj = reloj;
        this.eventos = eventos;
        this.transaccion = transaccion;
    }

    public Orden ejecutar(ComprarEntradasComando comando) {
        LocalDateTime ahora = reloj.ahora();
        Orden.exigirFechaValida(comando.fechaVisita(), ahora.toLocalDate());

        int cantidad = comando.visitantes().size();
        List<LineaDeOrden> lineas = comando.visitantes().stream()
                .map(v -> new LineaDeOrden(v.edad(), v.residente(),
                        precios.precioPara(comando.parqueId(), v, cantidad, comando.fechaVisita())))
                .toList();

        cupo.reservar(comando.parqueId(), comando.fechaVisita(), cantidad);
        Orden orden;
        try {
            orden = Orden.crear(comando.parqueId(), comando.fechaVisita(), lineas, ahora);
            Orden nueva = orden;
            transaccion.ejecutar(() -> {
                ordenes.guardar(nueva);
                eventos.publicarTodos(nueva.sacarEventos());
                return null;
            });
        } catch (RuntimeException e) {
            cupo.liberar(comando.parqueId(), comando.fechaVisita(), cantidad);
            throw e;
        }

        return orden;
    }
}
