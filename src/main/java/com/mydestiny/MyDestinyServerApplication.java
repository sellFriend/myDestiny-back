package com.mydestiny;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MyDestinyServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyDestinyServerApplication.class, args);
    }

}
