package com.smartticket.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Zona horaria explicita para comparar con el dia del evento y registrar el ingreso. */
@Configuration
public class RelojConfig {
    @Bean
    public Clock relojSmartTicket(
        @Value("${smartticket.zona-horaria:America/Argentina/Buenos_Aires}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
