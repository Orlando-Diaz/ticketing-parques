package com.orlandodiaz.ticketing.ventas.application;

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
 *  4. crear y guardar la orden. Si esto falla, DEVOLVEMOS el cupo (compensacion)
 *     para no dejar entradas "fantasma" ocupando lugar.
 */
public class ComprarEntradas {

    private final PreciosDeEntrada precios;
    private final CupoDeParque cupo;
    private final RepositorioDeOrdenes ordenes;
    private final Reloj reloj;

    public ComprarEntradas(PreciosDeEntrada precios, CupoDeParque cupo, RepositorioDeOrdenes ordenes, Reloj reloj) {
        this.precios = precios;
        this.cupo = cupo;
        this.ordenes = ordenes;
        this.reloj = reloj;
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
        try {
            Orden orden = Orden.crear(comando.parqueId(), comando.fechaVisita(), lineas, ahora);
            ordenes.guardar(orden);
            return orden;
        } catch (RuntimeException e) {
            cupo.liberar(comando.parqueId(), comando.fechaVisita(), cantidad);
            throw e;
        }
    }
}
