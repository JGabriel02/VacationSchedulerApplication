package com.JoaoGabriel.vacation_scheduler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/*
 * Relógio único da aplicação. O servidor (Render) roda em UTC, mas as regras
 * de férias ("hoje", "data futura") devem seguir o fuso da empresa.
 * Também permite usar um relógio fixo nos testes.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(
            @Value("${app.time-zone:America/Sao_Paulo}") String timeZone
    ) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
