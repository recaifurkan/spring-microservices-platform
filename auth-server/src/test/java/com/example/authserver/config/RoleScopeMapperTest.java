package com.example.authserver.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleScopeMapperTest {

    @Test
    void givenUserRoleWhenScopesRequestedThenReturnsDefaultUserScopes() {
        assertThat(RoleScopeMapper.scopesFor(List.of("ROLE_USER")))
                .contains("catalog:read", "orders:read", "orders:write", "cart:read", "cart:write");
    }

    @Test
    void givenAdminRoleWhenScopesRequestedThenReturnsManagerAndAdminScopes() {
        assertThat(RoleScopeMapper.scopesFor(List.of("ROLE_ADMIN")))
                .contains("catalog:write", "orders:manage", "payments:manage", "users:manage");
    }

    @Test
    void givenUnknownRoleWhenScopesRequestedThenReturnsEmptySet() {
        assertThat(RoleScopeMapper.scopesFor(List.of("ROLE_GUEST"))).isEmpty();
    }
}

