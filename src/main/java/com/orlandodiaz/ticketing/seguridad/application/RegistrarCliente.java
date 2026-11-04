package com.orlandodiaz.ticketing.seguridad.application;

import com.orlandodiaz.ticketing.seguridad.domain.Rol;
import com.orlandodiaz.ticketing.seguridad.domain.Usuario;
import java.util.Locale;

/**
 * CASO DE USO: una persona crea su propia cuenta de cliente con su correo y una clave.
 * Su nombre de usuario es el correo en minusculas (asi "Ana@Correo.com" y "ana@correo.com" son la misma cuenta).
 * Todavia no se verifica que el correo sea suyo (limite conocido, ver ADR 0010).
 */
public class RegistrarCliente {

    private final RepositorioDeUsuarios usuarios;
    private final CifradorDeClaves cifrador;

    public RegistrarCliente(RepositorioDeUsuarios usuarios, CifradorDeClaves cifrador) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
    }

    public Usuario ejecutar(String nombre, String correo, String clave) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Escribe tu nombre");
        }
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("Escribe tu correo");
        }
        if (clave == null || clave.length() < CrearUsuario.LARGO_MINIMO_DE_CLAVE) {
            throw new IllegalArgumentException(
                    "La clave debe tener al menos " + CrearUsuario.LARGO_MINIMO_DE_CLAVE + " caracteres");
        }
        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = new Usuario(correoNormalizado, cifrador.cifrar(clave), Rol.CLIENTE, nombre);
        if (usuarios.buscarPorNombre(correoNormalizado).isPresent()) {
            throw new UsuarioYaExisteException(correoNormalizado);
        }
        usuarios.guardar(usuario);
        return usuario;
    }
}
