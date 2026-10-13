package com.orlandodiaz.ticketing.facturacion.infrastructure;

import com.orlandodiaz.ticketing.facturacion.application.AnularFacturaDeOrden;
import com.orlandodiaz.ticketing.facturacion.application.EmitirFactura;
import com.orlandodiaz.ticketing.facturacion.application.EmitirFacturaComando;
import com.orlandodiaz.ticketing.facturacion.domain.LineaDeFactura;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import com.orlandodiaz.ticketing.ventas.domain.OrdenCancelada;
import com.orlandodiaz.ticketing.ventas.domain.OrdenConfirmada;
import java.util.List;
import java.util.stream.IntStream;

/**
 * ADAPTADOR de entrada: escucha los eventos de ventas y los traduce a casos de uso de facturacion.
 * Es el UNICO lugar de facturacion que conoce a ventas. Ventas no sabe que facturacion existe.
 */
public class EscuchaDeEventosDeVentas {

    private final EmitirFactura emitirFactura;
    private final AnularFacturaDeOrden anularFactura;

    public EscuchaDeEventosDeVentas(EmitirFactura emitirFactura, AnularFacturaDeOrden anularFactura) {
        this.emitirFactura = emitirFactura;
        this.anularFactura = anularFactura;
    }

    public void registrarEn(BusDeEventosEnMemoria bus) {
        bus.suscribir(OrdenConfirmada.class, this::alConfirmarseUnaOrden);
        bus.suscribir(OrdenCancelada.class, this::alCancelarseUnaOrden);
    }

    private void alConfirmarseUnaOrden(OrdenConfirmada evento) {
        List<LineaDeFactura> lineas = IntStream.range(0, evento.lineas().size())
                .mapToObj(i -> new LineaDeFactura(
                        "Entrada " + (i + 1) + " (visitante de " + evento.lineas().get(i).edadVisitante() + " anios)",
                        evento.lineas().get(i).precio()))
                .toList();
        emitirFactura.ejecutar(new EmitirFacturaComando(
                evento.ordenId().valor(), evento.parqueId(), lineas, evento.ocurridoEn()));
    }

    private void alCancelarseUnaOrden(OrdenCancelada evento) {
        anularFactura.ejecutar(evento.ordenId().valor());
    }
}
