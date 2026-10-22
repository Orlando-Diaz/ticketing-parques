package com.orlandodiaz.ticketing.seguridad.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orlandodiaz.ticketing.seguridad.domain.Rol;
import com.orlandodiaz.ticketing.seguridad.domain.Usuario;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SesionYUsuariosTest {

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
    private final EmisorDeTokens emisor = usuario ->
            new EmisorDeTokens.TokenEmitido("token-de-" + usuario.nombre() + "-" + usuario.rol(), 3600);

    private final CrearUsuario crear = new CrearUsuario(repositorio, cifrador);
    private final IniciarSesion iniciar = new IniciarSesion(repositorio, cifrador, emisor);

    @Test
    void laClaveSeGuardaCifradaNuncaEnClaro() {
        crear.ejecutar("maria", "una-clave-larga", Rol.PUERTA);

        assertThat(guardados.get("maria").claveCifrada()).isEqualTo("hash(una-clave-larga)");
    }

    @Test
    void noSeCreaDosVecesElMismoUsuario() {
        crear.ejecutar("maria", "una-clave-larga", Rol.PUERTA);

        assertThatThrownBy(() -> crear.ejecutar("maria", "otra-clave-larga", Rol.ADMIN))
                .isInstanceOf(UsuarioYaExisteException.class);
    }

    @Test
    void rechazaClavesCortasYNombresInvalidos() {
        assertThatThrownBy(() -> crear.ejecutar("maria", "corta", Rol.PUERTA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> crear.ejecutar("Maria Lopez", "una-clave-larga", Rol.PUERTA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(guardados).isEmpty();
    }

    @Test
    void conCredencialesCorrectasSeEmiteElToken() {
        crear.ejecutar("maria", "una-clave-larga", Rol.PUERTA);

        assertThat(iniciar.ejecutar("maria", "una-clave-larga").valor()).isEqualTo("token-de-maria-PUERTA");
    }

    @Test
    void claveIncorrectaYUsuarioInexistenteFallanIgual() {
        crear.ejecutar("maria", "una-clave-larga", Rol.PUERTA);

        assertThatThrownBy(() -> iniciar.ejecutar("maria", "mala")).isInstanceOf(CredencialesInvalidasException.class);
        assertThatThrownBy(() -> iniciar.ejecutar("nadie", "una-clave-larga")).isInstanceOf(CredencialesInvalidasException.class);
        assertThatThrownBy(() -> iniciar.ejecutar(null, null)).isInstanceOf(CredencialesInvalidasException.class);
    }
}
