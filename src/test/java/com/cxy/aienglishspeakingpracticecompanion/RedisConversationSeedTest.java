package com.cxy.aienglishspeakingpracticecompanion;

import com.cxy.aienglishspeakingpracticecompanion.chatMemory.RedisChatMemory;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
class RedisConversationSeedTest {

    static final String TEST_SESSION_ID = "apifox-summary-test-session";

    @Resource
    private RedisChatMemory redisChatMemory;

    @Test
    void seedConversationForSummaryApi() {
        redisChatMemory.clear(TEST_SESSION_ID);

        List<Message> messages = List.of(
                new UserMessage("Hello, I want practice ordering food."),
                new AssistantMessage("Sure. Welcome to Star Bites. What would you like to drink first?"),
                new UserMessage("I want a coffee and maybe some chicken sandwich."),
                new AssistantMessage("Of course. Would you like your coffee hot or iced?"),
                new UserMessage("Hot please. And I also want know what dessert you have."),
                new AssistantMessage("We have apple pie, chocolate cake, and ice cream. Which one sounds good to you?"),
                new UserMessage("I think apple pie is good. That's all, thank you."),
                new AssistantMessage("Great choice. Would you like anything else before I prepare your order?")
        );

        redisChatMemory.add(TEST_SESSION_ID, messages);

        System.out.println("Seeded Redis conversation for Apifox.");
        System.out.println("sessionId=" + TEST_SESSION_ID);
        System.out.println("summaryApi=/api/voice/summary?sessionId=" + TEST_SESSION_ID);
    }
}
