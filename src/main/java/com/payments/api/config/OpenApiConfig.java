package com.payments.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Payments API",
        version = "1.0.0",
        description = "API REST para processamento de pagamentos utilizando Arquitetura Hexagonal",
        contact = @Contact(
            name = "Payments API Support"
        )
    ),
    servers = {
        @Server(
            description = "Local",
            url = "http://localhost:8080"
        )
    }
)
public class OpenApiConfig {
}

