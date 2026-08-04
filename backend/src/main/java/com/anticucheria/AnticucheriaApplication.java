package com.anticucheria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AnticucheriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnticucheriaApplication.class, args);
    }
}
