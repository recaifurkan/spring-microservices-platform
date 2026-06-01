package com.example.cart.config;

import com.example.common.security.JwtConverterAutoConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/** JwtAuthenticationConverter → common modülündeki JwtConverterAutoConfig tarafından sağlanır. */
@Configuration @EnableWebSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtConverterAutoConfig().jwtAuthenticationConverter();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtAuthenticationConverter jwtConverter) throws Exception {
        http.csrf(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET,    "/api/cart", "/api/cart/**").hasAuthority("SCOPE_cart:read")
                .requestMatchers(HttpMethod.POST,   "/api/cart/**").hasAuthority("SCOPE_cart:write")
                .requestMatchers(HttpMethod.PUT,    "/api/cart/**").hasAuthority("SCOPE_cart:write")
                .requestMatchers(HttpMethod.DELETE, "/api/cart/**").hasAuthority("SCOPE_cart:write")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }
}
