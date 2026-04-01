package com.f2cg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class F2cgApplication {

    public static void main(String[] args) {
        SpringApplication.run(F2cgApplication.class, args);
    }
}