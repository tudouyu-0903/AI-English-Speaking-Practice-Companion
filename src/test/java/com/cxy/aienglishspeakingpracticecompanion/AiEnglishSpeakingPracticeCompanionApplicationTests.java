package com.cxy.aienglishspeakingpracticecompanion;

import com.cxy.aienglishspeakingpracticecompanion.app.SpeakApp;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiEnglishSpeakingPracticeCompanionApplicationTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private SpeakApp speakApp;
    @Test
    void baseSpeak() {
        String userInput="hello,Can you talk with me";
        String content = speakApp.baseSpeak(userInput);
        System.out.println(content);
    }
}
