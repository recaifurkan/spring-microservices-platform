package com.example.paymentservice.config;

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
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/3ds/callback").permitAll()
                // Payment confirmation / refund → payments:manage (ADMIN or MANAGER)
                .requestMatchers(HttpMethod.PUT, "/api/payments/*/confirm").hasAuthority("SCOPE_payments:manage")
                .requestMatchers(HttpMethod.PUT, "/api/payments/*/refund").hasAuthority("SCOPE_payments:manage")
                // Payment history → payments:read
                .requestMatchers(HttpMethod.GET, "/api/payments/**").hasAuthority("SCOPE_payments:read")
                // Start payment → payments:write
                .requestMatchers(HttpMethod.POST, "/api/payments/**").hasAuthority("SCOPE_payments:write")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }
}
