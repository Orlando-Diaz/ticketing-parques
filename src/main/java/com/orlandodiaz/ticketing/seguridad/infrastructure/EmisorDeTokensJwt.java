package com.orlandodiaz.ticketing.seguridad.infrastructure;

import com.orlandodiaz.ticketing.seguridad.application.EmisorDeTokens;
import com.orlandodiaz.ticketing.seguridad.domain.Usuario;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** Adaptador: JWT firmado con HMAC-SHA256. El token lleva el usuario (sub), el rol y cuando vence. */
public class EmisorDeTokensJwt implements EmisorDeTokens {

    public static final String CLAIM_ROL = "rol";

    private final JwtEncoder codificador;
    private final Duration vigencia;

    public EmisorDeTokensJwt(JwtEncoder codificador, Duration vigencia) {
        this.codificador = codificador;
        this.vigencia = vigencia;
    }

    @Override
    public TokenEmitido emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.nombre())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vigencia))
                .claim(CLAIM_ROL, usuario.rol().name())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new TokenEmitido(token, vigencia.toSeconds());
    }
}
