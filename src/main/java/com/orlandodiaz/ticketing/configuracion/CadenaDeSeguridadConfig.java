package com.orlandodiaz.ticketing.configuracion;

import com.orlandodiaz.ticketing.seguridad.domain.Rol;
import com.orlandodiaz.ticketing.seguridad.infrastructure.EmisorDeTokensJwt;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer;

/**
 * Reglas de acceso por ruta y rol. Va aparte y solo se carga en aplicaciones web: los tests de persistencia arrancan
 * Spring SIN servidor web y ahi no existe HttpSecurity.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CadenaDeSeguridadConfig {

    /**
     * CORS: el navegador solo deja que una pagina de OTRO origen (ej. Angular en localhost:4200) llame a la API si
     * la API lo autoriza. Solo se permiten los origenes listados en ticketing.cors.origenes (separados por coma).
     * OJO: el nombre del bean debe ser exactamente "corsConfigurationSource"; Spring Security lo busca por ese nombre.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${ticketing.cors.origenes:http://localhost:4200}") String origenes) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setMaxAge(3600L);
        var fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, JwtDecoder decodificador) throws Exception {
        var autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(EmisorDeTokensJwt.CLAIM_ROL);
        autoridades.setAuthorityPrefix("ROLE_");
        var convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);

        http
                .cors(Customizer.withDefaults()) // usa el bean corsConfigurationSource; responde los preflight antes de autenticar
                .csrf(csrf -> csrf.disable()) // API sin sesiones ni cookies: no hay nada que falsificar
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        // Publico: el visitante consulta, compra y ve lo suyo (el id de la orden es un UUID imposible de adivinar)
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/parques").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/parques/*/disponibilidad",
                                "/api/parques/*/precios/cotizacion").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ordenes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ordenes/*", "/api/ordenes/*/factura",
                                "/api/ordenes/*/pase").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ordenes/*/cancelacion").permitAll()
                        // Personal de la puerta (y administradores)
                        .requestMatchers("/api/accesos/**")
                        .hasAnyRole(Rol.PUERTA.name(), Rol.ADMIN.name())
                        // Solo administradores
                        .requestMatchers("/api/reportes/**", "/api/parques/**", "/api/usuarios/**")
                        .hasRole(Rol.ADMIN.name())
                        // Lo que no listamos aqui requiere al menos estar autenticado (nunca queda abierto por olvido)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(decodificador)
                        .jwtAuthenticationConverter(convertidor)))
                .httpBasic(b -> b.disable());
        return http.build();
    }
}
