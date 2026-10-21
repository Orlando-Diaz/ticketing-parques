package com.orlandodiaz.ticketing.accesos.infrastructure;

import com.orlandodiaz.ticketing.accesos.application.AnularPaseDeOrden;
import com.orlandodiaz.ticketing.accesos.application.EmitirPase;
import com.orlandodiaz.ticketing.accesos.application.EmitirPaseComando;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import com.orlandodiaz.ticketing.ventas.domain.OrdenCancelada;
import com.orlandodiaz.ticketing.ventas.domain.OrdenConfirmada;

/**
 * ADAPTADOR de entrada: escucha los eventos de ventas y los traduce a casos de uso de accesos.
 * Es el UNICO lugar de accesos que conoce a ventas. Ventas no sabe que accesos existe.
 */
public class EscuchaDeEventosDeVentas {

    private final EmitirPase emitirPase;
    private final AnularPaseDeOrden anularPase;

    public EscuchaDeEventosDeVentas(EmitirPase emitirPase, AnularPaseDeOrden anularPase) {
        this.emitirPase = emitirPase;
        this.anularPase = anularPase;
    }

    public void registrarEn(BusDeEventosEnMemoria bus) {
        bus.suscribir(OrdenConfirmada.class, this::alConfirmarseUnaOrden);
        bus.suscribir(OrdenCancelada.class, this::alCancelarseUnaOrden);
    }

    private void alConfirmarseUnaOrden(OrdenConfirmada evento) {
        emitirPase.ejecutar(new EmitirPaseComando(evento.ordenId().valor(), evento.parqueId(),
                evento.fechaVisita(), evento.lineas().size(), evento.ocurridoEn()));
    }

    private void alCancelarseUnaOrden(OrdenCancelada evento) {
        anularPase.ejecutar(evento.ordenId().valor());
    }
}
