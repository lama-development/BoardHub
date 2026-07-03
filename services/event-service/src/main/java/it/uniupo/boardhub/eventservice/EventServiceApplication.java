package it.uniupo.boardhub.eventservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EventServiceApplication {

    // Avvia il microservizio Spring Boot che ascolta e salva gli eventi.
    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}
