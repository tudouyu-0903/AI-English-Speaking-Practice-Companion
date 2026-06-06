package com.cxy.aienglishspeakingpracticecompanion.service;

import com.cxy.aienglishspeakingpracticecompanion.dto.VoiceChatResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SpeakService {
    /***
     * 语音对话
     * @param file 用户的录音文件
     * @param sessionId 会话ID
     * @return
     */
    VoiceChatResponse speakWithAI(MultipartFile file, String sessionId,String scene,String customScenario );
}
