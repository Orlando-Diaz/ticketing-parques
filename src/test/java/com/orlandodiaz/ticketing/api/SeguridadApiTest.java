package com.orlandodiaz.ticketing.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Quien puede hacer que: sin token, con token de puerta y con token de administrador. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SeguridadApiTest {

    private static final String CLAVE_ADMIN = "clave-de-pruebas-123";

    @Value("${local.server.port}")
    private int puerto;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> llamar(String metodo, String ruta, String cuerpo, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Content-Type", "application/json");
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        b.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(cuerpo));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String tokenAdmin() {
        return TokenDePruebas.de("admin", CLAVE_ADMIN, puerto);
    }

    /** Crea un usuario de puerta (lo hace el admin) y devuelve su token. */
    private String tokenDePuerta() throws Exception {
        String nombre = "puerta-" + UUID.randomUUID().toString().substring(0, 8);
        var creado = llamar("POST", "/api/usuarios",
                "{\"nombre\":\"" + nombre + "\",\"clave\":\"clave-de-puerta-1\",\"rol\":\"PUERTA\"}", tokenAdmin());
        assertThat(creado.statusCode()).as(creado.body()).isEqualTo(201);
        return TokenDePruebas.de(nombre, "clave-de-puerta-1", puerto);
    }

    @Test
    void sinTokenLasRutasDelPersonalDan401() throws Exception {
        assertThat(llamar("POST", "/api/accesos/ingresos", "{\"codigo\":\"x\",\"personas\":1}", null).statusCode()).isEqualTo(401);
        assertThat(llamar("GET", "/api/accesos/pases/x", null, null).statusCode()).isEqualTo(401);
        assertThat(llamar("GET", "/api/reportes/parques/p/dia?fecha=2026-10-20", null, null).statusCode()).isEqualTo(401);
        assertThat(llamar("POST", "/api/parques", "{\"id\":\"p\",\"nombre\":\"P\",\"cupoDiario\":5}", null).statusCode()).isEqualTo(401);
        assertThat(llamar("POST", "/api/usuarios", "{}", null).statusCode()).isEqualTo(401);
    }

    @Test
    void lasRutasPublicasDelVisitanteNoPidenToken() throws Exception {
        // 404 (no existe) y no 401: la peticion llego al negocio sin autenticarse
        assertThat(llamar("GET", "/api/parques/no-existe/disponibilidad?fecha=2026-12-20", null, null).statusCode()).isEqualTo(404);
        assertThat(llamar("GET", "/api/ordenes/no-existe", null, null).statusCode()).isEqualTo(404);
        assertThat(llamar("GET", "/api/ordenes/no-existe/pase", null, null).statusCode()).isEqualTo(404);
    }

    @Test
    void unTokenInventadoOMalFormadoDa401() throws Exception {
        assertThat(llamar("GET", "/api/accesos/pases/x", null, "esto.no.es-un-jwt").statusCode()).isEqualTo(401);
    }

    @Test
    void elLoginRechazaClavesIncorrectasSinDecirQueFallo() throws Exception {
        var claveMala = llamar("POST", "/api/auth/login", "{\"usuario\":\"admin\",\"clave\":\"incorrecta\"}", null);
        var usuarioFalso = llamar("POST", "/api/auth/login", "{\"usuario\":\"fantasma\",\"clave\":\"incorrecta\"}", null);

        assertThat(claveMala.statusCode()).isEqualTo(401);
        assertThat(usuarioFalso.statusCode()).isEqualTo(401);
        assertThat(claveMala.body()).isEqualTo(usuarioFalso.body()); // mismo mensaje en ambos casos
        assertThat(claveMala.body()).doesNotContain("token");
    }

    @Test
    void elLoginCorrectoDevuelveUnTokenBearer() throws Exception {
        var r = llamar("POST", "/api/auth/login", "{\"usuario\":\"admin\",\"clave\":\"" + CLAVE_ADMIN + "\"}", null);

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("\"token\":\"", "\"tipo\":\"Bearer\"", "\"expiraEnSegundos\":3600");
    }

    @Test
    void laPuertaPuedeValidarPeroNoAdministrar() throws Exception {
        String puerta = tokenDePuerta();

        // pasa la seguridad: el negocio responde 404 porque el pase no existe
        assertThat(llamar("GET", "/api/accesos/pases/inexistente", null, puerta).statusCode()).isEqualTo(404);
        assertThat(llamar("POST", "/api/accesos/ingresos", "{\"codigo\":\"inexistente\",\"personas\":1}", puerta).statusCode()).isEqualTo(404);

        // y NO puede lo del administrador: 403 (autenticada pero sin permiso)
        assertThat(llamar("GET", "/api/reportes/parques/p/dia?fecha=2026-10-20", null, puerta).statusCode()).isEqualTo(403);
        assertThat(llamar("POST", "/api/parques", "{\"id\":\"p\",\"nombre\":\"P\",\"cupoDiario\":5}", puerta).statusCode()).isEqualTo(403);
        assertThat(llamar("POST", "/api/usuarios",
                "{\"nombre\":\"otro-usuario\",\"clave\":\"clave-larga-123\",\"rol\":\"ADMIN\"}", puerta).statusCode()).isEqualTo(403);
    }

    @Test
    void elAdminPuedeCrearUsuariosConValidaciones() throws Exception {
        String admin = tokenAdmin();
        String nombre = "usuario-" + UUID.randomUUID().toString().substring(0, 8);
        String cuerpo = "{\"nombre\":\"" + nombre + "\",\"clave\":\"clave-larga-123\",\"rol\":\"puerta\"}";

        var creado = llamar("POST", "/api/usuarios", cuerpo, admin);
        assertThat(creado.statusCode()).isEqualTo(201);
        assertThat(creado.body()).contains("\"rol\":\"PUERTA\"").doesNotContain("clave");

        assertThat(llamar("POST", "/api/usuarios", cuerpo, admin).statusCode()).as("repetido").isEqualTo(409);
        assertThat(llamar("POST", "/api/usuarios",
                "{\"nombre\":\"" + nombre + "x\",\"clave\":\"corta\",\"rol\":\"PUERTA\"}", admin).statusCode()).as("clave corta").isEqualTo(400);
        assertThat(llamar("POST", "/api/usuarios",
                "{\"nombre\":\"" + nombre + "y\",\"clave\":\"clave-larga-123\",\"rol\":\"DIOS\"}", admin).statusCode()).as("rol raro").isEqualTo(400);
    }

    @Test
    void elListadoDeParquesEsPublicoPeroCrearUnoNo() throws Exception {
        assertThat(llamar("GET", "/api/parques", null, null).statusCode()).isEqualTo(200);
        assertThat(llamar("POST", "/api/parques", "{\"id\":\"p\",\"nombre\":\"P\",\"cupoDiario\":5}", null).statusCode()).isEqualTo(401);
    }

    @Test
    void corsAutorizaSoloAlOrigenDelFrontend() throws Exception {
        var permitido = preflight("http://localhost:4200");
        assertThat(permitido.statusCode()).isEqualTo(200);
        assertThat(permitido.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:4200");

        var ajeno = preflight("http://sitio-malicioso.example");
        assertThat(ajeno.statusCode()).isEqualTo(403);
    }

    private HttpResponse<String> preflight(String origen) throws Exception {
        var peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/ordenes"))
                .header("Origin", origen)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        return http.send(peticion, HttpResponse.BodyHandlers.ofString());
    }
}
