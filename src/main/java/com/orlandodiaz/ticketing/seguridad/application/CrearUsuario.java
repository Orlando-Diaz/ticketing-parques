package com.orlandodiaz.ticketing.seguridad.application;

import com.orlandodiaz.ticketing.seguridad.domain.Rol;
import com.orlandodiaz.ticketing.seguridad.domain.Usuario;

/** CASO DE USO: dar de alta a un miembro del personal. La clave se cifra aqui; nunca se guarda en claro. */
public class CrearUsuario {

    public static final int LARGO_MINIMO_DE_CLAVE = 10;

    private final RepositorioDeUsuarios usuarios;
    private final CifradorDeClaves cifrador;

    public CrearUsuario(RepositorioDeUsuarios usuarios, CifradorDeClaves cifrador) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
    }

    public Usuario ejecutar(String nombre, String clave, Rol rol) {
        if (clave == null || clave.length() < LARGO_MINIMO_DE_CLAVE) {
            throw new IllegalArgumentException("La clave debe tener al menos " + LARGO_MINIMO_DE_CLAVE + " caracteres");
        }
        Usuario usuario = new Usuario(nombre, cifrador.cifrar(clave), rol);
        if (usuarios.buscarPorNombre(nombre).isPresent()) {
            throw new UsuarioYaExisteException(nombre);
        }
        usuarios.guardar(usuario); // si otro lo creo justo ahora, el repositorio tambien lanza UsuarioYaExisteException
        return usuario;
    }
}
