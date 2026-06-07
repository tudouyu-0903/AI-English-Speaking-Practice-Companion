package com.cxy.aienglishspeakingpracticecompanion.model.dto;

import lombok.Data;

@Data
public class VoiceTranscribeResponse {
    private String userText;
    private String asrTaskId;
}