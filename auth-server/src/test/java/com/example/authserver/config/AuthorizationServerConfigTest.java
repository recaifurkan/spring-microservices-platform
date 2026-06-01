package com.example.authserver.config;

import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationServerConfigTest {

    private final AuthorizationServerConfig config = new AuthorizationServerConfig();

    @Test
    void givenIssuerWhenSettingsRequestedThenIssuerIsUsed() {
        ReflectionTestUtils.setField(config, "issuerUri", "http://issuer.test");

        assertThat(config.authorizationServerSettings().getIssuer()).isEqualTo("http://issuer.test");
    }

    @Test
    void givenConfigWhenCorsRequestedThenAllowsExpectedOriginsAndHeaders() {
        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).isNotNull().contains("http://localhost:8070", "http://127.0.0.1:8080");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).contains("Authorization", "Content-Type");
    }

    @Test
    void givenConfigWhenJwkSourceAndDecoderRequestedThenKeyMaterialExists() throws Exception {
        JWKSource<SecurityContext> jwkSource = config.jwkSource();
        JWKSet jwkSet = new JWKSet(jwkSource.get(new JWKSelector(new com.nimbusds.jose.jwk.JWKMatcher.Builder().build()), null));

        assertThat(jwkSet.getKeys()).isNotEmpty();
        assertThat(config.jwtDecoder(jwkSource)).isNotNull();
    }

    @Test
    void givenConfigWhenPasswordEncoderRequestedThenDelegatingEncoderIsReturned() {
        PasswordEncoder encoder = config.passwordEncoder();
        assertThat(encoder.encode("secret")).startsWith("{");
    }
}

