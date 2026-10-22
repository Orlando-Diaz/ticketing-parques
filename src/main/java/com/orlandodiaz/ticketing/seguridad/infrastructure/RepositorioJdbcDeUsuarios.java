package com.orlandodiaz.ticketing.seguridad.infrastructure;

import com.orlandodiaz.ticketing.seguridad.application.RepositorioDeUsuarios;
import com.orlandodiaz.ticketing.seguridad.application.UsuarioYaExisteException;
import com.orlandodiaz.ticketing.seguridad.domain.Rol;
import com.orlandodiaz.ticketing.seguridad.domain.Usuario;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Adaptador con SQL directo: son tres columnas y ninguna regla que proteger, no hace falta JPA. */
public class RepositorioJdbcDeUsuarios implements RepositorioDeUsuarios {

    private final JdbcTemplate jdbc;

    public RepositorioJdbcDeUsuarios(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Usuario> buscarPorNombre(String nombre) {
        return jdbc.query("select nombre, clave_cifrada, rol from usuario where nombre = ?",
                (rs, fila) -> new Usuario(rs.getString("nombre"), rs.getString("clave_cifrada"),
                        Rol.valueOf(rs.getString("rol"))),
                nombre).stream().findFirst();
    }

    @Override
    public void guardar(Usuario usuario) {
        try {
            jdbc.update("insert into usuario (nombre, clave_cifrada, rol, creado_en) values (?, ?, ?, ?)",
                    usuario.nombre(), usuario.claveCifrada(), usuario.rol().name(), LocalDateTime.now());
        } catch (DuplicateKeyException e) {
            throw new UsuarioYaExisteException(usuario.nombre());
        }
    }
}
