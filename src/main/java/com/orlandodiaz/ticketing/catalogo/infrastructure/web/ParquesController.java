package com.orlandodiaz.ticketing.catalogo.infrastructure.web;

import com.orlandodiaz.ticketing.catalogo.application.ConsultarCupoDisponible;
import com.orlandodiaz.ticketing.catalogo.application.ListarParques;
import com.orlandodiaz.ticketing.catalogo.application.RegistrarParque;
import com.orlandodiaz.ticketing.catalogo.domain.Parque;
import com.orlandodiaz.ticketing.shared.domain.ParqueId;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADAPTADOR de entrada (HTTP). Solo traduce: recibe JSON, llama al caso de uso y devuelve JSON.
 * No tiene reglas de negocio.
 */
@RestController
@RequestMapping("/api/parques")
public class ParquesController {

    public record RegistrarParqueRequest(String id, String nombre, int cupoDiario) {
    }

    public record ParqueResponse(String id, String nombre, int cupoDiario) {
    }

    public record DisponibilidadResponse(String parqueId, LocalDate fecha, int disponibles) {
    }

    private final RegistrarParque registrarParque;
    private final ConsultarCupoDisponible consultarCupo;
    private final ListarParques listarParques;

    public ParquesController(RegistrarParque registrarParque, ConsultarCupoDisponible consultarCupo,
                             ListarParques listarParques) {
        this.listarParques = listarParques;
        this.registrarParque = registrarParque;
        this.consultarCupo = consultarCupo;
    }

    @GetMapping
    public List<ParqueResponse> listar() {
        return listarParques.ejecutar().stream()
                .map(p -> new ParqueResponse(p.id().valor(), p.nombre(), p.cupoDiario()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<ParqueResponse> registrar(@RequestBody RegistrarParqueRequest request) {
        if (request.id() == null) {
            throw new IllegalArgumentException("El id del parque es obligatorio");
        }
        Parque parque = registrarParque.ejecutar(new ParqueId(request.id()), request.nombre(), request.cupoDiario());
        return ResponseEntity.created(URI.create("/api/parques/" + parque.id().valor()))
                .body(new ParqueResponse(parque.id().valor(), parque.nombre(), parque.cupoDiario()));
    }

    @GetMapping("/{id}/disponibilidad")
    public DisponibilidadResponse disponibilidad(
            @PathVariable("id") String id,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        int disponibles = consultarCupo.ejecutar(new ParqueId(id), fecha);
        return new DisponibilidadResponse(id, fecha, disponibles);
    }
}
