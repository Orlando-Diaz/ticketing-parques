package com.orlandodiaz.ticketing.configuracion;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.orlandodiaz.ticketing.seguridad.application.CifradorDeClaves;
import com.orlandodiaz.ticketing.seguridad.application.CrearUsuario;
import com.orlandodiaz.ticketing.seguridad.application.EmisorDeTokens;
import com.orlandodiaz.ticketing.seguridad.application.IniciarSesion;
import com.orlandodiaz.ticketing.seguridad.application.RepositorioDeUsuarios;
import com.orlandodiaz.ticketing.seguridad.domain.Rol;
import com.orlandodiaz.ticketing.seguridad.infrastructure.CifradorBCrypt;
import com.orlandodiaz.ticketing.seguridad.infrastructure.EmisorDeTokensJwt;
import com.orlandodiaz.ticketing.seguridad.infrastructure.RepositorioJdbcDeUsuarios;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * RAIZ DE COMPOSICION de seguridad. Autenticacion: JWT firmado (el servidor es a la vez quien lo emite y quien lo
 * valida, con la misma clave secreta). Autorizacion: por rol y por ruta, en {@link CadenaDeSeguridadConfig}.
 * Las credenciales llegan SOLO por variables de entorno: JWT_SECRET (obligatoria) y ADMIN_PASSWORD (opcional).
 */
@Configuration
public class SeguridadConfig {

    @Bean
    SecretKey claveDeFirmaJwt(@Value("${ticketing.seguridad.jwt-secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres (HS256 pide 256 bits)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey claveDeFirmaJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveDeFirmaJwt));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey claveDeFirmaJwt) {
        return NimbusJwtDecoder.withSecretKey(claveDeFirmaJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    // --- caso de uso y adaptadores del modulo seguridad ---

    @Bean
    RepositorioDeUsuarios repositorioDeUsuarios(JdbcTemplate jdbc) {
        return new RepositorioJdbcDeUsuarios(jdbc);
    }

    @Bean
    CifradorDeClaves cifradorDeClaves() {
        return new CifradorBCrypt();
    }

    @Bean
    EmisorDeTokens emisorDeTokens(JwtEncoder codificador,
                                  @Value("${ticketing.seguridad.token-minutos:60}") long minutos) {
        return new EmisorDeTokensJwt(codificador, Duration.ofMinutes(minutos));
    }

    @Bean
    CrearUsuario crearUsuario(RepositorioDeUsuarios usuarios, CifradorDeClaves cifrador) {
        return new CrearUsuario(usuarios, cifrador);
    }

    @Bean
    IniciarSesion iniciarSesion(RepositorioDeUsuarios usuarios, CifradorDeClaves cifrador, EmisorDeTokens emisor) {
        return new IniciarSesion(usuarios, cifrador, emisor);
    }

    /**
     * Al arrancar, si ADMIN_PASSWORD esta definida y el administrador no existe, lo crea. Asi el primer usuario nace
     * sin dejar ninguna clave escrita en el codigo ni en la base en claro. Sin ADMIN_PASSWORD no se crea nadie.
     */
    @Bean
    ApplicationRunner sembrarAdministrador(CrearUsuario crearUsuario, RepositorioDeUsuarios usuarios,
                                           @Value("${ticketing.seguridad.admin-usuario:admin}") String nombre,
                                           @Value("${ticketing.seguridad.admin-clave:}") String clave) {
        return args -> {
            if (clave.isBlank()) {
                System.out.println("ADMIN_PASSWORD no esta definida: no se crea ningun administrador.");
            } else if (usuarios.buscarPorNombre(nombre).isEmpty()) {
                crearUsuario.ejecutar(nombre, clave, Rol.ADMIN);
                System.out.println("Administrador '" + nombre + "' creado.");
            }
        };
    }
}
