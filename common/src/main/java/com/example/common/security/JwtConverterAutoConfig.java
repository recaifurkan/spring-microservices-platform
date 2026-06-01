package com.example.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Tüm resource-server servisleri için ortak JWT → GrantedAuthority dönüştürücüsü.
 *
 * Kural:
 *   - JWT "scope" claim'i → SCOPE_xxx authority
 *   - JWT "roles" claim'i → ROLE_xxx authority (olduğu gibi eklenir)
 *
 * Hangi rolün hangi scope'a sahip olduğu auth-server/RoleScopeMapper.java
 * içinde merkezi olarak tanımlanır. Servislerde asla hasRole() kullanılmaz,
 * sadece hasAuthority("SCOPE_xxx") kullanılır.
 *
 * @ConditionalOnMissingBean ile servislerin kendi bean'ini tanımlamasına izin verir.
 */
@AutoConfiguration
public class JwtConverterAutoConfig {

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            // scope claim: "catalog:read catalog:write" → SCOPE_catalog:read, SCOPE_catalog:write
            String scope = jwt.getClaimAsString("scope");
            if (scope != null)
                for (String s : scope.split("\\s+"))
                    if (!s.isBlank()) authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));

            // roles claim: ["ROLE_ADMIN"] → ROLE_ADMIN
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) roles.forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));

            return new ArrayList<>(authorities);
        });
        return converter;
    }
}

