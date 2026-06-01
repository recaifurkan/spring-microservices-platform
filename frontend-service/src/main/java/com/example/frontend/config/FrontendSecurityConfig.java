package com.example.frontend.config;

import com.example.frontend.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.frontend.security.OAuth2LoginSuccessHandler;
import com.example.frontend.service.GuestSessionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class FrontendSecurityConfig {

    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepo;
    private final OAuth2LoginSuccessHandler loginSuccessHandler;

    public FrontendSecurityConfig(HttpCookieOAuth2AuthorizationRequestRepository cookieRepo,
                                  OAuth2LoginSuccessHandler loginSuccessHandler) {
        this.cookieRepo          = cookieRepo;
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/home", "/register", "/css/**", "/js/**", "/images/**",
                                 "/actuator/**", "/error", "/payment/result",
                                 "/api/products/suggestions",
                                 "/products", "/products/**",
                                 "/cart", "/cart/add", "/cart/update", "/cart/remove", "/cart/clear")
                    .permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                // The session ID changes, but all attributes (including GUEST_CART) are preserved
                .sessionFixation().changeSessionId()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/dashboard", true)
                .successHandler(loginSuccessHandler)          // ← merge the guest cart
                .failureUrl("/?error=login_failed")
                .authorizationEndpoint(endpoint ->
                    endpoint.authorizationRequestRepository(cookieRepo)

                )
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID",
                        GuestSessionService.OAUTH2_AUTH_REQUEST_COOKIE)
                .clearAuthentication(true)
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    "/cart/**", "/checkout/**", "/orders",
                    "/notifications/**", "/profile", "/register"));
        return http.build();
    }
}
