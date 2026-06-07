package com.cxy.aienglishspeakingpracticecompanion.model.dto;

import lombok.Data;

@Data
public class TextChatRequest {
    private String sessionId;
    private String userText;
    private String scene;
    private String customScenario;
}