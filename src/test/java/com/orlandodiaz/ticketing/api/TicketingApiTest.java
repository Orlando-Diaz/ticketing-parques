package com.orlandodiaz.ticketing.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Levanta la aplicacion de verdad (servidor incluido, en un puerto libre) y la usa por HTTP,
 * como lo haria un cliente. Prueba el sistema completo: controladores, casos de uso, dominio, eventos
 * y la base de datos (H2 en memoria con las migraciones reales de Flyway).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TicketingApiTest {

    private static final String CONFIG_PRECIOS = """
            {
              "esquema": {
                "tipo": "POR_CATEGORIA",
                "categorias": [
                  {"nombre": "Nino",   "edadMinima": 3,  "edadMaxima": 10, "precio": 25000},
                  {"nombre": "Adulto", "edadMinima": 11, "precio": 50000}
                ]
              },
              "reglas": [ {"tipo": "DESCUENTO_RESIDENTE", "porcentaje": 20} ]
            }
            """;

    @Value("${local.server.port}")
    private int puerto;

    private final HttpClient http = HttpClient.newHttpClient();
    private final String visita = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(7).toString();

    private HttpResponse<String> llamar(String metodo, String ruta, String cuerpo) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + TokenDePruebas.de("admin", "clave-de-pruebas-123", puerto));
        b.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(cuerpo));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void crearParque(String id, int cupo) throws Exception {
        var r = llamar("POST", "/api/parques", "{\"id\":\"" + id + "\",\"nombre\":\"Parque " + id + "\",\"cupoDiario\":" + cupo + "}");
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(llamar("PUT", "/api/parques/" + id + "/precios", CONFIG_PRECIOS).statusCode()).isEqualTo(204);
    }

    private String comprar(String parque, String visitantesJson) throws Exception {
        var r = llamar("POST", "/api/ordenes",
                "{\"parqueId\":\"" + parque + "\",\"fechaVisita\":\"" + visita + "\",\"visitantes\":" + visitantesJson + "}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        Matcher m = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(r.body());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    @Test
    void flujoCompletoDeCompraCancelacionYFactura() throws Exception {
        crearParque("demo", 3);

        // cotizar
        var cotizacion = llamar("GET", "/api/parques/demo/precios/cotizacion?edad=30&residente=true&fecha=" + visita, null);
        assertThat(cotizacion.statusCode()).isEqualTo(200);
        assertThat(cotizacion.body()).contains("\"total\":40000"); // 50.000 - 20 % residente

        assertThat(llamar("GET", "/api/parques/demo/disponibilidad?fecha=" + visita, null).body())
                .contains("\"disponibles\":3");

        // comprar 2 entradas (nino + adulto)
        String ordenId = comprar("demo", "[{\"edad\":8},{\"edad\":30}]");
        var orden = llamar("GET", "/api/ordenes/" + ordenId, null);
        assertThat(orden.body()).contains("\"total\":75000").contains("\"estado\":\"CONFIRMADA\"");
        assertThat(llamar("GET", "/api/parques/demo/disponibilidad?fecha=" + visita, null).body())
                .contains("\"disponibles\":1");

        // no hay cupo para 2 mas -> 409
        var sinCupo = llamar("POST", "/api/ordenes", "{\"parqueId\":\"demo\",\"fechaVisita\":\"" + visita
                + "\",\"visitantes\":[{\"edad\":20},{\"edad\":21}]}");
        assertThat(sinCupo.statusCode()).isEqualTo(409);

        // la factura se genero sola por el evento
        var factura = llamar("GET", "/api/ordenes/" + ordenId + "/factura", null);
        assertThat(factura.statusCode()).isEqualTo(200);
        assertThat(factura.body()).contains("FAC-").contains("\"estado\":\"EMITIDA\"").contains("\"total\":75000");

        // cancelar: devuelve cupo y anula la factura
        var cancelada = llamar("POST", "/api/ordenes/" + ordenId + "/cancelacion", null);
        assertThat(cancelada.statusCode()).isEqualTo(200);
        assertThat(cancelada.body()).contains("\"estado\":\"CANCELADA\"");
        assertThat(llamar("GET", "/api/parques/demo/disponibilidad?fecha=" + visita, null).body())
                .contains("\"disponibles\":3");
        assertThat(llamar("GET", "/api/ordenes/" + ordenId + "/factura", null).body()).contains("\"estado\":\"ANULADA\"");

        // cancelar de nuevo -> 409
        assertThat(llamar("POST", "/api/ordenes/" + ordenId + "/cancelacion", null).statusCode()).isEqualTo(409);
    }

    @Test
    void erroresSeTraducenACodigosHttpCorrectos() throws Exception {
        assertThat(llamar("GET", "/api/parques/no-existe/disponibilidad?fecha=" + visita, null).statusCode()).isEqualTo(404);
        assertThat(llamar("GET", "/api/ordenes/no-existe", null).statusCode()).isEqualTo(404);

        crearParque("errores", 5);
        // parque repetido -> 409
        assertThat(llamar("POST", "/api/parques", "{\"id\":\"errores\",\"nombre\":\"Otro\",\"cupoDiario\":5}").statusCode())
                .isEqualTo(409);
        // fecha pasada -> 422
        var pasada = llamar("POST", "/api/ordenes",
                "{\"parqueId\":\"errores\",\"fechaVisita\":\"2020-01-01\",\"visitantes\":[{\"edad\":30}]}");
        assertThat(pasada.statusCode()).isEqualTo(422);
        // edad sin tarifa (el esquema empieza en 3 anios) -> 422
        var sinTarifa = llamar("POST", "/api/ordenes",
                "{\"parqueId\":\"errores\",\"fechaVisita\":\"" + visita + "\",\"visitantes\":[{\"edad\":1}]}");
        assertThat(sinTarifa.statusCode()).isEqualTo(422);
        // visitante sin edad -> 400
        var sinEdad = llamar("POST", "/api/ordenes",
                "{\"parqueId\":\"errores\",\"fechaVisita\":\"" + visita + "\",\"visitantes\":[{}]}");
        assertThat(sinEdad.statusCode()).isEqualTo(400);
        // tipo de esquema desconocido -> 400
        var malConfig = llamar("PUT", "/api/parques/errores/precios", "{\"esquema\":{\"tipo\":\"RARA\"}}");
        assertThat(malConfig.statusCode()).isEqualTo(400);
    }
}
