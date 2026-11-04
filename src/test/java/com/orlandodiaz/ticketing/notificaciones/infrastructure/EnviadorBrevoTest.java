package com.orlandodiaz.ticketing.notificaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Prueba el adaptador contra un servidor HTTP falso del JDK (no se contacta a Brevo de verdad). */
class EnviadorBrevoTest {

    private HttpServer servidor;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> claveRecibida = new AtomicReference<>();

    private String levantar(int estado) throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v3/smtp/email", intercambio -> {
            cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            claveRecibida.set(intercambio.getRequestHeaders().getFirst("api-key"));
            byte[] respuesta = (estado == 201 ? "{\"messageId\":\"1\"}" : "{\"message\":\"no\"}").getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estado, respuesta.length);
            intercambio.getResponseBody().write(respuesta);
            intercambio.close();
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v3/smtp/email";
    }

    @AfterEach
    void apagar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void mandaElCorreoConLaClaveYLosDatosEnJson() throws Exception {
        var enviador = new EnviadorBrevo(levantar(201), "clave-secreta", "boleteria@correo.com", "Boleteria");

        enviador.enviar("ana@correo.com", "Tu compra", "Linea 1\nCon \"comillas\"");

        assertThat(claveRecibida.get()).isEqualTo("clave-secreta");
        assertThat(cuerpoRecibido.get()).contains(
                "\"sender\":{\"name\":\"Boleteria\",\"email\":\"boleteria@correo.com\"}",
                "\"to\":[{\"email\":\"ana@correo.com\"}]",
                "\"subject\":\"Tu compra\"",
                "\"textContent\":\"Linea 1\\nCon \\\"comillas\\\"\"");
    }

    @Test
    void unaRespuestaDeErrorLanzaExcepcionParaQueSeRegistre() throws Exception {
        var enviador = new EnviadorBrevo(levantar(401), "mala", "b@correo.com", "B");

        assertThatThrownBy(() -> enviador.enviar("ana@correo.com", "x", "y"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("401");
    }
}
