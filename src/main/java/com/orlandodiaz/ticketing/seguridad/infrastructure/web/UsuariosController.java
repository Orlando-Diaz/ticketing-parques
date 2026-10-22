package com.orlandodiaz.ticketing.seguridad.infrastructure.web;

import com.orlandodiaz.ticketing.seguridad.application.CrearUsuario;
import com.orlandodiaz.ticketing.seguridad.domain.Rol;
import com.orlandodiaz.ticketing.seguridad.domain.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Solo ADMIN (lo exige la cadena de seguridad). La respuesta nunca incluye la clave, ni siquiera cifrada. */
@RestController
public class UsuariosController {

    public record UsuarioRequest(String nombre, String clave, String rol) {
    }

    public record UsuarioResponse(String nombre, String rol) {
    }

    private final CrearUsuario crearUsuario;

    public UsuariosController(CrearUsuario crearUsuario) {
        this.crearUsuario = crearUsuario;
    }

    @PostMapping("/api/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@RequestBody UsuarioRequest peticion) {
        if (peticion.rol() == null) {
            throw new IllegalArgumentException("El rol es obligatorio (ADMIN o PUERTA)");
        }
        Rol rol;
        try {
            rol = Rol.valueOf(peticion.rol().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol desconocido: " + peticion.rol() + " (usa ADMIN o PUERTA)");
        }
        Usuario creado = crearUsuario.ejecutar(peticion.nombre(), peticion.clave(), rol);
        return new UsuarioResponse(creado.nombre(), creado.rol().name());
    }
}
