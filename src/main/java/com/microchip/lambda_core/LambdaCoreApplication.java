package com.microchip.lambda_core;

import com.microchip.lambda_core.storage.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(StorageProperties.class)
public class LambdaCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(LambdaCoreApplication.class, args);
    }
}
