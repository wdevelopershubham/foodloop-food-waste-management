package com.foodloop.foodloop_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FoodloopBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodloopBackendApplication.class, args);
    }
}
