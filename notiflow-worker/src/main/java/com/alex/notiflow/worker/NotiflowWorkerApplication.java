package com.alex.notiflow.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class NotiflowWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotiflowWorkerApplication.class, args);
    }
}
