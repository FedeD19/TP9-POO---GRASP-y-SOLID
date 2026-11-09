package com.smartticket.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI smartTicketOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("SmartTicket API")
                .version("Etapa 2")
                .description("CRUD de Lugares, Sectores, Eventos y Clientes. Las historias de usuario aun no estan implementadas."));
    }
}
