package com.orlandodiaz.ticketing.seguridad.infrastructure.web;

import com.orlandodiaz.ticketing.seguridad.application.EmisorDeTokens.TokenEmitido;
import com.orlandodiaz.ticketing.seguridad.application.IniciarSesion;
import com.orlandodiaz.ticketing.seguridad.application.RegistrarCliente;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    public record LoginRequest(String usuario, String clave) {
    }

    public record RegistroRequest(String nombre, String correo, String clave) {
    }

    public record TokenResponse(String token, String tipo, long expiraEnSegundos) {
    }

    private final IniciarSesion iniciarSesion;
    private final RegistrarCliente registrarCliente;

    public AuthController(IniciarSesion iniciarSesion, RegistrarCliente registrarCliente) {
        this.iniciarSesion = iniciarSesion;
        this.registrarCliente = registrarCliente;
    }

    @PostMapping("/api/auth/login")
    public TokenResponse login(@RequestBody LoginRequest peticion) {
        TokenEmitido token = iniciarSesion.ejecutar(peticion.usuario(), peticion.clave());
        return new TokenResponse(token.valor(), "Bearer", token.expiraEnSegundos());
    }

    /** Crea la cuenta de un cliente y lo deja con la sesion iniciada (devuelve el mismo token que el login). */
    @PostMapping("/api/auth/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registro(@RequestBody RegistroRequest peticion) {
        registrarCliente.ejecutar(peticion.nombre(), peticion.correo(), peticion.clave());
        TokenEmitido token = iniciarSesion.ejecutar(peticion.correo(), peticion.clave());
        return new TokenResponse(token.valor(), "Bearer", token.expiraEnSegundos());
    }
}
