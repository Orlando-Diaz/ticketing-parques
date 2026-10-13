package com.orlandodiaz.ticketing.ventas.application;

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
 *  4. crear y guardar la orden. Si esto falla, DEVOLVEMOS el cupo (compensacion);
 *  5. publicar los eventos. Va FUERA del try: la venta ya es un hecho, y si un suscriptor
 *     (ej. facturacion) falla, no se deshace la compra.
 */
public class ComprarEntradas {

    private final PreciosDeEntrada precios;
    private final CupoDeParque cupo;
    private final RepositorioDeOrdenes ordenes;
    private final Reloj reloj;
    private final PublicadorDeEventos eventos;

    public ComprarEntradas(PreciosDeEntrada precios, CupoDeParque cupo, RepositorioDeOrdenes ordenes,
                           Reloj reloj, PublicadorDeEventos eventos) {
        this.precios = precios;
        this.cupo = cupo;
        this.ordenes = ordenes;
        this.reloj = reloj;
        this.eventos = eventos;
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
            ordenes.guardar(orden);
        } catch (RuntimeException e) {
            cupo.liberar(comando.parqueId(), comando.fechaVisita(), cantidad);
            throw e;
        }

        eventos.publicarTodos(orden.sacarEventos());
        return orden;
    }
}
