package com.example.notificationservice.config;

import com.example.common.openapi.OpenApiSecurityHelper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Notification Service API")
                .description("Bildirim yönetimi — servis-servis bildirimleri ve kullanıcı okuma")
                .version("1.0.0")
                .contact(new Contact().name("Microservice Demo")))
            .security(OpenApiSecurityHelper.securityRequirements())
            .components(OpenApiSecurityHelper.securityComponents());
    }
}
