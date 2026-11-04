package com.orlandodiaz.ticketing.notificaciones.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NotificarCompraConfirmadaTest {

    private record Correo(String para, String asunto, String texto) {
    }

    private final List<Correo> enviados = new ArrayList<>();
    private final Set<String> registrados = new HashSet<>();
    private final NotificarCompraConfirmada notificar = new NotificarCompraConfirmada(
            (para, asunto, texto) -> enviados.add(new Correo(para, asunto, texto)),
            parqueId -> "Parque Demo",
            new RegistroDeNotificaciones() {
                @Override
                public boolean yaEnviada(String clave) {
                    return registrados.contains(clave);
                }

                @Override
                public void marcarEnviada(String clave) {
                    registrados.add(clave);
                }
            },
            "https://mi-sitio.example/");

    private CompraConfirmada compra(String cliente) {
        return new CompraConfirmada("orden-1", cliente, "demo", LocalDate.of(2026, 12, 20), List.of(30, 8), 80000);
    }

    @Test
    void mandaUnCorreoConLosDatosYElEnlaceDeLaOrden() {
        notificar.ejecutar(compra("ana@correo.com"));

        assertThat(enviados).hasSize(1);
        Correo c = enviados.get(0);
        assertThat(c.para()).isEqualTo("ana@correo.com");
        assertThat(c.texto()).contains("Parque Demo", "20/12/2026", "Entradas: 2", "80.000",
                "https://mi-sitio.example/orden/orden-1");
    }

    @Test
    void noRepiteElCorreoSiElEventoLlegaDosVeces() {
        notificar.ejecutar(compra("ana@correo.com"));
        notificar.ejecutar(compra("ana@correo.com"));

        assertThat(enviados).hasSize(1);
    }

    @Test
    void sinCorreoDeClienteNoHaceNada() {
        notificar.ejecutar(compra(null));
        notificar.ejecutar(compra("admin"));

        assertThat(enviados).isEmpty();
        assertThat(registrados).isEmpty();
    }

    @Test
    void siElEnvioFallaNoQuedaMarcadoYSePuedeReintentar() {
        var fallaUnaVez = new int[]{0};
        var notificarConFallo = new NotificarCompraConfirmada(
                (para, asunto, texto) -> {
                    if (fallaUnaVez[0]++ == 0) {
                        throw new IllegalStateException("SMTP caido");
                    }
                    enviados.add(new Correo(para, asunto, texto));
                },
                parqueId -> "Parque Demo",
                new RegistroDeNotificaciones() {
                    @Override
                    public boolean yaEnviada(String clave) {
                        return registrados.contains(clave);
                    }

                    @Override
                    public void marcarEnviada(String clave) {
                        registrados.add(clave);
                    }
                },
                "https://mi-sitio.example");

        try {
            notificarConFallo.ejecutar(compra("ana@correo.com"));
        } catch (IllegalStateException esperado) {
            // el listener lo captura en produccion
        }
        assertThat(registrados).isEmpty();

        notificarConFallo.ejecutar(compra("ana@correo.com"));
        assertThat(enviados).hasSize(1);
    }
}
