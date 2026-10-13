package com.orlandodiaz.ticketing.facturacion.infrastructure.web;

import com.orlandodiaz.ticketing.facturacion.application.ConsultarFacturaDeOrden;
import com.orlandodiaz.ticketing.facturacion.domain.Factura;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FacturasController {

    public record LineaFacturaResponse(String descripcion, long valor) {
    }

    public record FacturaResponse(String numero, String ordenId, String parqueId, String estado,
                                  long total, LocalDateTime emitidaEn, List<LineaFacturaResponse> lineas) {
    }

    private final ConsultarFacturaDeOrden consultarFactura;

    public FacturasController(ConsultarFacturaDeOrden consultarFactura) {
        this.consultarFactura = consultarFactura;
    }

    @GetMapping("/api/ordenes/{ordenId}/factura")
    public FacturaResponse factura(@PathVariable("ordenId") String ordenId) {
        Factura f = consultarFactura.ejecutar(ordenId);
        return new FacturaResponse(f.numero(), f.ordenRef(), f.parqueId().valor(), f.estado().name(),
                f.total().pesos(), f.emitidaEn(),
                f.lineas().stream().map(l -> new LineaFacturaResponse(l.descripcion(), l.valor().pesos())).toList());
    }
}
