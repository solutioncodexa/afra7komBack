package com.afra7kom.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.afra7kom.backend", "com.afra7kom.service"})
@EntityScan(basePackages = {"com.afra7kom.backend.entity"})
@EnableJpaRepositories(basePackages = {"com.afra7kom.backend.repository"})
public class Afra7komBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(Afra7komBackendApplication.class, args);
    }
}

