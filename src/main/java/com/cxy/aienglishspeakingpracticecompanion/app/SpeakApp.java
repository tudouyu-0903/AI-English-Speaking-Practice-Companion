package com.cxy.aienglishspeakingpracticecompanion.app;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam;


import com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult;
import com.cxy.aienglishspeakingpracticecompanion.advisor.MyLoggerAdvisor;
import com.cxy.aienglishspeakingpracticecompanion.chatMemory.RedisChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/***
 * AI回复
 */
@Slf4j
@Component
public class SpeakApp {
    private final ChatClient chatClient;
    private static final String SYSTEM_PROMPT = """
            You are a friendly and patient English speaking partner.\s
          
            Rules for this conversation:
            1. Roleplay naturally based on the topic the user brings up.
            2. Keep your responses very short (1-2 sentences maximum) so it feels like a real spoken conversation.
            3. Use simple, everyday, casual English.
            4. Do NOT point out or correct the user's grammar mistakes now. Just focus on a smooth conversation.
            5. Always end your response with an open-ended question to keep the chat going.
            
            Start now by introducing yourself briefly and ask the user what they want to talk about today.
            """;
    public SpeakApp(ChatModel dashscopeChatModel, RedisChatMemory redisChatMemory) {
        ChatMemory chatMemory = redisChatMemory;
        DashScopeChatOptions multimodalOptions = DashScopeChatOptions.builder()
                .withModel("qwen3.5-plus")
                .withMultiModel(true)
                .withTemperature(0.7)
                .build();

        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultOptions(multimodalOptions) // 注入多模态配置
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor())
                .build();
    }
    public String baseSpeak(String userInput) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(userInput)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }
    public String audioSpeak(String userRecognizedText,String sessionId) {
        // Codex 修改：打印传入记忆 advisor 的会话 id，避免前端未传 sessionId 导致 Redis key 查错。
        log.info("audioSpeak memory sessionId={}, recognizedTextLength={}", sessionId, userRecognizedText == null ? 0 : userRecognizedText.length());
        ChatResponse chatResponse = chatClient.prompt()
                .user(userRecognizedText)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;

    }


}
