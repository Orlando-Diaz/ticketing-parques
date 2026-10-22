package com.orlandodiaz.ticketing.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** El recorrido completo de la puerta, por HTTP: compra -> pase (QR) -> ingresos -> cancelacion. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AccesosApiTest {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));

    @Value("${local.server.port}")
    private int puerto;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> llamar(String metodo, String ruta, String cuerpo) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + TokenDePruebas.de("admin", "clave-de-pruebas-123", puerto));
        b.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(cuerpo));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String parqueNuevo() throws Exception {
        String id = "acc-" + UUID.randomUUID().toString().substring(0, 8);
        assertThat(llamar("POST", "/api/parques",
                "{\"id\":\"" + id + "\",\"nombre\":\"Parque " + id + "\",\"cupoDiario\":50}").statusCode()).isEqualTo(201);
        assertThat(llamar("PUT", "/api/parques/" + id + "/precios",
                "{\"esquema\":{\"tipo\":\"UNICA\",\"precio\":40000},\"reglas\":[]}").statusCode()).isEqualTo(204);
        return id;
    }

    private String comprar(String parque, LocalDate fecha, int personas) throws Exception {
        String visitantes = "[" + "{\"edad\":30},".repeat(personas - 1) + "{\"edad\":30}]";
        var r = llamar("POST", "/api/ordenes",
                "{\"parqueId\":\"" + parque + "\",\"fechaVisita\":\"" + fecha + "\",\"visitantes\":" + visitantes + "}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return extraer(r.body(), "id");
    }

    private static String extraer(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\":\"([^\"]+)\"").matcher(json);
        assertThat(m.find()).as("campo " + campo + " en " + json).isTrue();
        return m.group(1);
    }

    private HttpResponse<String> entrar(String codigo, Integer personas) throws Exception {
        String cuerpo = personas == null
                ? "{\"codigo\":\"" + codigo + "\"}"
                : "{\"codigo\":\"" + codigo + "\",\"personas\":" + personas + ",\"puerta\":\"norte\"}";
        return llamar("POST", "/api/accesos/ingresos", cuerpo);
    }

    @Test
    void recorridoCompletoDeLaPuerta() throws Exception {
        String parque = parqueNuevo();
        String ordenId = comprar(parque, HOY, 3);

        // el comprador pide su pase: el codigo es lo que se dibuja como QR
        var pase = llamar("GET", "/api/ordenes/" + ordenId + "/pase", null);
        assertThat(pase.statusCode()).isEqualTo(200);
        assertThat(pase.body()).contains("\"estado\":\"ACTIVO\"", "\"entradasTotales\":3", "\"entradasRestantes\":3");
        String codigo = extraer(pase.body(), "codigo");
        assertThat(codigo).hasSizeGreaterThanOrEqualTo(20); // 128 bits: no se puede adivinar

        // entran 2 por la puerta norte
        var primera = entrar(codigo, 2);
        assertThat(primera.statusCode()).as(primera.body()).isEqualTo(200);
        assertThat(primera.body()).contains("\"entradasUsadas\":2", "\"entradasRestantes\":1", "\"puerta\":\"norte\"");

        // intentan entrar 2 mas, pero solo queda 1
        var demasiados = entrar(codigo, 2);
        assertThat(demasiados.statusCode()).isEqualTo(409);
        assertThat(demasiados.body()).contains("solo le quedan 1");

        // entra el ultimo
        assertThat(entrar(codigo, 1).body()).contains("\"entradasRestantes\":0");
        assertThat(entrar(codigo, 1).statusCode()).isEqualTo(409);

        // se puede consultar por codigo, con el historial
        var consulta = llamar("GET", "/api/accesos/pases/" + codigo, null);
        assertThat(consulta.statusCode()).isEqualTo(200);
        assertThat(consulta.body()).contains("\"entradasUsadas\":3");
    }

    @Test
    void erroresDeLaPuertaSeTraducenACodigosHttpCorrectos() throws Exception {
        // codigo inexistente -> 404; sin cantidad de personas -> 400
        assertThat(entrar("este-codigo-no-existe", 1).statusCode()).isEqualTo(404);
        assertThat(llamar("GET", "/api/accesos/pases/este-codigo-no-existe", null).statusCode()).isEqualTo(404);
        assertThat(llamar("GET", "/api/ordenes/no-existe/pase", null).statusCode()).isEqualTo(404);

        String parque = parqueNuevo();
        String hoy = extraer(llamar("GET", "/api/ordenes/" + comprar(parque, HOY, 1) + "/pase", null).body(), "codigo");
        assertThat(entrar(hoy, null).statusCode()).isEqualTo(400);

        // un pase para manana todavia no sirve hoy -> 422
        String ordenManana = comprar(parque, HOY.plusDays(1), 1);
        String codigoManana = extraer(llamar("GET", "/api/ordenes/" + ordenManana + "/pase", null).body(), "codigo");
        var temprano = entrar(codigoManana, 1);
        assertThat(temprano.statusCode()).isEqualTo(422);
        assertThat(temprano.body()).contains("es valido el " + HOY.plusDays(1));
    }

    @Test
    void cancelarLaOrdenAnulaElPase() throws Exception {
        String parque = parqueNuevo();
        String ordenId = comprar(parque, HOY, 2);
        String codigo = extraer(llamar("GET", "/api/ordenes/" + ordenId + "/pase", null).body(), "codigo");

        assertThat(llamar("POST", "/api/ordenes/" + ordenId + "/cancelacion", null).statusCode()).isEqualTo(200);

        var pase = llamar("GET", "/api/accesos/pases/" + codigo, null);
        assertThat(pase.body()).contains("\"estado\":\"ANULADO\"");
        var intento = entrar(codigo, 1);
        assertThat(intento.statusCode()).isEqualTo(409);
        assertThat(intento.body()).contains("anulado");
    }
}
