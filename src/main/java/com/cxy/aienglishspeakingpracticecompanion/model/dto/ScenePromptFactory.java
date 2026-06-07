package com.cxy.aienglishspeakingpracticecompanion.model.dto;

import com.cxy.aienglishspeakingpracticecompanion.excption.BusinessException;
import com.cxy.aienglishspeakingpracticecompanion.excption.ErrorCode;

public class ScenePromptFactory {

    // 1. 原有基础系统提示词底座
    private static final String BASE_SYSTEM_PROMPT_TEMPLATE = """
            You are a friendly and patient English speaking partner.
            
            Strict rules for this conversation:
            1. Keep your responses very short (2-3 sentences maximum) so it feels like a real spoken conversation.
            2. Use simple, everyday, casual English.
            3. Do NOT point out or correct the user's grammar mistakes now. Just focus on a smooth conversation.
            4. Always end your response with an open-ended question to keep the chat going.
            """;

    /**
     * 根据前端传来的场景标签，动态拼接出最终的系统提示词
     * @param sceneCode 场景标识 (interview / order_food / meeting / check_in)
     * @return 完美拼接后的全量 System Prompt
     */
    public static String buildFinalPrompt(String sceneCode) {
        StringBuilder promptBuilder = new StringBuilder(BASE_SYSTEM_PROMPT_TEMPLATE);
        promptBuilder.append("\nCURRENT SCENARIO ROLEPLAY:\n");
        if (sceneCode == null) {
            sceneCode = "default";
        }
            switch (sceneCode.toLowerCase()) {
                case "interview":
                    promptBuilder.append("""
                        - You are an experienced HR manager interviewing the user for a professional job position at TechGen company.
                        - Conduct a structured interview. Ask ONLY ONE question at a time.
                        - Start now by introducing yourself briefly as the HR manager and ask the first interview question.
                        """);
                    break;

                case "order_food":
                    promptBuilder.append("""
                        - You are a friendly waiter at a busy American diner called "Star Bites". The user is a customer.
                        - Take their order step-by-step (drinks, main course, then dessert). Do not rush.
                        - Start now by greeting the customer warmly and asking if they are ready to order drinks or food.
                        """);
                    break;

                case "meeting":
                    promptBuilder.append("""
                        - You are a project manager hosting an internal team meeting about "The New Mobile App Launch". The user is your colleague.
                        - Lead the meeting agenda item by item (progress, roadblocks, next steps).
                        - Start now by welcoming the user to the meeting and asking them for a brief status update on their recent tasks.
                        """);
                    break;

                case "check_in":
                    promptBuilder.append("""
                        - You are a professional ground crew agent at an airport check-in counter for "Global Airways". The user is a passenger.
                        - Follow standard check-in procedures: Ask for passport -> Ask about checked baggage -> Seat preference. Ask for ONLY ONE thing at a time.
                        - Start now by greeting the passenger politely and asking for their passport and booking details.
                        """);
                    break;
                default:
                    // 默认随聊兜底场景
                    promptBuilder.append("""
                        - There is no specific topic. Roleplay naturally based on whatever the user brings up.
                        - Start now by introducing yourself briefly and ask the user what they want to talk about today.
                        """);
                    break;
                }
        return promptBuilder.toString();
    }
    /**
     * 根据用户自定义的场景描述，动态拼接出最终的系统提示词
     * @param customScenario 用户输入的自定义角色/场景 (例如: "An IELTS examiner testing my speaking skills")
     * @return 完美拼接后的全量 System Prompt
     */
    public static String buildCustomPrompt(String customScenario) {
        if (customScenario == null || customScenario.isBlank()) {
            // 如果用户没传，可以走默认随聊场景
            return buildFinalPrompt("default");
        }

        StringBuilder promptBuilder = new StringBuilder(BASE_SYSTEM_PROMPT_TEMPLATE);
        promptBuilder.append("\nCURRENT SCENARIO ROLEPLAY:\n");
        // 动态拼接用户自定义的场景规则
        promptBuilder.append("- ").append(customScenario.trim()).append("\n");

        return promptBuilder.toString();
    }

}