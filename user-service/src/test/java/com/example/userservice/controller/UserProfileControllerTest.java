package com.example.userservice.controller;

import com.example.userservice.model.UserProfile;
import com.example.userservice.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
@Import(com.example.userservice.config.SecurityConfig.class)
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.com/jwks"
})
class UserProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  UserProfileService service;

    @Test
    void getMyProfile_withJwt_returns200() throws Exception {
        UserProfile p = new UserProfile();
        p.setUserId("sub-123");
        p.setUsername("user");
        when(service.findByUserId("sub-123")).thenReturn(p);

        mockMvc.perform(get("/api/users/profile")
                .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_users:read"))
                    .jwt(j -> j.subject("sub-123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("sub-123"));
    }

    @Test
    void getMyProfile_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileById_withJwt_returns200() throws Exception {
        UserProfile p = new UserProfile();
        p.setUserId("other-user");
        when(service.findByUserId("other-user")).thenReturn(p);

        mockMvc.perform(get("/api/users/other-user")
                .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_users:read"))
                    .jwt(j -> j.subject("other-user"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("other-user"));
    }

    @Test
    void listAll_withAdminJwt_returns200() throws Exception {
        UserProfile p = new UserProfile();
        p.setUserId("admin");
        when(service.findAll()).thenReturn(List.of(p));

        mockMvc.perform(get("/api/users")
                .with(jwt()
                    .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_users:manage"))
                    .jwt(j -> j.subject("admin").claim("roles", List.of("ROLE_ADMIN")))
                ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value("admin"));
    }

    @Test
    void listAll_withoutAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/users")
                .with(jwt().jwt(j -> j.subject("normal-user"))))
            .andExpect(status().isForbidden());
    }
}

