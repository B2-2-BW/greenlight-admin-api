package com.winten.greenlight.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GreenlightAdminApiApplication {

    static void main(String[] args) {
        SpringApplication.run(GreenlightAdminApiApplication.class, args);
    }

}