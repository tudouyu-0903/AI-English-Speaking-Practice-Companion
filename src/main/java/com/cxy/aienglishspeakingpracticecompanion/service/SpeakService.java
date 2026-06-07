package com.cxy.aienglishspeakingpracticecompanion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.ConversationSummaryResponse;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.VoiceChatResponse;
import com.cxy.aienglishspeakingpracticecompanion.model.dto.VoiceTranscribeResponse;
import com.cxy.aienglishspeakingpracticecompanion.model.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface SpeakService {
    /***
     * 用户语音转文字
     * @param file
     * @param sessionId
     * @return
     */
    VoiceTranscribeResponse transcribeVoice(MultipartFile file, String sessionId);
    /***
     * 将用户确认后的回答交给AI
     * @param userText
     * @param sessionId
     * @param scene
     * @param customScenario
     * @return
     */
    VoiceChatResponse chatWithConfirmedText(String userText, String sessionId, String scene, String customScenario);

    /***
     * 会话总结
     * @param sessionId 会话ID
     * @return 返回会话总结结果
     */
    ConversationSummaryResponse ConversationSummary(String sessionId);

    /**
     * 设置对话场景开场白
     * @param sessionId
     * @param scene
     * @param customScenario
     * @return
     */
    VoiceChatResponse sceneOpening(String sessionId, String scene, String customScenario);

}
