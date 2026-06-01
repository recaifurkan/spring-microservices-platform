package com.example.userservice.config;

import com.example.common.security.JwtConverterAutoConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/** JwtAuthenticationConverter → common modülündeki JwtConverterAutoConfig tarafından sağlanır. */
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
                // All users endpoint → users:manage (ADMIN only)
                .requestMatchers(HttpMethod.GET,    "/api/users").hasAuthority("SCOPE_users:manage")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("SCOPE_users:manage")
                .requestMatchers(HttpMethod.GET,    "/api/users/**").hasAnyAuthority("SCOPE_users:manage", "SCOPE_users:read")
                // Own profile → users:read / users:write
                .requestMatchers(HttpMethod.GET,  "/api/users/profile").hasAuthority("SCOPE_users:read")
                .requestMatchers(HttpMethod.POST, "/api/users/profile").hasAuthority("SCOPE_users:write")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }
}
