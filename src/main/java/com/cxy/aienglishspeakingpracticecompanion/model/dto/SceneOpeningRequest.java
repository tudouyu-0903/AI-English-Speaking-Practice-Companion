package com.cxy.aienglishspeakingpracticecompanion.model.dto;

import lombok.Data;

@Data
public class SceneOpeningRequest {
    private String sessionId;
    private String scene;
    private String customScenario;
}