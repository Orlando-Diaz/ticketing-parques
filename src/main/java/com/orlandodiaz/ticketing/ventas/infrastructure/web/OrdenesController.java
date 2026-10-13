package com.orlandodiaz.ticketing.ventas.infrastructure.web;

import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import com.orlandodiaz.ticketing.ventas.application.CancelarOrden;
import com.orlandodiaz.ticketing.ventas.application.ComprarEntradas;
import com.orlandodiaz.ticketing.ventas.application.ComprarEntradasComando;
import com.orlandodiaz.ticketing.ventas.application.ConsultarOrden;
import com.orlandodiaz.ticketing.ventas.domain.Orden;
import com.orlandodiaz.ticketing.ventas.domain.OrdenId;
import com.orlandodiaz.ticketing.ventas.domain.Visitante;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenesController {

    public record VisitanteRequest(Integer edad, Boolean residente) {
    }

    public record CompraRequest(String parqueId, LocalDate fechaVisita, List<VisitanteRequest> visitantes) {
    }

    public record LineaResponse(int edadVisitante, boolean residente, long precio) {
    }

    public record OrdenResponse(String id, String parqueId, LocalDate fechaVisita, String estado,
                                long total, List<LineaResponse> lineas) {

        static OrdenResponse desde(Orden orden) {
            return new OrdenResponse(orden.id().valor(), orden.parqueId().valor(), orden.fechaVisita(),
                    orden.estado().name(), orden.total().pesos(),
                    orden.lineas().stream()
                            .map(l -> new LineaResponse(l.edadVisitante(), l.residente(), l.precio().pesos()))
                            .toList());
        }
    }

    private final ComprarEntradas comprarEntradas;
    private final CancelarOrden cancelarOrden;
    private final ConsultarOrden consultarOrden;

    public OrdenesController(ComprarEntradas comprarEntradas, CancelarOrden cancelarOrden, ConsultarOrden consultarOrden) {
        this.comprarEntradas = comprarEntradas;
        this.cancelarOrden = cancelarOrden;
        this.consultarOrden = consultarOrden;
    }

    @PostMapping
    public ResponseEntity<OrdenResponse> comprar(@RequestBody CompraRequest request) {
        if (request.parqueId() == null || request.fechaVisita() == null || request.visitantes() == null) {
            throw new IllegalArgumentException("Se necesitan parqueId, fechaVisita y visitantes");
        }
        List<Visitante> visitantes = request.visitantes().stream().map(v -> {
            if (v.edad() == null) {
                throw new IllegalArgumentException("Cada visitante necesita su edad");
            }
            return new Visitante(v.edad(), Boolean.TRUE.equals(v.residente()));
        }).toList();

        Orden orden = comprarEntradas.ejecutar(new ComprarEntradasComando(
                new ParqueId(request.parqueId()), request.fechaVisita(), visitantes));
        return ResponseEntity.created(URI.create("/api/ordenes/" + orden.id().valor()))
                .body(OrdenResponse.desde(orden));
    }

    @GetMapping("/{id}")
    public OrdenResponse consultar(@PathVariable("id") String id) {
        return OrdenResponse.desde(consultarOrden.ejecutar(new OrdenId(id)));
    }

    @PostMapping("/{id}/cancelacion")
    public OrdenResponse cancelar(@PathVariable("id") String id) {
        return OrdenResponse.desde(cancelarOrden.ejecutar(new OrdenId(id)));
    }
}
