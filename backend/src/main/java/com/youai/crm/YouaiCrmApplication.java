package com.youai.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YouaiCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(YouaiCrmApplication.class, args);
    }
}

