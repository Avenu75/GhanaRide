package com.ghanaride;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableScheduling
@EntityScan("com.ghanaride.entity")
@EnableJpaRepositories("com.ghanaride.repository")
public class GhanaRideApplication {

    public static void main(String[] args) {
        SpringApplication.run(GhanaRideApplication.class, args);
    }
}