package com.orlandodiaz.ticketing.precios.infrastructure.web;

import com.orlandodiaz.ticketing.precios.application.CalcularPrecioDeEntrada;
import com.orlandodiaz.ticketing.precios.application.ConfigurarPreciosDelParque;
import com.orlandodiaz.ticketing.precios.domain.DatosDeVisita;
import com.orlandodiaz.ticketing.precios.domain.DesglosePrecio;
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

    private final ConfigurarPreciosDelParque configurarPrecios;
    private final CalcularPrecioDeEntrada calcularPrecio;

    public PreciosController(ConfigurarPreciosDelParque configurarPrecios, CalcularPrecioDeEntrada calcularPrecio) {
        this.configurarPrecios = configurarPrecios;
        this.calcularPrecio = calcularPrecio;
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
