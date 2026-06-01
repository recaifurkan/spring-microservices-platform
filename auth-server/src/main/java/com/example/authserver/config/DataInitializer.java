package com.example.authserver.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2 client kayıtları.
 *
 * Scope tasarımı (Netflix/e-commerce tarzı — resource-based granüler):
 *
 *   OIDC scopes  : openid, profile, email
 *   Katalog      : catalog:read, catalog:write
 *   Siparişler   : orders:read, orders:write, orders:manage
 *   Sepet        : cart:read, cart:write
 *   Ödemeler     : payments:read, payments:write, payments:manage
 *   Kullanıcılar : users:read, users:write, users:manage
 *   Bildirimler  : notifications:read, notifications:write
 *
 * Roller (JWT roles[] claim):
 *   ROLE_USER    → Standart müşteri
 *   ROLE_MANAGER → Mağaza yöneticisi (catalog:write, orders:manage, payments:manage)
 *   ROLE_ADMIN   → Platform yöneticisi (users:manage dahil her şey)
 */
@Configuration
public class DataInitializer {

    /** Tüm kullanıcı scope'ları (frontend-client ve client-app için) */
    private static final String[] ALL_SCOPES = {
        OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL,
        "catalog:read",  "catalog:write",
        "orders:read",   "orders:write",   "orders:manage",
        "cart:read",     "cart:write",
        "payments:read", "payments:write", "payments:manage",
        "users:read",    "users:write",    "users:manage",
        "notifications:read", "notifications:write"
    };

    @Bean
    ApplicationRunner initOAuth2Clients(RegisteredClientRepository repo) {
        return args -> {

            // ── client-app ───────────────────────────────────────────────────
            if (repo.findByClientId("client-app") == null) {
                RegisteredClient.Builder b = RegisteredClient
                        .withId(UUID.randomUUID().toString())
                        .clientId("client-app")
                        .clientSecret("{noop}secret")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .redirectUri("http://127.0.0.1:8081/login/oauth2/code/client-app-oidc")
                        .redirectUri("http://127.0.0.1:8081/authorized")
                        .redirectUri("http://localhost:8080/swagger-ui/oauth2-redirect.html")
                        .redirectUri("http://localhost:8080/swagger-ui/swagger-ui/oauth2-redirect.html")
                        .redirectUri("http://localhost:8081/swagger-ui/oauth2-redirect.html")
                        .redirectUri("http://localhost:8081/swagger-ui/swagger-ui/oauth2-redirect.html")
                        .tokenSettings(TokenSettings.builder()
                                .accessTokenTimeToLive(Duration.ofMinutes(30))
                                .refreshTokenTimeToLive(Duration.ofDays(1))
                                .reuseRefreshTokens(false)
                                .build())
                        .clientSettings(ClientSettings.builder()
                                .requireAuthorizationConsent(false)
                                .build());
                for (String s : ALL_SCOPES) b = b.scope(s);
                repo.save(b.build());
            }

            // ── frontend-client ──────────────────────────────────────────────
            if (repo.findByClientId("frontend-client") == null) {
                RegisteredClient.Builder b = RegisteredClient
                        .withId(UUID.randomUUID().toString())
                        .clientId("frontend-client")
                        .clientSecret("{noop}frontend-secret")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .redirectUri("http://localhost:8070/login/oauth2/code/auth-server")
                        .redirectUri("http://127.0.0.1:8070/login/oauth2/code/auth-server")
                        .postLogoutRedirectUri("http://localhost:8070/")
                        .tokenSettings(TokenSettings.builder()
                                .accessTokenTimeToLive(Duration.ofHours(1))
                                .refreshTokenTimeToLive(Duration.ofDays(7))
                                .reuseRefreshTokens(false)
                                .build())
                        .clientSettings(ClientSettings.builder()
                                .requireAuthorizationConsent(false)
                                .requireProofKey(false)
                                .build());
                for (String s : ALL_SCOPES) b = b.scope(s);
                repo.save(b.build());
            }

            // ── service-account ──────────────────────────────────────────────
            // Inter-service communication
            if (repo.findByClientId("service-account") == null) {
                repo.save(RegisteredClient
                        .withId(UUID.randomUUID().toString())
                        .clientId("service-account")
                        .clientSecret("{noop}service-secret")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .scope("catalog:read").scope("catalog:write")
                        .scope("orders:read").scope("orders:write").scope("orders:manage")
                        .scope("payments:read").scope("payments:write").scope("payments:manage")
                        .scope("notifications:read").scope("notifications:write")
                        .tokenSettings(TokenSettings.builder()
                                .accessTokenTimeToLive(Duration.ofHours(1))
                                .build())
                        .build());
            }
        };
    }
}
