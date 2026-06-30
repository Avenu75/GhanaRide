package com.ghanaride;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GhanaRideApplication {

    public static void main(String[] args) {
        SpringApplication.run(GhanaRideApplication.class, args);
    }

}
