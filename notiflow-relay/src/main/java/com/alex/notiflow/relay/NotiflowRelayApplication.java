package com.alex.notiflow.relay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NotiflowRelayApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotiflowRelayApplication.class, args);
    }
}
