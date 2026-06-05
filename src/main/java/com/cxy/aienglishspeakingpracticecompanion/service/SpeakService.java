package com.cxy.aienglishspeakingpracticecompanion.service;

import com.cxy.aienglishspeakingpracticecompanion.dto.VoiceChatResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SpeakService {
    VoiceChatResponse speakWithAI(MultipartFile file, String sessionId);
}
