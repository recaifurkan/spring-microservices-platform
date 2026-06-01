package com.example.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConverterAutoConfigTest {

    private final JwtConverterAutoConfig config = new JwtConverterAutoConfig();

    @Test
    void givenScopeAndRolesWhenConvertThenMapsAuthorities() {
        var converter = config.jwtAuthenticationConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "catalog:read  orders:write ")
                .claim("roles", List.of("ROLE_ADMIN", "ROLE_USER"))
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        JwtAuthenticationToken authentication = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SCOPE_catalog:read", "SCOPE_orders:write", "ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void givenBlankScopeAndMissingRolesWhenConvertThenIgnoresEmptyValues() {
        var converter = config.jwtAuthenticationConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "  ")
                .claim("roles", List.of())
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        JwtAuthenticationToken authentication = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authentication.getAuthorities()).isEmpty();
        assertThat(authentication.getTokenAttributes()).containsEntry("scope", "  ");
    }
}

