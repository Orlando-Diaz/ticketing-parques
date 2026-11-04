package com.orlandodiaz.ticketing.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Cuentas de clientes: registro, compra con cuenta y el perfil con "mis compras". */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CuentasApiTest {

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

    private static String extraer(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\":\"([^\"]+)\"").matcher(json);
        assertThat(m.find()).as("campo " + campo + " en " + json).isTrue();
        return m.group(1);
    }

    private String correoNuevo() {
        return "cliente-" + UUID.randomUUID().toString().substring(0, 8) + "@correo.com";
    }

    /** Registra un cliente y devuelve su token. */
    private String registrar(String correo) throws Exception {
        var r = llamar("POST", "/api/auth/registro",
                "{\"nombre\":\"Ana Perez\",\"correo\":\"" + correo + "\",\"clave\":\"clave-de-cliente-1\"}", null);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return extraer(r.body(), "token");
    }

    private String parqueConPrecios() throws Exception {
        String admin = TokenDePruebas.de("admin", "clave-de-pruebas-123", puerto);
        String id = "cta-" + UUID.randomUUID().toString().substring(0, 8);
        assertThat(llamar("POST", "/api/parques",
                "{\"id\":\"" + id + "\",\"nombre\":\"Parque " + id + "\",\"cupoDiario\":50}", admin).statusCode()).isEqualTo(201);
        assertThat(llamar("PUT", "/api/parques/" + id + "/precios",
                "{\"esquema\":{\"tipo\":\"UNICA\",\"precio\":40000},\"reglas\":[]}", admin).statusCode()).isEqualTo(204);
        return id;
    }

    private String cuerpoDeCompra(String parque) {
        return "{\"parqueId\":\"" + parque + "\",\"fechaVisita\":\"" + LocalDate.now().plusDays(3)
                + "\",\"visitantes\":[{\"edad\":30},{\"edad\":8}]}";
    }

    @Test
    void elRegistroCreaLaCuentaYDejaLaSesionIniciada() throws Exception {
        String correo = correoNuevo();
        String token = registrar(correo);

        // el token sirve: el perfil responde (vacio) en vez de 401
        var mias = llamar("GET", "/api/ordenes/mias", null, token);
        assertThat(mias.statusCode()).isEqualTo(200);
        assertThat(mias.body()).isEqualTo("[]");

        // y tambien puede iniciar sesion despues, con el correo en cualquier mayuscula/minuscula
        var login = llamar("POST", "/api/auth/login",
                "{\"usuario\":\"" + correo.toUpperCase() + "\",\"clave\":\"clave-de-cliente-1\"}", null);
        assertThat(login.statusCode()).isEqualTo(200);
    }

    @Test
    void elRegistroValidaLosDatos() throws Exception {
        String correo = correoNuevo();
        registrar(correo);

        assertThat(llamar("POST", "/api/auth/registro",
                "{\"nombre\":\"Otra\",\"correo\":\"" + correo + "\",\"clave\":\"clave-de-cliente-1\"}", null).statusCode())
                .as("correo repetido").isEqualTo(409);
        assertThat(llamar("POST", "/api/auth/registro",
                "{\"nombre\":\"Ana\",\"correo\":\"no-es-un-correo\",\"clave\":\"clave-de-cliente-1\"}", null).statusCode())
                .as("correo invalido").isEqualTo(400);
        assertThat(llamar("POST", "/api/auth/registro",
                "{\"nombre\":\"Ana\",\"correo\":\"" + correoNuevo() + "\",\"clave\":\"corta\"}", null).statusCode())
                .as("clave corta").isEqualTo(400);
        assertThat(llamar("POST", "/api/auth/registro",
                "{\"nombre\":\"\",\"correo\":\"" + correoNuevo() + "\",\"clave\":\"clave-de-cliente-1\"}", null).statusCode())
                .as("sin nombre").isEqualTo(400);
    }

    @Test
    void sinCuentaNoSePuedeComprarNiVerElPerfil() throws Exception {
        String parque = parqueConPrecios();

        assertThat(llamar("POST", "/api/ordenes", cuerpoDeCompra(parque), null).statusCode()).isEqualTo(401);
        assertThat(llamar("GET", "/api/ordenes/mias", null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void elPersonalDeLaPuertaNoCompra() throws Exception {
        String admin = TokenDePruebas.de("admin", "clave-de-pruebas-123", puerto);
        String nombre = "puerta-" + UUID.randomUUID().toString().substring(0, 8);
        assertThat(llamar("POST", "/api/usuarios",
                "{\"nombre\":\"" + nombre + "\",\"clave\":\"clave-de-puerta-1\",\"rol\":\"PUERTA\"}", admin).statusCode()).isEqualTo(201);
        String puerta = TokenDePruebas.de(nombre, "clave-de-puerta-1", puerto);

        assertThat(llamar("POST", "/api/ordenes", cuerpoDeCompra("x"), puerta).statusCode()).isEqualTo(403);
    }

    @Test
    void unClienteNoPuedeUsarLasRutasDelAdministrador() throws Exception {
        String token = registrar(correoNuevo());

        assertThat(llamar("POST", "/api/parques", "{\"id\":\"p\",\"nombre\":\"P\",\"cupoDiario\":5}", token).statusCode()).isEqualTo(403);
        assertThat(llamar("POST", "/api/usuarios",
                "{\"nombre\":\"intruso\",\"clave\":\"clave-larga-123\",\"rol\":\"ADMIN\"}", token).statusCode()).isEqualTo(403);
        assertThat(llamar("GET", "/api/accesos/pases/x", null, token).statusCode()).isEqualTo(403);
    }

    @Test
    void laCompraQuedaEnElPerfilDeQuienLaHizoYSoloEnEse() throws Exception {
        String parque = parqueConPrecios();
        String ana = registrar(correoNuevo());
        String luis = registrar(correoNuevo());

        var compra = llamar("POST", "/api/ordenes", cuerpoDeCompra(parque), ana);
        assertThat(compra.statusCode()).as(compra.body()).isEqualTo(201);
        String ordenId = extraer(compra.body(), "id");

        var deAna = llamar("GET", "/api/ordenes/mias", null, ana);
        assertThat(deAna.statusCode()).isEqualTo(200);
        assertThat(deAna.body()).contains(ordenId).contains("\"creadaEn\"");

        assertThat(llamar("GET", "/api/ordenes/mias", null, luis).body()).isEqualTo("[]");
    }
}
