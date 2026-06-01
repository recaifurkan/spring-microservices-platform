package com.example.productservice.config;

import com.example.common.security.JwtConverterAutoConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * YETKİLENDİRME KURALI:
 *   Sadece SCOPE_xxx kontrol edilir, asla hasRole() kullanılmaz.
 *   Hangi rolün hangi scope'a sahip olduğu auth-server/RoleScopeMapper.java
 *   içinde merkezi olarak tanımlanır.
 *
 *   JwtAuthenticationConverter → common modülündeki JwtConverterAutoConfig tarafından sağlanır.
 */
@Configuration @EnableWebSecurity @EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtConverterAutoConfig().jwtAuthenticationConverter();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtAuthenticationConverter jwtConverter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                // Stok azaltma / CUD → catalog:write scope (RoleScopeMapper: ADMIN ve MANAGER)
                .requestMatchers(HttpMethod.PATCH,  "/api/products/**").hasAuthority("SCOPE_catalog:write")
                .requestMatchers(HttpMethod.POST,   "/api/products/**").hasAuthority("SCOPE_catalog:write")
                .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasAuthority("SCOPE_catalog:write")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAuthority("SCOPE_catalog:write")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }
}
