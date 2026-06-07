package com.cxy.aienglishspeakingpracticecompanion.app;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.cxy.aienglishspeakingpracticecompanion.advisor.MyLoggerAdvisor;
import com.cxy.aienglishspeakingpracticecompanion.chatMemory.RedisChatMemory;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.ScenePromptFactory;
import com.cxy.aienglishspeakingpracticecompanion.excption.BusinessException;
import com.cxy.aienglishspeakingpracticecompanion.excption.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Slf4j
@Component
public class SpeakApp {

    private final ChatClient chatClient;
    private final ChatClient summaryChatClient;
    private final RedisChatMemory redisChatMemory;

    private static final String SYSTEM_PROMPT = """
            You are a friendly and patient English speaking partner.

            Rules for this conversation:
            1. Roleplay naturally based on the topic the user brings up.
            2. Keep your responses very short (1-2 sentences maximum) so it feels like a real spoken conversation.
            3. Use simple, everyday, casual English.
            4. Do NOT point out or correct the user's grammar mistakes now. Just focus on a smooth conversation.
            5. Always end your response with an open-ended question to keep the chat going.

            Start now by introducing yourself briefly and ask the user what they want to talk about today.
            """;

    private static final String SUMMARY_SYSTEM_PROMPT = """
            You are an English speaking coach.

            Analyze the conversation provided by the user and return ONLY valid JSON.
            Do not wrap it in markdown.
            Do not add explanations outside JSON.

            JSON format:
            {
              "summary": "Brief summary of this practice conversation.",
              "totalScore": 0,
              "fluencyScore": 0,
              "pronunciationScore": 0,
              "grammarScore": 0,
              "corrections": [
                "Original: ... | Correction: ... | Reason: ..."
              ],
              "suggestions": [
                "..."
              ]
            }

            Scoring rules:
            - totalScore: 0-100
            - fluencyScore: 0-100
            - pronunciationScore: 0-100
            - grammarScore: 0-100

            Important:
            - If pronunciation cannot be judged from text only, give a conservative score and explain this limitation in suggestions.
            - Focus corrections only on the user's English, not the AI's replies.
            """;

    public SpeakApp(ChatModel dashscopeChatModel, RedisChatMemory redisChatMemory) {
        this.redisChatMemory = redisChatMemory;

        DashScopeChatOptions chatOptions = DashScopeChatOptions.builder()
                .withModel("qwen3.5-plus")
                .withMultiModel(true)
                .withTemperature(0.7)
                .build();

        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultOptions(chatOptions)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(redisChatMemory),
                        new MyLoggerAdvisor())
                .build();

        this.summaryChatClient = ChatClient.builder(dashscopeChatModel)
                .defaultOptions(chatOptions)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
    }

    /***
     * 纯文本对话
     * @param userInput
     * @return
     */
    public String baseSpeak(String userInput) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(userInput)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }

    /***
     * 音频对话
     * @param userRecognizedText
     * @param sessionId
     * @return
     */
    public String audioSpeak(String userRecognizedText, String sessionId) {
        log.info("audioSpeak memory sessionId={}, recognizedTextLength={}",
                sessionId,
                userRecognizedText == null ? 0 : userRecognizedText.length());

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

    /***
     * 场景对话
     * @param userRecognizedText
     * @param sessionId
     * @param scene
     * @return
     */
    public String sceneSpeak(String userRecognizedText, String sessionId, String scene) {
        if (scene == null || scene.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场景不能为空");
        }

        String scenePrompt = ScenePromptFactory.buildFinalPrompt(scene);
        ChatResponse chatResponse = chatClient.prompt()
                .system(scenePrompt)
                .user(userRecognizedText)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }

    /***
     * 自定义场景对话
     * @param userRecognizedText
     * @param sessionId
     * @param customScenario
     * @return
     */
    public String customSpeak(String userRecognizedText, String sessionId, String customScenario) {
        String scenePrompt = ScenePromptFactory.buildCustomPrompt(customScenario);
        ChatResponse chatResponse = chatClient.prompt()
                .system(scenePrompt)
                .user(userRecognizedText)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }

    /***
     * 会话总结
     * @param conversationId
     * @return
     */
    public String summarySpeak(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话ID不能为空");
        }
        //会话ID获取完整对话
        List<Message> fullHistory = redisChatMemory.getFullHistory(conversationId);
        if (fullHistory == null || fullHistory.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或暂无对话内容");
        }

        String conversationText = fullHistory.stream()
                .map(message -> message.getMessageType() + ": " + message.getText())
                .collect(Collectors.joining("\n"));
        //会话总结
        ChatResponse chatResponse = summaryChatClient.prompt()
                .system(SUMMARY_SYSTEM_PROMPT)
                .user("Conversation:\n" + conversationText)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI总结结果为空");
        }

        log.info("summary content:{}", content);
        return content;
    }
}
