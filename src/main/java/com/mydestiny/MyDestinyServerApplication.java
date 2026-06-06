package com.mydestiny;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class MyDestinyServerApplication {

    @PostConstruct
    public void init() {
        // 서버 시간대와 무관하게 한국 시간(KST)으로 동작하도록 고정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(MyDestinyServerApplication.class, args);
    }

}
