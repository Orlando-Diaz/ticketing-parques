package com.orlandodiaz.ticketing.seguridad.application;

import com.orlandodiaz.ticketing.seguridad.application.EmisorDeTokens.TokenEmitido;
import com.orlandodiaz.ticketing.seguridad.domain.Usuario;

/** CASO DE USO: cambiar usuario + clave por un token. Si algo no coincide, siempre el mismo error. */
public class IniciarSesion {

    private final RepositorioDeUsuarios usuarios;
    private final CifradorDeClaves cifrador;
    private final EmisorDeTokens emisor;

    public IniciarSesion(RepositorioDeUsuarios usuarios, CifradorDeClaves cifrador, EmisorDeTokens emisor) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.emisor = emisor;
    }

    public TokenEmitido ejecutar(String nombre, String clave) {
        if (nombre == null || clave == null) {
            throw new CredencialesInvalidasException();
        }
        Usuario usuario = usuarios.buscarPorNombre(nombre).orElseThrow(CredencialesInvalidasException::new);
        if (!cifrador.coincide(clave, usuario.claveCifrada())) {
            throw new CredencialesInvalidasException();
        }
        return emisor.emitir(usuario);
    }
}
