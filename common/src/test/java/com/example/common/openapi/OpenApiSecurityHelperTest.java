package com.example.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiSecurityHelperTest {

    @Test
    void givenHelperWhenSecurityRequirementsThenReturnsBearerAndOauth2() {
        assertThat(OpenApiSecurityHelper.securityRequirements())
                .extracting(SecurityRequirement::keySet)
                .containsExactlyInAnyOrder(
                        java.util.Set.of("bearerAuth"),
                        java.util.Set.of("oauth2")
                );
    }

    @Test
    void givenHelperWhenSecurityComponentsThenContainsBearerAndOauth2Schemes() {
        Components components = OpenApiSecurityHelper.securityComponents();

        assertThat(components.getSecuritySchemes()).containsKeys("bearerAuth", "oauth2");
        SecurityScheme bearer = components.getSecuritySchemes().get("bearerAuth");
        SecurityScheme oauth2 = components.getSecuritySchemes().get("oauth2");

        assertThat(bearer.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(bearer.getScheme()).isEqualTo("bearer");
        assertThat(bearer.getBearerFormat()).isEqualTo("JWT");
        assertThat(oauth2.getType()).isEqualTo(SecurityScheme.Type.OAUTH2);
        assertThat(oauth2.getFlows().getAuthorizationCode().getAuthorizationUrl())
                .hasToString("http://localhost:9000/oauth2/authorize");
        assertThat(oauth2.getFlows().getAuthorizationCode().getTokenUrl())
                .hasToString("http://localhost:9000/oauth2/token");
    }
}

