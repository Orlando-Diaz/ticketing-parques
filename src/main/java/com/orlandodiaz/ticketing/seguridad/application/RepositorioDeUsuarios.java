package com.orlandodiaz.ticketing.seguridad.application;

import com.orlandodiaz.ticketing.seguridad.domain.Usuario;
import java.util.Optional;

/** PUERTO: donde se guardan los usuarios. */
public interface RepositorioDeUsuarios {

    Optional<Usuario> buscarPorNombre(String nombre);

    /** @throws UsuarioYaExisteException si ya hay un usuario con ese nombre (tambien si dos lo crean a la vez) */
    void guardar(Usuario usuario);
}
