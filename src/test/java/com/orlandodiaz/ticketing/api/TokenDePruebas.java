package com.orlandodiaz.ticketing.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ayuda para los tests de API: inicia sesion y devuelve el token. */
final class TokenDePruebas {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private TokenDePruebas() {
    }

    static String de(String usuario, String clave, int puerto) {
        try {
            var peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"usuario\":\"" + usuario + "\",\"clave\":\"" + clave + "\"}"))
                    .build();
            HttpResponse<String> r = HTTP.send(peticion, HttpResponse.BodyHandlers.ofString());
            assertThat(r.statusCode()).as("login de " + usuario + ": " + r.body()).isEqualTo(200);
            Matcher m = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(r.body());
            assertThat(m.find()).as(r.body()).isTrue();
            return m.group(1);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
