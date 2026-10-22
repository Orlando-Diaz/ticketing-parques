package com.orlandodiaz.ticketing.seguridad.infrastructure.web;

import com.orlandodiaz.ticketing.seguridad.application.EmisorDeTokens.TokenEmitido;
import com.orlandodiaz.ticketing.seguridad.application.IniciarSesion;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    public record LoginRequest(String usuario, String clave) {
    }

    public record TokenResponse(String token, String tipo, long expiraEnSegundos) {
    }

    private final IniciarSesion iniciarSesion;

    public AuthController(IniciarSesion iniciarSesion) {
        this.iniciarSesion = iniciarSesion;
    }

    @PostMapping("/api/auth/login")
    public TokenResponse login(@RequestBody LoginRequest peticion) {
        TokenEmitido token = iniciarSesion.ejecutar(peticion.usuario(), peticion.clave());
        return new TokenResponse(token.valor(), "Bearer", token.expiraEnSegundos());
    }
}
