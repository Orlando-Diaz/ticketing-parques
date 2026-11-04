package com.orlandodiaz.ticketing.notificaciones.application;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

/**
 * CASO DE USO: avisar por correo al cliente que su compra quedo confirmada.
 * - Solo si el cliente tiene un correo (las compras de personal o antiguas no lo tienen: no se hace nada).
 * - Es idempotente: los eventos se entregan "al menos una vez"; si el aviso ya salio, no se repite.
 * - El enlace lleva a la orden, donde estan el codigo QR y la factura.
 */
public class NotificarCompraConfirmada {

    private final EnviadorDeCorreo correo;
    private final NombresDeParques parques;
    private final RegistroDeNotificaciones registro;
    private final String urlDelFrontend;

    public NotificarCompraConfirmada(EnviadorDeCorreo correo, NombresDeParques parques,
                                     RegistroDeNotificaciones registro, String urlDelFrontend) {
        this.correo = correo;
        this.parques = parques;
        this.registro = registro;
        this.urlDelFrontend = urlDelFrontend == null ? "" : urlDelFrontend.replaceAll("/+$", "");
    }

    public void ejecutar(CompraConfirmada compra) {
        if (compra.cliente() == null || !compra.cliente().contains("@")) {
            return;
        }
        String clave = "compra:" + compra.ordenId();
        if (registro.yaEnviada(clave)) {
            return;
        }
        correo.enviar(compra.cliente(), "Tu compra quedo confirmada", redactar(compra));
        registro.marcarEnviada(clave);
    }

    private String redactar(CompraConfirmada c) {
        NumberFormat pesos = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO"));
        int n = c.edades().size();
        return """
                Hola,

                Tu compra quedo confirmada.

                Parque: %s
                Fecha de visita: %s
                Entradas: %d
                Total: $ %s COP

                Aqui puedes ver tus entradas con su codigo QR y tu factura:
                %s/orden/%s

                Tambien las encuentras siempre en "Mi perfil".
                Presenta el codigo QR en la entrada del parque.
                """.formatted(parques.nombreDe(c.parqueId()), fecha(c.fechaVisita()), n, pesos.format(c.total()),
                urlDelFrontend, c.ordenId());
    }

    private static String fecha(LocalDate f) {
        return f.getDayOfMonth() + "/" + f.getMonthValue() + "/" + f.getYear();
    }
}
