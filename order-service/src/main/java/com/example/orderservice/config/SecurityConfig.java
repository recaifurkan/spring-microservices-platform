package com.example.orderservice.config;

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
 * JwtAuthenticationConverter → common modülündeki JwtConverterAutoConfig tarafından sağlanır.
 * When a new role is added, only auth-server/RoleScopeMapper.java needs to be updated.
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
                // Payment confirmation → arrives from payment-service using a service-account token
                .requestMatchers(HttpMethod.POST, "/api/orders/*/confirm-payment").hasAuthority("SCOPE_orders:manage")
                // All orders + status updates → orders:manage scope (ADMIN and MANAGER)
                .requestMatchers(HttpMethod.GET, "/api/orders/all").hasAuthority("SCOPE_orders:manage")
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/status").hasAuthority("SCOPE_orders:manage")
                // View own orders → orders:read scope
                .requestMatchers(HttpMethod.GET, "/api/orders", "/api/orders/**").hasAuthority("SCOPE_orders:read")
                // Create / cancel orders → orders:write scope
                .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAuthority("SCOPE_orders:write")
                .requestMatchers(HttpMethod.PUT,  "/api/orders/**").hasAuthority("SCOPE_orders:write")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }
}
