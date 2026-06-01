package com.example.authserver.config;

import com.example.authserver.repository.UserRepository;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spring Authorization Server – JDBC destekli konfigürasyon.
 *
 * Kaynaklar:
 *   - RegisteredClient'lar     → oauth2_registered_client tablosu (DataInitializer ile eklenir)
 *   - Kullanıcılar & grant'lar → app_users + user_grants tabloları (Flyway V3 ile eklenir)
 *   - Yetkilendirmeler         → oauth2_authorization tablosu
 *
 * Endpoints:
 *   POST /oauth2/token                       → token al
 *   GET  /oauth2/jwks                        → public key
 *   GET  /.well-known/openid-configuration   → OIDC discovery
 *   GET  /h2-console/**                      → H2 yönetim konsolu (sadece dev)
 */
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    @Value("${auth-server.issuer-uri:http://localhost:9000}")
    private String issuerUri;

    // ─────────────────────────────────────────────────────────────────────────
    // 1) Authorization Server Security Filter Chain
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2) Default Security Filter Chain (form login, H2 console)
    // ─────────────────────────────────────────────────────────────────────────
    // 2) Default Security Filter Chain (form login, HTTP Basic, H2 console)
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    @Order(2)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**", "/admin/**", "/register")
                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/h2-console/**").permitAll()
                        // Public user registration endpoint
                        .requestMatchers("/register").permitAll()
                        // User management API → requires ROLE_ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // HTTP Basic auth → curl -u admin:admin123 http://localhost:9000/admin/users
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PasswordEncoder – BCrypt (encodes new user passwords)
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3) JDBC – RegisteredClientRepository (oauth2_registered_client table)
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4) JDBC – OAuth2AuthorizationService (oauth2_authorization table)
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5) JDBC – OAuth2AuthorizationConsentService (oauth2_authorization_consent)
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6) CORS – allow token endpoint access from Swagger UI origins
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:8070", "http://localhost:8080", "http://localhost:8081",
                "http://localhost:8082", "http://localhost:8084", "http://localhost:8085",
                "http://localhost:8086", "http://localhost:8087", "http://localhost:8090",
                "http://127.0.0.1:8070", "http://127.0.0.1:8080", "http://127.0.0.1:8081"));
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.OPTIONS.name()));
        config.setAllowedHeaders(List.of(
                HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION, "X-Requested-With"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7) RSA key pair → feeds the JWK Set endpoint
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8) JWT Decoder
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9) Authorization Server settings
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().issuer(issuerUri).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10) Token Customizer — role-to-scope conversion happens here
    //
    //  JWT structure (authorization_code — user token):
    //    sub                → username
    //    scope              → scopes derived from roles by RoleScopeMapper
    //                         (intersected with the client-requested scopes)
    //    roles              → ["ROLE_USER"] — informational only, not used for authorization
    //    email, name, preferred_username → OIDC claims
    //
    //  JWT structure (client_credentials — service token):
    //    sub   → client_id
    //    scope → the client's registered scopes (not processed by RoleScopeMapper)
    //
    //  RULE:
    //    Endpoints check only SCOPE_xxx, never hasRole().
    //    Adding a new role means adding a case to RoleScopeMapper.java.
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(UserRepository userRepository) {
        return context -> {
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) return;

            context.getClaims().claims(claims -> {

                if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getAuthorizationGrantType())) {
                    String username = context.getPrincipal().getName();

                    userRepository.findByUsername(username).ifPresent(user -> {

                        // ── User roles ─────────────────────────────────────────
                        List<String> roles = user.getGrants().stream()
                                .filter(g -> g.startsWith("ROLE_"))
                                .collect(Collectors.toList());

                        // ── Role → Scope conversion (central rule: RoleScopeMapper) ─
                        // All scopes the user can obtain from their roles
                        Set<String> roleBasedScopes = RoleScopeMapper.scopesFor(roles);

                        // Intersect with the scopes requested by the client
                        // (the client can only request scopes it is allowed to ask for)
                        List<String> grantedScopes = new ArrayList<>();
                        for (String requested : context.getAuthorizedScopes()) {
                            if (isOidcScope(requested) || roleBasedScopes.contains(requested)) {
                                grantedScopes.add(requested);
                            }
                        }

                        claims.put("scope", String.join(" ", grantedScopes));

                        // ── Roles — informational claim for audit/logging ───────
                        // Endpoints should use scopes, not this claim
                        claims.put("roles", roles);

                        // ── Standard OIDC claims ───────────────────────────────
                        claims.put("preferred_username", user.getUsername());
                        if (user.getEmail()    != null) claims.put("email",  user.getEmail());
                        if (user.getFullName() != null) claims.put("name",   user.getFullName());
                    });

                } else if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                    // service-account / machine-to-machine — no user, no roles
                    // grant the client's registered scopes directly
                    claims.put("scope", String.join(" ", context.getAuthorizedScopes()));
                    claims.put("client_id", context.getRegisteredClient().getClientId());
                }
            });
        };
    }

    /** openid, profile, email → standard OIDC scopes, independent of the role table */
    private boolean isOidcScope(String scope) {
        return "openid".equals(scope) || "profile".equals(scope) || "email".equals(scope);
    }
}

