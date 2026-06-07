package com.cxy.aienglishspeakingpracticecompanion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cxy.aienglishspeakingpracticecompanion.mapper")
public class AiEnglishSpeakingPracticeCompanionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiEnglishSpeakingPracticeCompanionApplication.class, args);
    }

}
