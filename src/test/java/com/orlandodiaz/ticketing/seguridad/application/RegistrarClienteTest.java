package com.orlandodiaz.ticketing.seguridad.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.seguridad.domain.Rol;
import com.orlandodiaz.ticketing.seguridad.domain.Usuario;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegistrarClienteTest {

    private final Map<String, Usuario> guardados = new HashMap<>();
    private final RepositorioDeUsuarios repositorio = new RepositorioDeUsuarios() {
        @Override
        public Optional<Usuario> buscarPorNombre(String nombre) {
            return Optional.ofNullable(guardados.get(nombre));
        }

        @Override
        public void guardar(Usuario usuario) {
            guardados.put(usuario.nombre(), usuario);
        }
    };
    private final CifradorDeClaves cifrador = new CifradorDeClaves() {
        @Override
        public String cifrar(String claveEnClaro) {
            return "hash(" + claveEnClaro + ")";
        }

        @Override
        public boolean coincide(String claveEnClaro, String claveCifrada) {
            return cifrar(claveEnClaro).equals(claveCifrada);
        }
    };
    private final RegistrarCliente registrar = new RegistrarCliente(repositorio, cifrador);

    @Test
    void creaUnClienteConElCorreoEnMinusculasComoUsuario() {
        Usuario u = registrar.ejecutar("  Ana Perez ", "  Ana@Correo.COM ", "clave-larga-123");

        assertThat(u.nombre()).isEqualTo("ana@correo.com");
        assertThat(u.rol()).isEqualTo(Rol.CLIENTE);
        assertThat(u.nombreVisible()).isEqualTo("Ana Perez");
        assertThat(guardados.get("ana@correo.com").claveCifrada()).isEqualTo("hash(clave-larga-123)");
    }

    @Test
    void rechazaCorreosRepetidosInvalidosYClavesCortas() {
        registrar.ejecutar("Ana", "ana@correo.com", "clave-larga-123");

        assertThatThrownBy(() -> registrar.ejecutar("Otra", "ANA@correo.com", "clave-larga-123"))
                .isInstanceOf(UsuarioYaExisteException.class);
        assertThatThrownBy(() -> registrar.ejecutar("Ana", "sin-arroba", "clave-larga-123"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registrar.ejecutar("Ana", "b@correo.com", "corta"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registrar.ejecutar(" ", "c@correo.com", "clave-larga-123"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(guardados).hasSize(1);
    }

    @Test
    void unClienteSoloPuedeTenerUnCorreoComoNombre() {
        assertThatThrownBy(() -> new Usuario("ana", "hash", Rol.CLIENTE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new Usuario("puerta1", "hash", Rol.PUERTA).paraMostrar()).isEqualTo("puerta1");
    }
}
