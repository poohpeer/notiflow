package com.alex.notiflow.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NotiflowApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotiflowApiApplication.class, args);
    }
}
