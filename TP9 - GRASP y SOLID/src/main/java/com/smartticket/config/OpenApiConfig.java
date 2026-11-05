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
                .version("Etapa 1")
                .description("Infraestructura y rutas preparadas. Las historias de usuario todavia no estan implementadas."));
    }
}
