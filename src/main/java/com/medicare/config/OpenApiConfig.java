package com.medicare.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "RxPro Pharmacy Management API",
        version = "1.0.0",
        description = "Complete REST API for RxPro Pharmacy Management System",
        contact = @Contact(name = "RxPro Support", email = "support@rxpro.com")
    ),
    servers = {
        @Server(url = "http://localhost:8080/api", description = "Local Development"),
        @Server(url = "https://api.rxpro.com", description = "Production")
    },
    security = @SecurityRequirement(name = "BearerAuth")
)
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Enter your JWT token obtained from /auth/login"
)
public class OpenApiConfig {
}


