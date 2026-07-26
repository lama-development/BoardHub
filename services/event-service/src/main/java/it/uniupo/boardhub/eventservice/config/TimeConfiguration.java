package it.uniupo.boardhub.eventservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfiguration {

    // Centralizza l'orologio UTC per rendere scadenze e test deterministici.
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }
}
