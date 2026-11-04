package com.orlandodiaz.ticketing.precios.infrastructure.web;

import com.orlandodiaz.ticketing.precios.application.CalcularPrecioDeEntrada;
import com.orlandodiaz.ticketing.precios.application.ConfigurarPreciosDelParque;
import com.orlandodiaz.ticketing.precios.application.ConsultarPreciosDelParque;
import com.orlandodiaz.ticketing.precios.domain.ConfiguracionDePrecios;
import com.orlandodiaz.ticketing.precios.domain.DatosDeVisita;
import com.orlandodiaz.ticketing.precios.domain.DesglosePrecio;
import com.orlandodiaz.ticketing.precios.domain.EsquemaDeTarifa;
import com.orlandodiaz.ticketing.precios.domain.TarifaPorCategoria;
import com.orlandodiaz.ticketing.precios.domain.TarifaUnica;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parques/{parqueId}/precios")
public class PreciosController {

    public record AjusteResponse(String tipo, String motivo, long monto) {
    }

    public record CotizacionResponse(long precioBase, List<AjusteResponse> ajustes, long total) {
    }

    /** edadMaxima nula significa "sin limite". */
    public record CategoriaResponse(String nombre, int edadMinima, Integer edadMaxima, long precio) {
    }

    /** tipo "UNICA" usa precio; tipo "POR_CATEGORIA" usa categorias. Es la misma forma que recibe el PUT. */
    public record EsquemaResponse(String tipo, Long precio, List<CategoriaResponse> categorias) {
    }

    public record ReglaResponse(String tipo, Integer porcentaje, Integer tamanoMinimo) {
    }

    public record ConfiguracionResponse(EsquemaResponse esquema, List<ReglaResponse> reglas) {
    }

    private final ConfigurarPreciosDelParque configurarPrecios;
    private final ConsultarPreciosDelParque consultarPrecios;
    private final CalcularPrecioDeEntrada calcularPrecio;

    public PreciosController(ConfigurarPreciosDelParque configurarPrecios,
                             ConsultarPreciosDelParque consultarPrecios,
                             CalcularPrecioDeEntrada calcularPrecio) {
        this.configurarPrecios = configurarPrecios;
        this.consultarPrecios = consultarPrecios;
        this.calcularPrecio = calcularPrecio;
    }

    /** Publico: la pantalla de compra necesita saber que tipos de entrada hay y cuanto cuestan. */
    @GetMapping
    public ConfiguracionResponse consultar(@PathVariable("parqueId") String parqueId) {
        ConfiguracionDePrecios configuracion = consultarPrecios.ejecutar(new ParqueId(parqueId));
        return new ConfiguracionResponse(
                aEsquema(configuracion.esquema()),
                configuracion.reglas().stream().map(r -> {
                    var d = r.describir();
                    return new ReglaResponse(d.tipo(), d.porcentaje(), d.tamanoMinimo());
                }).toList());
    }

    private static EsquemaResponse aEsquema(EsquemaDeTarifa esquema) {
        return switch (esquema) {
            case TarifaUnica unica -> new EsquemaResponse("UNICA", unica.precio().pesos(), null);
            case TarifaPorCategoria porCategoria -> new EsquemaResponse("POR_CATEGORIA", null,
                    porCategoria.tarifas().stream().map(t -> new CategoriaResponse(
                            t.categoria().nombre(),
                            t.categoria().edadMinima(),
                            t.categoria().edadMaxima() == Integer.MAX_VALUE ? null : t.categoria().edadMaxima(),
                            t.precio().pesos())).toList());
        };
    }

    @PutMapping
    public ResponseEntity<Void> configurar(@PathVariable("parqueId") String parqueId,
                                           @RequestBody ConfiguracionDePreciosRequest request) {
        configurarPrecios.ejecutar(request.aDominio(new ParqueId(parqueId)));
        return ResponseEntity.noContent().build();
    }

    /** Cuanto costaria una entrada para un visitante, con el desglose de descuentos y recargos. */
    @GetMapping("/cotizacion")
    public CotizacionResponse cotizar(
            @PathVariable("parqueId") String parqueId,
            @RequestParam("edad") int edad,
            @RequestParam(name = "residente", defaultValue = "false") boolean residente,
            @RequestParam(name = "grupo", defaultValue = "1") int grupo,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        DesglosePrecio desglose = calcularPrecio.ejecutar(new ParqueId(parqueId),
                new DatosDeVisita(edad, residente, grupo, fecha));
        return new CotizacionResponse(
                desglose.precioBase().pesos(),
                desglose.ajustesAplicados().stream()
                        .map(a -> new AjusteResponse(a.tipo().name(), a.motivo(), a.monto().pesos()))
                        .toList(),
                desglose.total().pesos());
    }
}
