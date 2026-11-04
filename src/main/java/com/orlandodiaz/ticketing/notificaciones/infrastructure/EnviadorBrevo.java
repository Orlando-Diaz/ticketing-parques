package com.orlandodiaz.ticketing.notificaciones.infrastructure;

import com.orlandodiaz.ticketing.notificaciones.application.EnviadorDeCorreo;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ADAPTADOR: envia el correo por la API HTTPS de Brevo. Existe porque algunos hostings gratuitos (Render) bloquean
 * el SMTP saliente pero dejan salir HTTPS. El remitente debe estar verificado en Brevo.
 * Usa solo el cliente HTTP del JDK: no agrega dependencias.
 */
public class EnviadorBrevo implements EnviadorDeCorreo {

    public static final String URL_POR_DEFECTO = "https://api.brevo.com/v3/smtp/email";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String url;
    private final String claveDeApi;
    private final String remitente;
    private final String nombreDelRemitente;

    public EnviadorBrevo(String url, String claveDeApi, String remitente, String nombreDelRemitente) {
        this.url = url;
        this.claveDeApi = claveDeApi;
        this.remitente = remitente;
        this.nombreDelRemitente = nombreDelRemitente;
    }

    @Override
    public void enviar(String para, String asunto, String texto) {
        String cuerpo = "{\"sender\":{\"name\":" + json(nombreDelRemitente) + ",\"email\":" + json(remitente) + "},"
                + "\"to\":[{\"email\":" + json(para) + "}],"
                + "\"subject\":" + json(asunto) + ",\"textContent\":" + json(texto) + "}";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("api-key", claveDeApi)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        try {
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() / 100 != 2) {
                throw new IllegalStateException("Brevo respondio " + respuesta.statusCode() + ": " + respuesta.body());
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo contactar a Brevo: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio interrumpido", e);
        }
    }

    /** Un texto como literal JSON (con comillas y caracteres especiales escapados). */
    static String json(String texto) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
