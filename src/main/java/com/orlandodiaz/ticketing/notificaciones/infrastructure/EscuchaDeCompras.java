package com.orlandodiaz.ticketing.notificaciones.infrastructure;

import com.orlandodiaz.ticketing.notificaciones.application.CompraConfirmada;
import com.orlandodiaz.ticketing.notificaciones.application.NotificarCompraConfirmada;
import com.orlandodiaz.ticketing.shared.infrastructure.BusDeEventosEnMemoria;
import com.orlandodiaz.ticketing.ventas.domain.OrdenConfirmada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ADAPTADOR de entrada: escucha la compra confirmada y avisa por correo.
 * Un correo que falla NO debe frenar al resto de eventos (el despachador reintenta y un mensaje que falla
 * retrasaria a los demas), asi que aqui se registra el error y se sigue. Limite conocido: si el correo falla,
 * no se reintenta (ver ADR 0010).
 */
public class EscuchaDeCompras {

    private static final Logger log = LoggerFactory.getLogger(EscuchaDeCompras.class);

    private final NotificarCompraConfirmada notificar;

    public EscuchaDeCompras(NotificarCompraConfirmada notificar) {
        this.notificar = notificar;
    }

    public void registrarEn(BusDeEventosEnMemoria bus) {
        bus.suscribir(OrdenConfirmada.class, this::alConfirmarse);
    }

    private void alConfirmarse(OrdenConfirmada evento) {
        try {
            notificar.ejecutar(new CompraConfirmada(
                    evento.ordenId().valor(), evento.cliente(), evento.parqueId().valor(), evento.fechaVisita(),
                    evento.lineas().stream().map(l -> l.edadVisitante()).toList(), evento.total().pesos()));
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el correo de la orden {}: {}", evento.ordenId().valor(), e.getMessage());
        }
    }
}
