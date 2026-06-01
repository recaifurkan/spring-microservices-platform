package com.example.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.List;

/**
 * Helper class that centrally builds the repeated Swagger/OpenAPI OAuth2 security
 * definitions used across all services.
 *
 * Usage:
 * <pre>
 *   return new OpenAPI()
 *       .info(new Info().title("My Service API").version("1.0.0"))
 *       .security(OpenApiSecurityHelper.securityRequirements())
 *       .components(OpenApiSecurityHelper.securityComponents());
 * </pre>
 */
public final class OpenApiSecurityHelper {

    private static final String AUTH_URL  = "http://localhost:9000/oauth2/authorize";
    private static final String TOKEN_URL = "http://localhost:9000/oauth2/token";

    private OpenApiSecurityHelper() {}

    /** Returns the bearerAuth + oauth2 security requirements. */
    public static List<SecurityRequirement> securityRequirements() {
        return List.of(
            new SecurityRequirement().addList("bearerAuth"),
            new SecurityRequirement().addList("oauth2")
        );
    }

    /** Returns Components containing the bearerAuth and oauth2 security schemes. */
    public static Components securityComponents() {
        return new Components()
            .addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT Bearer token (Authorization: Bearer <token>)"))
            .addSecuritySchemes("oauth2",
                new SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                            .authorizationUrl(AUTH_URL)
                            .tokenUrl(TOKEN_URL)
                            .scopes(new Scopes()
                                .addString("openid", "OpenID Connect")
                                .addString("read", "Read access")
                                .addString("write", "Write access")))));
    }
}
