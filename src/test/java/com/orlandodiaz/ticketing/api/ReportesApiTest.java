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

/** Los reportes por HTTP: compra, cancelacion e ingresos alimentan el resumen sin que nadie lo pida. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReportesApiTest {

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
        String id = "rep-" + UUID.randomUUID().toString().substring(0, 8);
        assertThat(llamar("POST", "/api/parques",
                "{\"id\":\"" + id + "\",\"nombre\":\"Parque " + id + "\",\"cupoDiario\":50}").statusCode()).isEqualTo(201);
        assertThat(llamar("PUT", "/api/parques/" + id + "/precios",
                "{\"esquema\":{\"tipo\":\"UNICA\",\"precio\":40000},\"reglas\":[]}").statusCode()).isEqualTo(204);
        return id;
    }

    private String comprar(String parque, int personas) throws Exception {
        String visitantes = "[" + "{\"edad\":30},".repeat(personas - 1) + "{\"edad\":30}]";
        var r = llamar("POST", "/api/ordenes",
                "{\"parqueId\":\"" + parque + "\",\"fechaVisita\":\"" + HOY + "\",\"visitantes\":" + visitantes + "}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return extraer(r.body(), "id");
    }

    private static String extraer(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\":\"([^\"]+)\"").matcher(json);
        assertThat(m.find()).as("campo " + campo + " en " + json).isTrue();
        return m.group(1);
    }

    private String resumenDelDia(String parque) throws Exception {
        var r = llamar("GET", "/api/reportes/parques/" + parque + "/dia?fecha=" + HOY, null);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        return r.body();
    }

    @Test
    void elResumenSigueLaVidaDeLasCompras() throws Exception {
        String parque = parqueNuevo();
        assertThat(resumenDelDia(parque)).contains("\"entradasVendidas\":0", "\"ventas\":0");

        String orden1 = comprar(parque, 3);   // 120.000
        String orden2 = comprar(parque, 2);   //  80.000
        assertThat(resumenDelDia(parque)).contains("\"ordenesVigentes\":2", "\"entradasVendidas\":5", "\"ventas\":200000");

        // se cancela la segunda: deja de contar como venta y se informa aparte
        assertThat(llamar("POST", "/api/ordenes/" + orden2 + "/cancelacion", null).statusCode()).isEqualTo(200);
        assertThat(resumenDelDia(parque)).contains("\"entradasVendidas\":3", "\"ventas\":120000",
                "\"ordenesCanceladas\":1", "\"entradasCanceladas\":2");

        // entran 2 de los 3 de la primera orden por la puerta norte
        String codigo = extraer(llamar("GET", "/api/ordenes/" + orden1 + "/pase", null).body(), "codigo");
        var ingreso = llamar("POST", "/api/accesos/ingresos",
                "{\"codigo\":\"" + codigo + "\",\"personas\":2,\"puerta\":\"norte\"}");
        assertThat(ingreso.statusCode()).as(ingreso.body()).isEqualTo(200);
        assertThat(resumenDelDia(parque)).contains("\"ingresaron\":2", "\"sinIngresar\":1", "\"norte\":2");

        // el rango incluye el dia con actividad
        var rango = llamar("GET", "/api/reportes/parques/" + parque + "/rango?desde=" + HOY.minusDays(1)
                + "&hasta=" + HOY.plusDays(1), null);
        assertThat(rango.statusCode()).isEqualTo(200);
        assertThat(rango.body()).startsWith("[").contains("\"fecha\":\"" + HOY + "\"", "\"entradasVendidas\":3");
    }

    @Test
    void losErroresDelRangoSonPeticionesInvalidas() throws Exception {
        String parque = parqueNuevo();

        var alReves = llamar("GET", "/api/reportes/parques/" + parque + "/rango?desde=" + HOY + "&hasta="
                + HOY.minusDays(1), null);
        assertThat(alReves.statusCode()).isEqualTo(400);

        var demasiado = llamar("GET", "/api/reportes/parques/" + parque + "/rango?desde=" + HOY.minusDays(400)
                + "&hasta=" + HOY, null);
        assertThat(demasiado.statusCode()).isEqualTo(400);

        assertThat(llamar("GET", "/api/reportes/parques/" + parque + "/dia", null).statusCode()).isEqualTo(400);
    }
}
